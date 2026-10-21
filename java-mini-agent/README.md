# Mini Agent：能说会做的 Java 智能体

一个使用 **纯 Java（JDK 11，零第三方依赖）** 实现的 AI 智能体：既能用自然语言对话，也能真实调用工具
（读写文件、查询天气、计算表达式、维护待办清单），并且兼容 MCP（Model Context Protocol）协议对外暴露工具。

## 特性

- **能说**：接入任何兼容 OpenAI Chat Completions 协议的大模型（DeepSeek、通义千问、智谱、OpenAI 等）。
- **会做**：13 个内置工具，通过 function calling 触发，工具调用结果会回灌给模型继续推理（最多 5 轮）。
- **记得住**：多轮对话记忆按会话隔离，保留最近 20 条消息并持久化到磁盘，重启后可继续。
- **可接入**：提供 MCP 标准 JSON-RPC 端点（`/mcp`），支持 `initialize`、`tools/list`、`tools/call`。
- **有界面**：内置网页对话前端，可查看每一次工具调用的参数与结果。
- **零依赖**：只使用 JDK 标准库（`java.net.http`、`com.sun.net.httpserver`），`javac` 编译即可运行。

## 目录结构

```
ai-agent/
├── src/com/team/agent/
│   ├── Main.java                      启动入口，装配各模块
│   ├── core/
│   │   ├── AgentEngine.java           决策循环：理解意图 → 调用工具 → 汇总结果
│   │   ├── ConversationStore.java     多轮对话记忆与持久化
│   │   └── Settings.java              运行配置（环境变量）
│   ├── llm/
│   │   ├── LlmClient.java             模型客户端接口
│   │   ├── OpenAiCompatibleClient.java OpenAI 兼容协议实现
│   │   ├── RuleBasedLlmClient.java    无 API Key 时的离线演示引擎
│   │   ├── ChatMessage.java           消息模型
│   │   ├── ToolCall.java  LlmResponse.java
│   ├── tools/                         工具层（文件、天气、计算、待办、系统）
│   ├── mcp/McpServer.java             MCP JSON-RPC 服务端
│   ├── web/ApiServer.java             REST 接口与静态资源服务
│   └── util/                          JSON 解析、HTTP 封装
├── web/                               网页前端（index.html / style.css / app.js）
├── docs/                              说明文档与博客随笔
├── build.sh  run.sh                   macOS / Linux 构建与启动脚本
└── build.bat run.bat                  Windows 构建与启动脚本
```

## 快速开始

### Windows

双击 `build.bat` 编译，再双击 `run.bat` 运行即可（脚本会自动切换 UTF-8 代码页，中文不会乱码）。
也可以在命令行里执行，方便接入真实模型：

```bat
cd /d D:\your-path\ai-agent
build.bat

rem 接入真实模型，然后启动
set DEEPSEEK_API_KEY=sk-xxxxxx
run.bat
```

### macOS / Linux

```bash
# 1. 编译
./build.sh

# 2. 运行（未配置 API Key 时自动进入离线演示模式，工具仍会真实执行）
./run.sh

# 3. 接入真实大模型（以 DeepSeek 为例）
export DEEPSEEK_API_KEY=sk-xxxxxx
./run.sh
```

浏览器打开 <http://localhost:8080> 即可对话。

> Windows 注意：需要 JDK 11 或更高版本（JDK 8 没有 `java.net.http`，会编译失败）；
> 首次启动时若弹出防火墙提示，选择"允许访问"，否则浏览器打不开 8080 端口。

## 环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `AGENT_API_KEY` / `DEEPSEEK_API_KEY` / `OPENAI_API_KEY` | 空 | 大模型密钥，为空则进入离线演示模式 |
| `AGENT_BASE_URL` | `https://api.deepseek.com` | 兼容 OpenAI 协议的服务地址 |
| `AGENT_MODEL` | `deepseek-flash` | 模型名称（DeepSeek 当前推荐 `deepseek-flash`） |
| `AGENT_PORT` | `8080` | HTTP 服务端口 |
| `AGENT_WORKSPACE` | `workspace` | 智能体可操作的文件工作区 |
| `AGENT_MAX_ROUNDS` | `5` | 单次对话最多工具轮数 |
| `AGENT_MEMORY` | `20` | 每个会话保留的消息条数 |

> 模型名说明：DeepSeek 的 `deepseek-chat` 与 `deepseek-reasoner` 已下线，
> 现在使用 `deepseek-flash`（V4.1 Flash）或 `deepseek-v4-pro`（V4 Pro）；
> 官方已宣布 V4 Pro 有序退役，请求会被路由到 V4.1 Flash，因此默认选择 `deepseek-flash`。
> 若接口地址返回 404，可尝试 `AGENT_BASE_URL=https://api.deepseek.com/v1`。

## REST 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/chat` | 对话，入参 `{conversationId, message}` |
| GET | `/api/tools` | 查看已注册工具与当前模型 |
| GET | `/api/conversations` | 查看历史会话 |
| POST | `/api/reset` | 清空指定会话记忆 |
| POST | `/mcp` | MCP JSON-RPC 端点 |

示例：

```bash
curl -X POST http://localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"conversationId":"demo","message":"帮我算一下 (12 + 8) * 3 / 2"}'
```

## MCP 接入

```bash
curl -X POST http://localhost:8080/mcp -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'

curl -X POST http://localhost:8080/mcp -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"calculate","arguments":{"expression":"2^10+8*3"}}}'
```

## 安全说明

文件类工具只能访问 `AGENT_WORKSPACE` 指定目录，路径中出现的 `..` 会在解析后被校验并拒绝，
避免智能体越权读写系统文件。
