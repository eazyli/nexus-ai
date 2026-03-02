# AI Agent 微内核插件化架构设计

## 架构概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           用户交互层 (User Interface)                          │
│                     ┌───────────────────────────────────┐                   │
│                     │ REST API / WebSocket / CLI / SDK  │                   │
│                     └────────────────┬──────────────────┘                   │
└──────────────────────────────────────┼──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       ReAct 执行引擎 (统一入口)                                │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                          ReActEngine                                    ││
│  │  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐            ││
│  │  │  Think   │ → │   Act    │ → │ Observe  │ → │ Reflect  │            ││
│  │  │  思考    │   │   行动   │   │   观察   │   │   反思   │            ││
│  │  └──────────┘   └──────────┘   └──────────┘   └──────────┘            ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                             │
│  核心能力：完整 ReAct 循环 / 思考过程暴露 / 自动反思 / 错误恢复              │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     LangChain4j AiServices 核心层                             │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐ │
│  │ AgentAssistant      │  │ AssistantFactory    │  │ PluginToolAdapter   │ │
│  │ (接口定义)          │  │ (工厂模式)          │  │ (插件适配)          │ │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘ │
│                                                                             │
│  核心能力：自动 Tool Calling / 会话记忆 / 结构化输出                         │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                    ┌──────────────────┴──────────────────┐
                    │                                     │
                    ▼                                     ▼
┌──────────────────────────────────┐  ┌──────────────────────────────────────┐
│      记忆层 (ChatMemory)          │  │      内部编排层 (Optional)            │
│  ┌──────────────────────────┐    │  │  ┌──────────────────────────────┐    │
│  │ PersistentChatMemory     │    │  │  │ InternalOrchestrator         │    │
│  │ (持久化会话记忆)          │    │  │  │ (复杂任务编排，内部实现)      │    │
│  └──────────────────────────┘    │  │  └──────────────────────────────┘    │
└──────────────────────────────────┘  └──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       插件注册中心 (Plugin Registry)                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ 插件发现    │  │ 版本管理    │  │ 依赖解析    │  │ 生命周期管理        │ │
│  │ Discovery   │  │ Version     │  │ Dependency  │  │ Lifecycle           │ │
│  │             │  │ Management  │  │ Resolution  │  │ Management          │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

## ReAct 执行流程

```
用户请求
    │
    ▼
┌──────────────┐
│   Think      │  分析意图，决定下一步行动
│   思考       │  "我需要使用搜索工具来查找..."
└──────┬───────┘
       │
       ▼
┌──────────────┐
│    Act       │  选择并调用工具
│    行动      │  tool_name: "search", input: "..."
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  Observe     │  获取工具执行结果
│   观察       │  observation: "搜索结果..."
└──────┬───────┘
       │
       ▼
   ┌───────┐     是      ┌──────────────┐
   │完成?  │────────────→│  Reflect     │
   └───┬───┘             │   反思       │
       │否               │  评估效果    │
       │                 └──────┬───────┘
       │                        │
       └────────────────────────┘
                                │
                                ▼
                         ┌──────────────┐
                         │ Final Answer │
                         │   最终答案   │
                         └──────────────┘
```

## 模块划分

```
nexus-ai/
├── nexus-ai-api/                    # API契约层
│   ├── dto/                         # 数据传输对象
│   ├── react/                       # 🔥 ReAct 核心模型
│   │   ├── ReActStep.java          # 执行步骤
│   │   ├── ReActContext.java       # 执行上下文
│   │   ├── ReflectionResult.java   # 反思结果
│   │   └── ThoughtEvent.java       # 思考事件
│   └── 定义所有模块间的接口契约
│
├── nexus-ai-core/                   # 核心框架层（微内核）
│   ├── assistant/                   # 🔥 LangChain4j AiServices
│   │   ├── AgentAssistant.java     # 智能助手接口定义
│   │   ├── AssistantFactory.java   # Assistant 工厂
│   │   └── PluginToolAdapter.java  # 插件->工具适配器
│   ├── engine/                      # 🔥 ReAct 执行引擎
│   │   └── ReActEngine.java        # 统一执行入口
│   ├── reflection/                  # 🔥 反思模块
│   │   └── ReflectionAgent.java    # 反思型智能体
│   ├── memory/                      # 基于 ChatMemory 的记忆管理
│   ├── planner/                     # 内部编排器
│   │   └── InternalOrchestrator.java
│   ├── scheduler/                   # 插件调度器（内部使用）
│   └── registry/                    # 插件注册中心
│
├── nexus-ai-plugin/                 # 插件体系
│   ├── spi/                         # 插件SPI接口
│   └── manager/                     # 插件生命周期管理
│
├── nexus-ai-tools/                  # 内置工具实现
│   └── @Tool 注解定义的工具类
│
├── nexus-ai-observability/          # 可观测性层
│
├── nexus-ai-starter/                # Spring Boot Starter
│
└── nexus-ai-web/                    # Web应用
```

