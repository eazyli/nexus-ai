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
│                     LangChain4j AiServices 核心层                             │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐ │
│  │ AgentAssistant      │  │ AssistantFactory    │  │ PluginToolAdapter   │ │
│  │ (接口定义)          │  │ (工厂模式)          │  │ (插件适配)          │ │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘ │
│                                                                             │
│  核心能力：自动 Tool Calling / 自动 ReAct 循环 / 会话记忆 / 结构化输出         │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                    ┌──────────────────┴──────────────────┐
                    │                                     │
                    ▼                                     ▼
┌──────────────────────────────────┐  ┌──────────────────────────────────────┐
│      记忆层 (ChatMemory)          │  │      插件调度层 (Optional)            │
│  ┌──────────────────────────┐    │  │  ┌─────────┐  ┌─────────┐            │
│  │ MessageWindowChatMemory  │    │  │  │ 负载均衡│  │ 熔断降级│            │
│  │ (LangChain4j 原生)       │    │  │  └─────────┘  └─────────┘            │
│  └──────────────────────────┘    │  │                                      │
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

## 模块划分

```
nexus-ai/
├── nexus-ai-api/                    # API契约层
│   └── 定义所有模块间的接口契约
│
├── nexus-ai-core/                   # 核心框架层（微内核）
│   ├── assistant/                   # 🔥 LangChain4j AiServices
│   │   ├── AgentAssistant.java     # 智能助手接口定义
│   │   ├── AssistantFactory.java   # Assistant 工厂
│   │   └── PluginToolAdapter.java  # 插件->工具适配器
│   ├── engine/                      # Agent 引擎
│   ├── memory/                      # 🔥 基于 ChatMemory 的记忆管理
│   ├── planner/                     # 任务规划（高级模式）
│   ├── scheduler/                   # 插件调度器
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

### 1. LangChain4j 原生优先

**之前（手动实现）：**
```java
// 手动实现 ReAct 循环
public AgentResponse execute(AgentRequest request) {
    for (int i = 0; i < maxIterations; i++) {
        String thought = extractThought(content);
        String action = extractAction(content);
        String observation = executeTool(action, input);
        // ... 手动解析和执行
    }
}
```

**现在（LangChain4j）：**
```java
// 一行代码，自动处理一切
String answer = assistant.chat("帮我计算 2+2*3");
```

### 2. 微内核架构

- **内核最小化**：核心只包含插件调度、生命周期管理等基础能力
- **插件可扩展**：所有业务功能通过插件实现
- **热插拔支持**：插件可动态加载、卸载、升级

### 3. 双模式执行

| 模式 | 触发条件 | 执行流程 |
|-----|---------|---------|
| **简单模式** | 默认 | AiServices → 自动 Tool Calling → 返回结果 |
| **高级模式** | 显式调用 | 意图分析 → 任务规划 → 插件调度 → 结果整合 |

### 4. 配置优先

- 各层行为可通过配置调整
- 支持运行时配置变更
- 约定优于配置

## 组件职责

| 组件 | 职责 | 状态 |
|-----|------|------|
| `AgentAssistant` | 定义智能助手接口 | 🔥 新增 |
| `AssistantFactory` | 创建和配置 AiServices | 🔥 新增 |
| `PluginToolAdapter` | 将 Plugin 适配为 Tool | 🔥 新增 |
| `LangChain4jAgent` | 基于 AiServices 的 Agent 实现 | 🔥 新增 |
| `LangChain4jMemoryManager` | 基于 ChatMemory 的记忆管理 | 🔥 新增 |
| `AgentEngine` | 核心协调器 | ✅ 重构 |
| `PluginRegistry` | 插件注册中心 | ✅ 保留 |
| `PluginScheduler` | 插件调度器 | ✅ 保留（高级模式） |
| `ReActAgent` | 手动 ReAct 实现 | ❌ 删除 |
| `LlmDrivenRouter` | LLM 路由 | ✅ 简化 |
| `InMemoryMemoryManager` | 内存记忆管理 | ❌ 删除 |

## 迁移指南

### 从旧 API 迁移

```java
// 旧方式：需要手动管理 Agent 类型
@Autowired
private ReActAgent reActAgent;
AgentResponse response = reActAgent.execute(request);

// 新方式：统一入口
@Autowired
private AgentEngine agentEngine;
AgentResponse response = agentEngine.execute(request);

// 或更简单：直接使用 Assistant
@Autowired
private AssistantFactory assistantFactory;
AgentAssistant assistant = assistantFactory.getAssistant();
String answer = assistant.chat("你好");
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
| LangChain4j AiServices | 原生支持 Tool Calling，自动 ReAct 循环 |
| ChatMemory | 原生会话记忆，支持多种后端 |
| @Tool 注解 | 声明式定义工具，简化开发 |
| PluginRegistry | LangChain4j 无此能力，保留自定义实现 |
