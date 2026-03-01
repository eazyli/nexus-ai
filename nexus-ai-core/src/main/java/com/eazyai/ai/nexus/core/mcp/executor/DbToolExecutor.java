package com.eazyai.ai.nexus.core.mcp.executor;

import com.eazyai.ai.nexus.core.mcp.McpToolDescriptor;
import com.eazyai.ai.nexus.core.mcp.McpToolResult;
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
 */
@Slf4j
@Component
public class DbToolExecutor {

    // 数据源缓存
    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    // 参数占位符匹配模式 #{paramName}
    private static final Pattern PARAM_PATTERN = Pattern.compile("#\\{(\\w+)}");

    /**
     * 执行数据库工具
     *
     * @param descriptor 工具描述符
     * @param params     输入参数
     * @return 执行结果
     */
    public McpToolResult execute(McpToolDescriptor descriptor, Map<String, Object> params) {
        Map<String, Object> config = descriptor.getConfig();
        if (config == null) {
            return McpToolResult.error(descriptor.getToolId(), "CONFIG_MISSING", "工具配置缺失");
        }

        String datasourceId = (String) config.get("datasourceId");
        String sqlTemplate = (String) config.get("sqlTemplate");
        String queryType = (String) config.getOrDefault("queryType", "SELECT");

        if (datasourceId == null || datasourceId.isEmpty()) {
            return McpToolResult.error(descriptor.getToolId(), "DATASOURCE_MISSING", "数据源ID未配置");
        }
        if (sqlTemplate == null || sqlTemplate.isEmpty()) {
            return McpToolResult.error(descriptor.getToolId(), "SQL_MISSING", "SQL模板未配置");
        }

        try {
            // 获取数据源
            DataSource dataSource = getDataSource(datasourceId, config);
            if (dataSource == null) {
                return McpToolResult.error(descriptor.getToolId(), "DATASOURCE_NOT_FOUND", 
                        "数据源不存在: " + datasourceId);
            }

            // 替换SQL中的参数占位符（使用预编译方式）
            String sql = sqlTemplate;
            Object[] paramValues = extractParamValues(sqlTemplate, params);
            sql = convertToPreparedStatement(sqlTemplate);

            // 执行SQL
            log.info("执行数据库工具: {} - SQL: {}", descriptor.getName(), sql);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            Object result;
            if ("SELECT".equalsIgnoreCase(queryType)) {
                result = executeQuery(jdbcTemplate, sql, paramValues);
            } else {
                result = executeUpdate(jdbcTemplate, sql, paramValues, queryType);
            }

            log.info("数据库工具执行成功: {}", descriptor.getName());
            return McpToolResult.success(descriptor.getToolId(), result);

        } catch (Exception e) {
            log.error("数据库工具执行异常: {}", descriptor.getName(), e);
            return McpToolResult.error(descriptor.getToolId(), "EXECUTION_ERROR", e.getMessage());
        }
    }

    /**
     * 获取数据源
     * 实际项目中应该从数据源管理器获取配置
     */
    @SuppressWarnings("unchecked")
    private DataSource getDataSource(String datasourceId, Map<String, Object> config) {
        // 先从缓存获取
        DataSource cached = dataSourceCache.get(datasourceId);
        if (cached != null) {
            return cached;
        }

        // 从配置中获取数据源信息（实际项目中应该从数据源管理服务获取）
        Map<String, Object> datasourceConfig = (Map<String, Object>) config.get("datasourceConfig");
        if (datasourceConfig == null) {
            log.warn("数据源配置不存在: {}", datasourceId);
            return null;
        }

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

            // 缓存数据源
            dataSourceCache.put(datasourceId, dataSource);
            log.info("创建数据源: {}", datasourceId);

            return dataSource;
        } catch (Exception e) {
            log.error("创建数据源失败: {}", datasourceId, e);
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
     * 提取参数值数组
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
     * 注册数据源
     * 用于动态注册新的数据源
     */
    public void registerDataSource(String datasourceId, Map<String, Object> config) {
        DataSource dataSource = getDataSource(datasourceId, Map.of("datasourceConfig", config));
        if (dataSource != null) {
            dataSourceCache.put(datasourceId, dataSource);
            log.info("注册数据源: {}", datasourceId);
        }
    }

    /**
     * 移除数据源
     */
    public void removeDataSource(String datasourceId) {
        dataSourceCache.remove(datasourceId);
        log.info("移除数据源: {}", datasourceId);
    }

    /**
     * 测试数据源连接
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
            log.error("数据源连接测试失败: {}", datasourceId, e);
            return false;
        }
    }
}
