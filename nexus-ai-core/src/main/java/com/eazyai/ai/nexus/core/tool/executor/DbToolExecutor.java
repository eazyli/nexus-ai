package com.eazyai.ai.nexus.core.tool.executor;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolExecutor;
import com.eazyai.ai.nexus.api.tool.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库工具执行器
 * 执行数据库查询类型的工具
 *
 * <h3>配置示例：</h3>
 * <pre>
 * {
 *   "datasourceId": "primary-db",
 *   "sqlTemplate": "SELECT * FROM users WHERE id = #{userId} AND status = #{status}",
 *   "queryType": "SELECT",
 *   "datasourceConfig": {
 *     "driverClassName": "com.mysql.cj.jdbc.Driver",
 *     "url": "jdbc:mysql://localhost:3306/mydb",
 *     "username": "root",
 *     "password": "password"
 *   }
 * }
 * </pre>
 *
 * <h3>特性：</h3>
 * <ul>
 *   <li>支持SELECT/INSERT/UPDATE/DELETE等SQL操作</li>
 *   <li>参数化SQL预编译，防止SQL注入：#{param} -> ?</li>
 *   <li>数据源缓存管理，避免重复创建连接</li>
 *   <li>动态数据源注册/注销</li>
 *   <li>连接测试功能</li>
 * </ul>
 *
 * <h3>安全说明：</h3>
 * <p>所有参数使用预编译方式处理，避免SQL注入风险。</p>
 * <p>建议通过datasourceId引用预先配置的数据源，而非动态配置。</p>
 */
@Slf4j
@Component
public class DbToolExecutor implements ToolExecutor {

    private static final String EXECUTOR_TYPE = "db";

    /**
     * 数据源缓存：datasourceId -> DataSource
     */
    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    /**
     * 参数占位符匹配模式 #{paramName}
     */
    private static final Pattern PARAM_PATTERN = Pattern.compile("#\\{(\\w+)}");

    @Override
    public String getExecutorType() {
        return EXECUTOR_TYPE;
    }

    @Override
    public ToolResult execute(ToolDescriptor descriptor, Map<String, Object> params, AgentContext context) {
        Map<String, Object> config = descriptor.getConfig();
        if (config == null) {
            return ToolResult.error(descriptor.getToolId(), "CONFIG_MISSING", "工具配置缺失");
        }

        String datasourceId = (String) config.get("datasourceId");
        String sqlTemplate = (String) config.get("sqlTemplate");
        String queryType = (String) config.getOrDefault("queryType", "SELECT");

        if (datasourceId == null || datasourceId.isEmpty()) {
            return ToolResult.error(descriptor.getToolId(), "DATASOURCE_MISSING", "数据源ID未配置");
        }
        if (sqlTemplate == null || sqlTemplate.isEmpty()) {
            return ToolResult.error(descriptor.getToolId(), "SQL_MISSING", "SQL模板未配置");
        }

        try {
            // 1. 获取数据源
            DataSource dataSource = getDataSource(datasourceId, config);
            if (dataSource == null) {
                return ToolResult.error(descriptor.getToolId(), "DATASOURCE_NOT_FOUND",
                        "数据源不存在: " + datasourceId);
            }

            // 2. 提取参数值并转换为预编译SQL
            Object[] paramValues = extractParamValues(sqlTemplate, params);
            String sql = convertToPreparedStatement(sqlTemplate);

            // 3. 执行SQL
            log.info("[DbToolExecutor] 执行数据库工具: {} - SQL: {}", descriptor.getName(), sql);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            Object result;
            if ("SELECT".equalsIgnoreCase(queryType)) {
                result = executeQuery(jdbcTemplate, sql, paramValues);
            } else {
                result = executeUpdate(jdbcTemplate, sql, paramValues, queryType);
            }

            log.info("[DbToolExecutor] 执行成功: {} - 影响行数/返回行数: {}", 
                    descriptor.getName(), getResultCount(result));
            return ToolResult.success(descriptor.getToolId(), result);

        } catch (Exception e) {
            log.error("[DbToolExecutor] 执行异常: {} - {}", descriptor.getName(), e.getMessage(), e);
            return ToolResult.error(descriptor.getToolId(), "EXECUTION_ERROR", e.getMessage(), e);
        }
    }

