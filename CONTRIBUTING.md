# 贡献指南

## 开发环境要求

- **Java 17+** (必需)
- **Maven 3.8+**
- **IntelliJ IDEA** 或 **VS Code**

## 构建项目

```bash
# 克隆项目
git clone <repository-url>
cd nexus-ai

# 编译安装到本地仓库
mvn clean install -DskipTests

# 运行测试
mvn test

# 打包
mvn clean package -DskipTests
```

## 模块开发指南

### 1. 开发自定义插件

在 `nexus-ai-tools` 模块中添加新的插件实现：

```java
@Component
public class MyPlugin implements Plugin {
    
    @Override
    public ExecutionResult execute(Map<String, Object> params, AgentContext context) {
        // 实现插件逻辑
        Object result = doSomething(params);
        return ExecutionResult.success(result);
    }
    
    @Override
    public PluginDescriptor getDescriptor() {
        return PluginDescriptor.builder()
            .id("my-plugin")
            .name("My Plugin")
            .version("1.0.0")
            .type("custom")
            .description("插件描述")
            .capabilities(List.of("capability1", "capability2"))
            .build();
    }
}
```

### 2. 扩展意图分析器

```java
@Component
public class CustomIntentAnalyzer implements IntentAnalyzer {
    
    @Override
    public IntentResult analyze(AgentRequest request, AgentContext context) {
        // 实现意图分析逻辑
        return IntentResult.builder()
            .intentType("custom_intent")
            .confidence(0.9)
            .build();
    }
    
    @Override
    public boolean supports(AgentRequest request) {
        // 判断是否支持该请求
        return true;
    }
}
```

### 3. 扩展任务规划器

```java
@Component
public class CustomPlanner implements TaskPlanner {
    
    @Override
    public TaskPlan createPlan(IntentResult intent, AgentContext context) {
        return TaskPlan.builder()
            .planId(UUID.randomUUID().toString())
            .name("Custom Plan")
            .steps(List.of(
                TaskPlan.TaskStep.builder()
                    .stepId("step1")
                    .pluginType("my-plugin")
                    .params(Map.of("key", "value"))
                    .build()
            ))
            .build();
    }
    
    @Override
    public boolean supports(String intentType) {
        return "custom_intent".equals(intentType);
    }
}
```

## 架构原则

1. **接口隔离**：各层通过接口交互，不依赖具体实现
2. **插件化**：业务功能通过插件实现，支持热插拔
3. **配置优先**：行为通过配置控制，支持运行时变更
4. **可观测性**：所有关键操作都应产生事件和指标

## 提交代码

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 代码规范

- 遵循 Google Java Style Guide
- 所有公共API必须有Javadoc
- 单元测试覆盖率不低于70%
- 使用 Lombok 减少样板代码
