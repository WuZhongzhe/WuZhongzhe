package com.team.agent.llm;

import com.team.agent.util.Json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 对话消息，兼容 OpenAI Chat Completions 协议的消息格式。
 */
public class ChatMessage {

    public enum Role {
        SYSTEM("system"),
        USER("user"),
        ASSISTANT("assistant"),
        TOOL("tool");

        private final String value;

        Role(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    private final Role role;
    private final String content;
    private final String toolCallId;
    private final String toolName;
    private final List<ToolCall> toolCalls;

    private ChatMessage(Role role, String content, String toolCallId, String toolName, List<ToolCall> toolCalls) {
        this.role = role;
        this.content = content;
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.toolCalls = toolCalls == null ? Collections.emptyList() : toolCalls;
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(Role.SYSTEM, content, null, null, null);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(Role.USER, content, null, null, null);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(Role.ASSISTANT, content, null, null, null);
    }

    public static ChatMessage assistantWithCalls(String content, List<ToolCall> toolCalls) {
        return new ChatMessage(Role.ASSISTANT, content == null ? "" : content, null, null, toolCalls);
    }

    public static ChatMessage toolResult(String toolCallId, String toolName, String content) {
        return new ChatMessage(Role.TOOL, content, toolCallId, toolName, null);
    }

    public Role role() {
        return role;
    }

    public String content() {
        return content;
    }

    public String toolCallId() {
        return toolCallId;
    }

    public String toolName() {
        return toolName;
    }

    public List<ToolCall> toolCalls() {
        return toolCalls;
    }

    /** 转换为请求体中的消息对象。 */
    public Map<String, Object> toRequestMap() {
        Map<String, Object> map = Json.obj();
        map.put("role", role.value());
        if (role == Role.ASSISTANT && !toolCalls.isEmpty()) {
            List<Object> calls = new ArrayList<>();
            for (ToolCall call : toolCalls) {
                Map<String, Object> callMap = Json.obj();
                callMap.put("id", call.id());
                callMap.put("type", "function");
                Map<String, Object> function = Json.obj();
                function.put("name", call.name());
                function.put("arguments", call.arguments());
                callMap.put("function", function);
                calls.add(callMap);
            }
            map.put("tool_calls", calls);
            if (content != null && !content.isEmpty()) {
                map.put("content", content);
            }
            return map;
        }
        if (role == Role.TOOL) {
            map.put("tool_call_id", toolCallId);
            if (toolName != null) {
                map.put("name", toolName);
            }
        }
        map.put("content", content == null ? "" : content);
        return map;
    }

    /** 反序列化，用于加载磁盘上的历史会话。 */
    @SuppressWarnings("unchecked")
    public static ChatMessage fromMap(Map<String, Object> map) {
        String role = Json.text(map, "role", "user");
        String content = Json.text(map, "content", "");
        if ("tool".equals(role)) {
            return toolResult(Json.text(map, "tool_call_id"), Json.text(map, "name"), content);
        }
        List<Object> rawCalls = map.get("tool_calls") == null ? null : (List<Object>) map.get("tool_calls");
        if (rawCalls != null && !rawCalls.isEmpty()) {
            List<ToolCall> calls = new ArrayList<>();
            for (Object raw : rawCalls) {
                Map<String, Object> call = (Map<String, Object>) raw;
                calls.add(ToolCall.fromMap(call));
            }
            return new ChatMessage(Role.ASSISTANT, content, null, null, calls);
        }
        switch (role) {
            case "system":
                return system(content);
            case "assistant":
                return assistant(content);
            default:
                return user(content);
        }
    }

    /** 序列化后写入磁盘，仅保存纯文本消息，工具调用过程不落盘。 */
    public Map<String, Object> toStorageMap() {
        Map<String, Object> map = Json.obj();
        map.put("role", role.value());
        map.put("content", content == null ? "" : content);
        return map;
    }
}
