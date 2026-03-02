# AI Agent Framework - 企业级通用AI平台

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.30-orange.svg)](https://docs.langchain4j.dev/)
[![GitHub Stars](https://img.shields.io/github/stars/eazyai/nexus-ai.svg?style=social)](https://github.com/eazyai/nexus-ai/stargazers)

**中文** | [English](README_EN.md)

一个基于 Spring AI 和 LangChain4j 的企业级通用 AI 智能体框架，支持零代码接入业务系统。

## ✨ 核心特性

- 🚀 **双引擎支持** - 同时支持 LangChain4j 和 Spring AI
- 🔌 **零代码接入** - 通过配置动态注册工具，无需修改业务系统
- 🤖 **多模型适配** - 支持 OpenAI、DeepSeek、Qwen、Azure 等多种模型
- 📚 **企业级RAG** - 内置向量检索和知识库管理
- 🔧 **MCP工具总线** - 支持动态工具注册和调用
- 🎯 **流式响应** - 支持 SSE 流式输出
- 📊 **可观测性** - 完善的监控和日志体系

## 架构概览

本项目是一个企业级通用AI平台，支持通过配置赋能任意业务系统，可灵活扩展各类AI能力。

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           接入层 (Access Layer)                              │
│              REST API / WebSocket / SDK / WebHook / SSE                     │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           应用层 (Application Layer)                         │
│       应用管理 / 场景配置 / 知识库管理 / 运营监控 / 能力配置                    │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        核心能力层 (Core Layer)                               │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐   │
│  │ NLU引擎     │ │ ReAct Agent │ │ MCP工具总线 │ │ RAG引擎             │   │
│  │ 意图/实体   │ │ 任务规划    │ │ 工具调用    │ │ 检索增强           │   │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────────────┘   │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐   │
│  │ 记忆中心    │ │ 规则引擎    │ │ 结构化输出  │ │ 内容生成            │   │
│  │ 会话/长期   │ │ 条件/决策   │ │ JSON/Schema │ │ 文本/图表           │   │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────────────┘   │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        模型适配层 (Model Layer)                              │
│       模型抽象网关 / OpenAI / Azure / Anthropic / Qwen / DeepSeek           │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       基础设施层 (Infra Layer)                               │
│       MySQL / Redis / Milvus / RocketMQ / 安全 / 监控 / 日志                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 模块结构

```
nexus-ai/
├── nexus-ai-api/                    # API契约层
│   ├── dto/                         # 数据传输对象
│   ├── model/                       # 模型相关接口
│   ├── application/                 # 应用层接口
│   ├── plugin/                      # 插件SPI
│   └── registry/                    # 注册中心接口
│
├── nexus-ai-model/                  # 🔥 模型适配层
│   ├── gateway/                     # 模型网关
│   └── adapter/                     # 多模型适配器
│
├── nexus-ai-application/            # 🔥 应用层
│   ├── app/                         # 应用管理
│   ├── scene/                       # 场景配置
│   └── capability/                  # 能力配置
│
├── nexus-ai-core/                   # 核心能力层
│   ├── assistant/                   # LangChain4j AiServices
│   ├── engine/                      # Agent引擎
│   ├── nlu/                         # 🔥 NLU引擎
│   ├── mcp/                         # 🔥 MCP工具总线
│   ├── rule/                        # 🔥 规则引擎
│   ├── output/                      # 🔥 结构化输出
│   ├── memory/                      # 记忆中心
│   ├── planner/                     # 任务规划
│   ├── scheduler/                   # 调度器
│   └── registry/                    # 插件注册中心
│
├── nexus-ai-infra/                  # 🔥 基础设施层
│   ├── storage/                     # 存储服务
│   ├── cache/                       # 缓存服务
│   ├── security/                    # 安全服务
│   └── mq/                          # 消息队列
│
├── nexus-ai-tools/                  # 内置工具实现
├── nexus-ai-observability/          # 可观测性层
├── nexus-ai-starter/                # Spring Boot Starter
└── nexus-ai-web/                    # Web应用
```

## 核心特性

### 1. 通用化设计
- 不绑定任何具体业务场景
- 支持任意业务系统零改造接入
- 通过配置动态适配不同场景

### 2. 五层架构
| 层级 | 核心组件 | 职责 |
|-----|---------|------|
| 接入层 | REST/WebSocket/SDK | 统一接入入口 |
| 应用层 | App/Scene/Capability | 应用和场景配置 |
| 核心能力层 | NLU/Agent/MCP/RAG | AI核心能力 |
| 模型适配层 | ModelGateway/Adapter | 多模型适配 |
| 基础设施层 | Storage/Cache/Security | 底层支撑 |

### 3. 核心API

#### 执行AI任务
```bash
POST /api/v1/agent/run
Content-Type: application/json

{
  "appId": "app-001",
  "sceneId": "scene-001",
  "input": "帮我分析这份数据",
  "sessionId": "session-001"
}
```

#### 注册工具
```bash
POST /api/v1/tool/register
Content-Type: application/json

{
  "toolId": "weather-api",
  "name": "天气查询",
  "type": "http",
  "description": "查询指定城市的天气信息",
  "config": {
    "url": "https://api.weather.com/v1/query",
    "method": "GET"
  }
}
```

## 快速开始

### 1. 引入依赖
```xml
<dependency>
    <groupId>com.eazyai</groupId>
    <artifactId>nexus-ai-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置模型

复制示例配置文件并根据实际情况修改：

```bash
cp nexus-ai-web/src/main/resources/application-example.yml nexus-ai-web/src/main/resources/application.yml
```

配置环境变量：

```bash
# 设置API Key（支持OpenAI、DeepSeek等兼容接口）
export OPENAI_API_KEY=your-api-key-here
export OPENAI_BASE_URL=https://api.openai.com  # 或 https://api.deepseek.com
export OPENAI_MODEL=gpt-4  # 或 deepseek-chat

# LangChain4j 配置（可选，默认使用上面的配置）
export LANGCHAIN4J_API_KEY=${OPENAI_API_KEY}
export LANGCHAIN4J_BASE_URL=${OPENAI_BASE_URL}
export LANGCHAIN4J_MODEL=${OPENAI_MODEL}

# Embedding 配置（可选）
export EMBEDDING_API_KEY=${OPENAI_API_KEY}
export EMBEDDING_BASE_URL=${OPENAI_BASE_URL}
```

或直接在 `application.yml` 中配置：

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key-here
      base-url: https://api.openai.com  # 或 https://api.deepseek.com
      chat:
        options:
          model: gpt-4  # 或 deepseek-chat
```

### 3. 调用Agent
```java
@Autowired
private AgentEngine agentEngine;

AgentResponse response = agentEngine.execute(
    AgentRequest.builder()
        .appId("my-app")
        .input("你好")
        .build()
);
```

## 接入业务系统教程

本平台通过动态注册工具的方式接入任意业务系统，无需修改业务系统代码。

### 第一步：创建应用

```bash
curl -X POST http://localhost:8088/api/v1/apps \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "wenhuagong",
    "name": "文化宫智能助手",
    "description": "文化宫课程活动查询系统",
    "type": "agent",
    "config": {
      "systemPrompt": "你是文化宫的智能助手，可以帮助用户查询课程活动信息。请用友好的语气回答用户问题。"
    }
  }'
```

### 第二步：注册业务工具

#### 2.1 注册HTTP工具（调用REST API）

**GET请求示例：**
```bash
curl -X POST http://localhost:8088/api/v1/tools/http \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "wenhuagong",
    "name": "getActivityList",
    "description": "查询文化宫课程活动列表。可根据活动状态筛选：1-进行中，2-未开始，3-已结束。支持分页查询。",
    "capabilities": ["activity", "query", "list"],
    "parameters": [
        {
            "name": "activity_statu",
            "type": "string",
            "description": "活动状态：1-进行中，2-未开始，3-已结束",
            "required": false,
            "defaultValue": "1",
            "options": ["1", "2", "3"]
        },
        {
            "name": "pageNum",
            "type": "integer",
            "description": "页码，从1开始",
            "required": false,
            "defaultValue": 1
        },
        {
            "name": "pageSize",
            "type": "integer",
            "description": "每页数量",
            "required": false,
            "defaultValue": 10
        }
    ],
    "url": "https://trade.grgbanking.com/szqy/appnew/train/activity-list",
    "method": "POST",
    "headers": {
        "Content-Type": "application/json"
    },
    "responsePath": "$.data",
    "timeout": 30000,
    "retryTimes": 3
}'
```

**参数说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| appId | string | 是 | 关联的应用ID |
| name | string | 是 | 工具名称（唯一标识） |
| description | string | 是 | 工具描述（AI会根据此描述决定是否调用） |
| capabilities | array | 否 | 能力标签，用于工具筛选 |
| parameters | array | 否 | 参数定义列表 |
| url | string | 是 | 请求URL |
| method | string | 否 | 请求方法，默认GET |
| headers | object | 否 | 请求头 |
| authType | string | 否 | 认证类型：none/basic/bearer/api_key |
| authConfig | object | 否 | 认证配置 |
| responsePath | string | 否 | 响应数据提取路径（JSONPath） |
| timeout | integer | 否 | 超时时间（毫秒） |
| retryTimes | integer | 否 | 重试次数 |

#### 2.2 注册数据库工具（执行SQL查询）

```bash
curl -X POST http://localhost:8088/api/v1/tools/db \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "wenhuagong",
    "name": "queryUserOrders",
    "description": "查询用户订单记录",
    "capabilities": ["order", "query"],
    "parameters": [
        {
            "name": "userId",
            "type": "string",
            "description": "用户ID",
            "required": true
        },
        {
            "name": "startDate",
            "type": "string",
            "description": "开始日期，格式yyyy-MM-dd",
            "required": false
        }
    ],
    "datasourceId": "mysql-main",
    "sqlTemplate": "SELECT * FROM orders WHERE user_id = :userId AND create_date >= :startDate",
    "queryType": "SELECT"
}'
```

### 第三步：执行Agent调用

```bash
curl -X POST http://localhost:8088/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "wenhuagong",
    "query": "文化宫当前有多少个正在进行的课程活动？"
  }'
```

**响应示例：**
```json
{
  "success": true,
  "content": "文化宫当前有15个正在进行的课程活动，包括瑜伽、书法、舞蹈等多种类型。",
  "toolCalls": [
    {
      "toolName": "getActivityList",
      "arguments": {"activity_statu": "1", "pageNum": 1, "pageSize": 10},
      "result": {...}
    }
  ]
}
```

### 工具管理API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/tools/http` | POST | 注册HTTP工具 |
| `/api/v1/tools/db` | POST | 注册数据库工具 |
| `/api/v1/tools` | GET | 获取工具列表 |
| `/api/v1/tools/{toolId}` | GET | 获取工具详情 |
| `/api/v1/tools/{toolId}` | DELETE | 删除工具 |
| `/api/v1/tools/{toolId}/invoke` | POST | 直接执行工具 |

### 接入流程图

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   业务系统    │     │   AI平台     │     │   大模型     │
│  (无需改造)   │     │             │     │             │
└──────┬───────┘     └──────┬───────┘     └──────┬───────┘
       │                    │                    │
       │  1. 暴露REST API   │                    │
       │◄───────────────────│                    │
       │                    │                    │
       │                    │  2. 注册工具       │
       │                    │    (描述API接口)   │
       │                    │                    │
       │                    │  3. 用户提问       │
       │                    │◄───────────────────│
       │                    │                    │
       │                    │  4. 大模型理解     │
       │                    │    决定调用工具    │
       │                    │───────────────────►│
       │                    │                    │
       │  5. 调用业务API    │                    │
       │◄───────────────────│                    │
       │                    │                    │
       │  6. 返回业务数据   │                    │
       │───────────────────►│                    │
       │                    │                    │
       │                    │  7. 生成回答       │
       │                    │◄───────────────────│
       │                    │                    │
       │                    │  8. 返回结果       │
       │                    │───────────────────►│
```

## 使用案例：工具管理应用

本案例展示如何创建一个"工具管理应用"，通过AI对话方式实现工具的批量导入。

### 案例背景

在实际项目中，经常需要为业务应用批量导入大量工具（如将Apifox导出的接口文档转为工具）。传统做法是：
1. 手动逐个注册工具
2. 或编写脚本批量调用注册接口

本案例展示了更优雅的方案：**通过创建一个"工具管理应用"，让AI帮助完成批量导入工作**。

### 核心思想

```
AI平台的核心价值：
- 不是"我实现所有功能"，而是"我能调用所有能力"
- 平台提供"大脑"（智能调度），能力可以来自任何地方
- 任何功能只要能提供HTTP接口，就能成为AI的能力
```

### 系统新增接口

本系统已新增以下两个接口，用于支持工具批量导入：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/tools/parse-apifox` | POST | 解析Apifox/OpenAPI文档，转换为工具定义列表 |
| `/api/v1/tools/batch-import` | POST | 批量导入工具到指定应用 |

### 创建工具管理应用

通过调用系统原有接口创建"工具管理助手"应用：

#### 步骤1：创建应用（调用 `/api/v1/apps`）

```bash
curl -X POST http://localhost:8088/api/v1/apps \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "tool-manager",
    "name": "工具管理助手",
    "description": "AI工具管理助手，支持解析Apifox文档并批量导入工具到其他应用",
    "type": "agent",
    "config": {
      "systemPrompt": "你是一个工具管理助手。你可以帮助用户：\n1. 解析Apifox导出的OpenAPI文档\n2. 将解析出的接口批量导入为工具\n3. 管理各应用的工具配置\n\n当用户提供Apifox文档时，你会解析文档内容并生成工具定义。\n当用户要求批量导入工具时，你会调用批量导入接口完成操作。\n请友好、专业地与用户交互，并在操作完成后提供清晰的反馈。"
    },
    "enabled": true
  }'
```

#### 步骤2：注册工具 - 解析Apifox文档（调用 `/api/v1/tools/http`）

将本系统新增的 `parse-apifox` 接口注册为工具：

```bash
curl -X POST http://localhost:8088/api/v1/tools/http \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "tool-manager",
    "name": "parseApifox",
    "description": "解析Apifox导出的OpenAPI文档，将API端点转换为工具定义",
    "url": "http://localhost:8088/api/v1/tools/parse-apifox",
    "method": "POST",
    "headers": {"Content-Type": "application/json"},
    "capabilities": ["parse", "apifox", "openapi"],
    "parameters": [
      {
        "name": "documentContent",
        "type": "string",
        "description": "Apifox导出的OpenAPI JSON文档内容",
        "required": true
      },
      {
        "name": "baseUrl",
        "type": "string",
        "description": "基础URL，用于拼接接口路径，如 http://api.example.com",
        "required": false
      },
      {
        "name": "includeTags",
        "type": "array",
        "description": "只解析指定标签的接口",
        "required": false
      },
      {
        "name": "excludeTags",
        "type": "array",
        "description": "排除指定标签的接口",
        "required": false
      }
    ],
    "timeout": 60000,
    "retryTimes": 1
  }'
```

#### 步骤3：注册工具 - 批量导入工具（调用 `/api/v1/tools/http`）

将本系统新增的 `batch-import` 接口注册为工具：

```bash
curl -X POST http://localhost:8088/api/v1/tools/http \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "tool-manager",
    "name": "batchImportTools",
    "description": "批量导入工具到指定应用",
    "url": "http://localhost:8088/api/v1/tools/batch-import",
    "method": "POST",
    "headers": {"Content-Type": "application/json"},
    "capabilities": ["import", "batch", "tools"],
    "parameters": [
      {
        "name": "targetAppId",
        "type": "string",
        "description": "目标应用ID，要将工具导入到哪个应用",
        "required": true
      },
      {
        "name": "tools",
        "type": "array",
        "description": "工具定义列表，每个元素包含name、description、url、method、parameters等字段",
        "required": true
      },
      {
        "name": "overwrite",
        "type": "boolean",
        "description": "是否覆盖已存在的同名工具，默认false",
        "required": false
      }
    ],
    "timeout": 60000,
    "retryTimes": 1
  }'
```

#### 步骤4：注册工具 - 获取应用列表（调用 `/api/v1/tools/http`）

将系统的 `listApps` 接口注册为工具：

```bash
curl -X POST http://localhost:8088/api/v1/tools/http \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "tool-manager",
    "name": "listApps",
    "description": "获取所有应用列表，用于了解系统中有哪些应用可以导入工具",
    "url": "http://localhost:8088/api/v1/apps",
    "method": "GET",
    "headers": {"Content-Type": "application/json"},
    "capabilities": ["query", "apps"],
    "timeout": 30000
  }'
```

#### 步骤5：注册工具 - 获取应用工具列表（调用 `/api/v1/tools/http`）

将系统的 `listAppTools` 接口注册为工具：

```bash
curl -X POST http://localhost:8088/api/v1/tools/http \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "tool-manager",
    "name": "listAppTools",
    "description": "获取指定应用下的所有工具列表",
    "url": "http://localhost:8088/api/v1/tools/app/{appId}",
    "method": "GET",
    "headers": {"Content-Type": "application/json"},
    "capabilities": ["query", "tools"],
    "parameters": [
      {
        "name": "appId",
        "type": "string",
        "description": "应用ID，要查询哪个应用的工具列表",
        "required": true
      }
    ],
    "timeout": 30000
  }'
```

#### 步骤6：验证创建结果

```bash
# 查看创建的应用
curl http://localhost:8088/api/v1/apps/tool-manager

# 查看应用的工具列表
curl http://localhost:8088/api/v1/tools/app/tool-manager
```

### 使用工具管理应用

创建完成后，即可与工具管理助手对话：

```bash
# 查看系统中的应用
curl -X POST http://localhost:8088/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "tool-manager",
    "query": "帮我查看系统中有哪些应用"
  }'

# 解析并导入工具（需要在对话中提供Apifox文档）
curl -X POST http://localhost:8088/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "tool-manager",
    "query": "这是Apifox导出的接口文档，请解析并批量导入到wenhuagong应用",
    "context": {
      "apifoxDocument": "{...OpenAPI JSON文档...}"
    }
  }'
```

### 工作流程图

```
┌──────────────────────────────────────────────────────────────────┐
│                     用户与工具管理助手对话                          │
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
用户: "帮我解析这份Apifox文档，然后导入到wenhuagong应用"
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│ AI理解意图，决定调用工具：parseApifox                              │
│ 参数: {documentContent: "...", baseUrl: "http://api.example.com"}│
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│ parseApifox工具执行                                               │
│ 调用: POST /api/v1/tools/parse-apifox                            │
│ 返回: 解析出的工具定义列表                                          │
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│ AI分析解析结果，决定调用工具：batchImportTools                      │
│ 参数: {targetAppId: "wenhuagong", tools: [...]}                  │
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│ batchImportTools工具执行                                          │
│ 调用: POST /api/v1/tools/batch-import                            │
│ 返回: 导入结果统计                                                  │
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
AI: "已成功解析15个接口，并导入到wenhuagong应用。成功导入15个工具。"
```

### 新增API接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/tools/parse-apifox` | POST | 解析Apifox/OpenAPI文档 |
| `/api/v1/tools/batch-import` | POST | 批量导入工具 |

#### 解析Apifox文档

```bash
POST /api/v1/tools/parse-apifox
Content-Type: application/json

{
  "documentContent": "{...OpenAPI JSON文档...}",
  "baseUrl": "http://api.example.com",
  "includeTags": ["用户管理", "订单管理"],
  "excludeTags": ["内部接口"]
}
```

#### 批量导入工具

```bash
POST /api/v1/tools/batch-import
Content-Type: application/json

{
  "targetAppId": "wenhuagong",
  "tools": [
    {
      "name": "getUserInfo",
      "description": "获取用户信息",
      "url": "http://api.example.com/user/info",
      "method": "GET",
      "parameters": [
        {"name": "userId", "type": "string", "description": "用户ID", "required": true}
      ]
    }
  ],
  "overwrite": true
}
```

### 架构优势

1. **平台无需感知具体功能**：平台只提供工具注册和调度能力
2. **能力可插拔**：工具可以指向内部API，也可以指向外部系统
3. **AI增强用户体验**：用户用自然语言描述需求，AI完成复杂操作
4. **易于扩展**：新增功能只需注册新工具，无需修改平台代码

### 扩展方向

这个案例可以进一步扩展：
- 支持更多文档格式（Postman、Swagger YAML）
- 增加工具测试功能
- 支持工具版本管理
- 工具使用情况统计分析

## 技术栈

| 组件 | 版本 | 说明 |
|-----|------|------|
| Java | 21 | GraalVM |
| Spring Boot | 3.2.x | 框架 |
| LangChain4j | 0.30.x | AI编排 |
| Spring AI | 1.0.x | AI集成 |

## 环境变量说明

| 变量名 | 说明 | 示例 |
|-------|------|------|
| `OPENAI_API_KEY` | OpenAI/DeepSeek API密钥 | `sk-xxx` |
| `OPENAI_BASE_URL` | API基础URL | `https://api.openai.com` |
| `OPENAI_MODEL` | 默认模型 | `gpt-4` 或 `deepseek-chat` |
| `LANGCHAIN4J_API_KEY` | LangChain4j API密钥 | 默认使用 `OPENAI_API_KEY` |
| `EMBEDDING_API_KEY` | Embedding API密钥 | 默认使用 `OPENAI_API_KEY` |
| `SEARCH_API_KEY` | 搜索API密钥 | Bing Search API |

## 贡献指南

欢迎贡献代码！请查看 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详情。

## 许可证

[Apache License 2.0](LICENSE)

## 致谢

感谢以下开源项目：
- [LangChain4j](https://github.com/langchain4j/langchain4j)
- [Spring AI](https://github.com/spring-projects/spring-ai)
- [Spring Boot](https://github.com/spring-projects/spring-boot)
