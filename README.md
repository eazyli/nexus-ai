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
