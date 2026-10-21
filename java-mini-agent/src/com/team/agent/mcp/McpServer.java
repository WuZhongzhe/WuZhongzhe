package com.team.agent.mcp;

import com.team.agent.tools.Tool;
import com.team.agent.tools.ToolRegistry;
import com.team.agent.util.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP（Model Context Protocol）兼容服务端。
 * 以 JSON-RPC 2.0 的形式对外暴露本机工具，支持 initialize、tools/list、tools/call 三个方法，
 * 任何遵循 MCP 规范的客户端都可以直接接入本智能体的工具能力。
 */
public class McpServer {

    private static final String PROTOCOL_VERSION = "2024-11-05";

    private final ToolRegistry registry;
    private final String serverName;
    private final String serverVersion;

    public McpServer(ToolRegistry registry, String serverName, String serverVersion) {
        this.registry = registry;
        this.serverName = serverName;
        this.serverVersion = serverVersion;
    }

    /**
     * 处理一条 JSON-RPC 请求。
     *
     * @param requestBody 请求体原文
     * @return 响应体；通知类消息返回 null，由调用方决定是否响应
     */
    public String handle(String requestBody) {
        Object id = null;
        try {
            Map<String, Object> request = Json.asObject(Json.parse(requestBody));
            id = request.get("id");
            String method = Json.text(request, "method", "");
            Map<String, Object> params = request.get("params") instanceof Map
                    ? Json.asObject(request.get("params")) : Json.obj();
            switch (method) {
                case "initialize":
                    return success(id, initializeResult());
                case "tools/list":
                    return success(id, toolsListResult());
                case "tools/call":
                    return success(id, toolsCallResult(params));
                default:
                    return error(id, -32601, "不支持的方法：" + method);
            }
        } catch (RuntimeException e) {
            return error(id, -32700, "请求解析失败：" + e.getMessage());
        }
    }

    private Map<String, Object> initializeResult() {
        Map<String, Object> capabilities = Json.obj();
        Map<String, Object> toolsCapability = Json.obj();
        toolsCapability.put("listChanged", Boolean.FALSE);
        capabilities.put("tools", toolsCapability);
        Map<String, Object> serverInfo = Json.obj();
        serverInfo.put("name", serverName);
        serverInfo.put("version", serverVersion);
        Map<String, Object> result = Json.obj();
        result.put("protocolVersion", PROTOCOL_VERSION);
        result.put("capabilities", capabilities);
        result.put("serverInfo", serverInfo);
        return result;
    }

    private Map<String, Object> toolsListResult() {
        List<Object> tools = new ArrayList<>();
        for (Tool tool : registry.all()) {
            Map<String, Object> item = Json.obj();
            item.put("name", tool.name());
            item.put("description", tool.description());
            item.put("inputSchema", tool.parameters());
            tools.add(item);
        }
        Map<String, Object> result = Json.obj();
        result.put("tools", tools);
        return result;
    }

    private Map<String, Object> toolsCallResult(Map<String, Object> params) {
        String name = Json.text(params, "name", "");
        Object arguments = params.get("arguments");
        String argumentsJson = arguments == null ? "{}" : Json.write(arguments);
        String text = registry.execute(name, argumentsJson);
        List<Object> content = new ArrayList<>();
        Map<String, Object> item = Json.obj();
        item.put("type", "text");
        item.put("text", text);
        content.add(item);
        Map<String, Object> result = Json.obj();
        result.put("content", content);
        result.put("isError", text.startsWith("错误："));
        return result;
    }

    private static String success(Object id, Map<String, Object> result) {
        Map<String, Object> response = Json.obj();
        response.put("jsonrpc", "2.0");
        response.put("id", id == null ? Json.NULL : id);
        response.put("result", result);
        return Json.write(response);
    }

    private static String error(Object id, int code, String message) {
        Map<String, Object> response = Json.obj();
        response.put("jsonrpc", "2.0");
        response.put("id", id == null ? Json.NULL : id);
        Map<String, Object> error = Json.obj();
        error.put("code", code);
        error.put("message", message);
        response.put("error", error);
        return Json.write(response);
    }
}
