package com.team.agent.llm;

import com.team.agent.util.Json;

import java.util.Map;
import java.util.UUID;

/**
 * 模型返回的一次工具调用请求，字段与 OpenAI function calling 保持一致。
 */
public class ToolCall {

    private final String id;
    private final String name;
    private final String arguments;

    public ToolCall(String id, String name, String arguments) {
        this.id = id == null || id.isEmpty() ? "call_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) : id;
        this.name = name;
        this.arguments = arguments == null ? "{}" : arguments;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String arguments() {
        return arguments;
    }

    public static ToolCall fromMap(Map<String, Object> map) {
        String id = Json.text(map, "id");
        String arguments;
        String name;
        Object function = map.get("function");
        if (function instanceof Map) {
            Map<String, Object> fn = Json.asObject(function);
            name = Json.text(fn, "name");
            arguments = Json.text(fn, "arguments", "{}");
        } else {
            name = Json.text(map, "name");
            arguments = Json.text(map, "arguments", "{}");
        }
        return new ToolCall(id, name, arguments);
    }

    @Override
    public String toString() {
        return name + "(" + arguments + ")";
    }
}