    /**
     * 获取数据源
     * 优先从缓存获取，缓存不存在则从配置创建
     */
    @SuppressWarnings("unchecked")
    private DataSource getDataSource(String datasourceId, Map<String, Object> config) {
        // 1. 先从缓存获取
        DataSource cached = dataSourceCache.get(datasourceId);
        if (cached != null) {
            return cached;
        }

        // 2. 从配置中获取数据源信息
        Map<String, Object> datasourceConfig = (Map<String, Object>) config.get("datasourceConfig");
        if (datasourceConfig == null) {
            log.warn("[DbToolExecutor] 数据源配置不存在: {}", datasourceId);
            return null;
        }

        // 3. 创建数据源
        try {
            String driverClassName = (String) datasourceConfig.get("driverClassName");
            String url = (String) datasourceConfig.get("url");
            String username = (String) datasourceConfig.get("username");
            String password = (String) datasourceConfig.get("password");

            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(driverClassName);
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);

            // 4. 缓存数据源
            dataSourceCache.put(datasourceId, dataSource);
            log.info("[DbToolExecutor] 创建数据源: {} -> {}", datasourceId, url);

            return dataSource;
        } catch (Exception e) {
            log.error("[DbToolExecutor] 创建数据源失败: {}", datasourceId, e);
            return null;
        }
    }

    /**
     * 将SQL模板转换为预编译语句格式
     * #{param} -> ?
     */
    private String convertToPreparedStatement(String sqlTemplate) {
        return PARAM_PATTERN.matcher(sqlTemplate).replaceAll("?");
    }

    /**
     * 提取参数值数组（按SQL中参数顺序）
     */
    private Object[] extractParamValues(String sqlTemplate, Map<String, Object> params) {
        java.util.List<Object> values = new java.util.ArrayList<>();
        Matcher matcher = PARAM_PATTERN.matcher(sqlTemplate);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = params != null ? params.get(paramName) : null;
            values.add(value);
        }
        return values.toArray();
    }

    /**
     * 执行查询
     */
    private List<Map<String, Object>> executeQuery(JdbcTemplate jdbcTemplate,
                                                    String sql, Object[] params) {
        return jdbcTemplate.queryForList(sql, params);
    }

    /**
     * 执行更新
     */
    private Map<String, Object> executeUpdate(JdbcTemplate jdbcTemplate,
                                               String sql, Object[] params, String queryType) {
        int affectedRows = jdbcTemplate.update(sql, params);
        return Map.of(
                "affectedRows", affectedRows,
                "queryType", queryType,
                "success", true
        );
    }

    /**
     * 获取结果数量
     */
    private int getResultCount(Object result) {
        if (result instanceof List) {
            return ((List<?>) result).size();
        } else if (result instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) result;
            Object affectedRows = map.get("affectedRows");
            if (affectedRows instanceof Integer) {
                return (Integer) affectedRows;
            }
            return 0;
        }
        return 0;
    }

    // ==================== 数据源管理API ====================

    /**
     * 注册数据源
     * 用于动态注册新的数据源
     *
     * @param datasourceId 数据源ID
     * @param config       数据源配置
     */
    public void registerDataSource(String datasourceId, Map<String, Object> config) {
        DataSource dataSource = getDataSource(datasourceId, Map.of("datasourceConfig", config));
        if (dataSource != null) {
            dataSourceCache.put(datasourceId, dataSource);
            log.info("[DbToolExecutor] 注册数据源: {}", datasourceId);
        }
    }

    /**
     * 移除数据源
     *
     * @param datasourceId 数据源ID
     */
    public void removeDataSource(String datasourceId) {
        dataSourceCache.remove(datasourceId);
        log.info("[DbToolExecutor] 移除数据源: {}", datasourceId);
    }

    /**
     * 测试数据源连接
     *
     * @param datasourceId 数据源ID
     * @return 连接是否正常
     */
    public boolean testConnection(String datasourceId) {
        DataSource dataSource = dataSourceCache.get(datasourceId);
        if (dataSource == null) {
            return false;
        }
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.error("[DbToolExecutor] 数据源连接测试失败: {}", datasourceId, e);
            return false;
        }
    }

    /**
     * 获取已注册的数据源ID列表
     */
    public java.util.Set<String> getRegisteredDatasourceIds() {
        return dataSourceCache.keySet();
    }
}