## 核心设计原则

### 1. ReAct 流程完整性

```java
// 完整的 ReAct 循环
public AgentResponse execute(AgentRequest request, Consumer<ThoughtEvent> callback) {
    ReActContext context = new ReActContext();
    
    for (int i = 0; i < MAX_ITERATIONS; i++) {
        // 1. Think - 思考
        Thought thought = think(request, context);
        context.addStep(ReActStep.thought(i, thought.reasoning()));
        
        // 2. Act - 行动
        if (thought.isFinalAnswer()) {
            return buildResponse(thought.answer(), context);
        }
        
        // 3. Execute - 执行工具
        ToolResult result = executeTool(thought.action());
        context.addStep(ReActStep.observation(i, result.output()));
        
        // 4. Reflect - 反思（可选）
        if (shouldReflect(result)) {
            ReflectionResult reflection = reflect(context);
            // 根据反思结果调整策略
        }
    }
}
```

### 2. 统一执行入口

**之前（双模式）：**
```java
// 简单模式
agentEngine.execute(request);

// 高级模式
agentEngine.executeWithOrchestration(request, plan);
```

**现在（统一入口）：**
```java
// 统一入口，内部自动处理
reActEngine.execute(request);

// 带思考过程回调
reActEngine.execute(request, event -> log.info("{}", event.getContent()));
```

### 3. 思考过程可观测

```java
// 通过回调实时获取思考过程
reActEngine.execute(request, event -> {
    switch (event.getType()) {
        case THOUGHT -> log.info("思考: {}", event.getContent());
        case TOOL_SELECTED -> log.info("选择工具: {}", event.getToolName());
        case TOOL_RESULT -> log.debug("工具结果: {}", event.getToolOutput());
        case REFLECTION -> log.info("反思: {}", event.getContent());
        case FINAL_ANSWER -> log.info("最终答案: {}", event.getContent());
    }
});
```

### 4. 微内核架构

- **内核最小化**：核心只包含 ReAct 执行循环、插件管理等基础能力
- **插件可扩展**：所有业务功能通过插件实现
- **热插拔支持**：插件可动态加载、卸载、升级

## 组件职责

| 组件 | 职责 | 状态 |
|-----|------|------|
| `ReActEngine` | 统一执行入口，完整的 ReAct 循环 | 🔥 新增 |
| `ReflectionAgent` | 执行后反思，提供改进建议 | 🔥 新增 |
| `InternalOrchestrator` | 复杂任务内部编排 | 🔥 新增 |
| `AgentAssistant` | 定义智能助手接口 | ✅ 保留 |
| `AssistantFactory` | 创建和配置 AiServices | ✅ 保留 |
| `PluginToolAdapter` | 将 Plugin 适配为 Tool | ✅ 保留 |
| `PluginRegistry` | 插件注册中心 | ✅ 保留 |
| `PluginScheduler` | 插件调度器 | ✅ 保留（内部使用） |
| `HybridPlanner` | 混合规划器 | ❌ 删除 |
| `LlmDrivenRouter` | LLM 路由 | ❌ 删除 |

## 迁移指南

### 从旧 API 迁移

```java
// 新方式：统一入口
@Autowired
private ReActEngine reActEngine;

// 基本执行
AgentResponse response = reActEngine.execute(request);

// 带思考过程回调
AgentResponse response = reActEngine.execute(request, event -> log.info("{}", event.getContent()));
```

### 定义工具

```java
@Component
public class MyTools {
    
    @Tool("执行自定义操作")
    public String myOperation(String input) {
        return "结果: " + input;
    }
}
```

工具会自动被 `AssistantFactory` 发现并注册。

## 技术选型理由

| 选择 | 原因 |
|-----|------|
| LangChain4j AiServices | 原生支持 Tool Calling，自动处理工具调用循环 |
| ChatMemory | 原生会话记忆，支持多种后端 |
| @Tool 注解 | 声明式定义工具，简化开发 |
| ReActEngine | 统一入口，完整的 ReAct 流程 |
| ReflectionAgent | 执行后反思，提升决策质量 |
| PluginRegistry | LangChain4j 无此能力，保留自定义实现 |
