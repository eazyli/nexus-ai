# AI Agent Framework - Enterprise AI Platform

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.30-orange.svg)](https://docs.langchain4j.dev/)
[![GitHub Stars](https://img.shields.io/github/stars/eazyai/nexus-ai.svg?style=social)](https://github.com/eazyai/nexus-ai/stargazers)

[中文](README.md) | **English**

An enterprise-grade general AI agent framework based on Spring AI and LangChain4j, supporting zero-code integration with business systems.

## ✨ Key Features

- 🚀 **Dual Engine Support** - Both LangChain4j and Spring AI
- 🔌 **Zero-Code Integration** - Dynamic tool registration via configuration
- 🤖 **Multi-Model Adapters** - Support for OpenAI, DeepSeek, Qwen, Azure, etc.
- 📚 **Enterprise RAG** - Built-in vector search and knowledge management
- 🔧 **MCP Tool Bus** - Dynamic tool registration and invocation
- 🎯 **Streaming Response** - SSE streaming output support
- 📊 **Observability** - Comprehensive monitoring and logging

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Access Layer                                       │
│              REST API / WebSocket / SDK / WebHook / SSE                     │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Application Layer                                    │
│       App Management / Scene Configuration / Knowledge Base / Operations    │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Core Layer                                         │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐   │
│  │ NLU Engine  │ │ ReAct Agent │ │ MCP Tool Bus│ │ RAG Engine          │   │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────────────┘   │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Model Layer                                         │
│       Model Gateway / OpenAI / Azure / Anthropic / Qwen / DeepSeek          │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Infrastructure Layer                                  │
│       MySQL / Redis / Milvus / RocketMQ / Security / Monitoring / Logging   │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/eazyai/nexus-ai.git
cd nexus-ai
```

### 2. Configure Model

Copy the example configuration file:

```bash
cp nexus-ai-web/src/main/resources/application-example.yml nexus-ai-web/src/main/resources/application.yml
```

Set environment variables:

```bash
# Set API Key (supports OpenAI, DeepSeek, etc.)
export OPENAI_API_KEY=your-api-key-here
export OPENAI_BASE_URL=https://api.openai.com  # or https://api.deepseek.com
export OPENAI_MODEL=gpt-4  # or deepseek-chat
```

### 3. Build and Run
```bash
mvn clean install -DskipTests
cd nexus-ai-web
mvn spring-boot:run
```

### 4. Access API Documentation
- Swagger UI: http://localhost:8088/swagger-ui.html
- API Docs: http://localhost:8088/api-docs

## Core APIs

### Execute AI Task
```bash
POST /api/v1/agent/run
Content-Type: application/json

{
  "appId": "app-001",
  "sceneId": "scene-001",
  "input": "Analyze this data for me",
  "sessionId": "session-001"
}
```

### Register Tool
```bash
POST /api/v1/tool/register
Content-Type: application/json

{
  "toolId": "weather-api",
  "name": "Weather Query",
  "type": "http",
  "description": "Query weather information for a specified city",
  "config": {
    "url": "https://api.weather.com/v1/query",
    "method": "GET"
  }
}
```

## Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI/DeepSeek API key | `sk-xxx` |
| `OPENAI_BASE_URL` | API base URL | `https://api.openai.com` |
| `OPENAI_MODEL` | Default model | `gpt-4` or `deepseek-chat` |
| `LANGCHAIN4J_API_KEY` | LangChain4j API key | Defaults to `OPENAI_API_KEY` |
| `EMBEDDING_API_KEY` | Embedding API key | Defaults to `OPENAI_API_KEY` |
| `SEARCH_API_KEY` | Search API key | Bing Search API |

## Tech Stack

| Component | Version | Description |
|-----------|---------|-------------|
| Java | 21 | GraalVM |
| Spring Boot | 3.2.x | Framework |
| LangChain4j | 0.30.x | AI Orchestration |
| Spring AI | 1.0.x | AI Integration |

## Contributing

Contributions are welcome! Please check [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

[Apache License 2.0](LICENSE)

## Acknowledgments

Thanks to these open source projects:
- [LangChain4j](https://github.com/langchain4j/langchain4j)
- [Spring AI](https://github.com/spring-projects/spring-ai)
- [Spring Boot](https://github.com/spring-projects/spring-boot)
