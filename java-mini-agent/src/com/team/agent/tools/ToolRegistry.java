package com.team.agent.tools;

import com.team.agent.util.Json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册表，负责工具的登记、查找与安全执行。
 */
public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) {
        if (tools.containsKey(tool.name())) {
            throw new IllegalStateException("重复注册工具：" + tool.name());
        }
        tools.put(tool.name(), tool);
    }

    public List<Tool> all() {
        return Collections.unmodifiableList(new ArrayList<>(tools.values()));
    }

    public Tool find(String name) {
        return tools.get(name);
    }

    /** 转换为 OpenAI tools 数组，供模型做 function calling。 */
    public List<Object> toRequestTools() {
        List<Object> result = new ArrayList<>();
        for (Tool tool : tools.values()) {
            Map<String, Object> function = Json.obj();
            function.put("name", tool.name());
            function.put("description", tool.description());
            function.put("parameters", tool.parameters());
            Map<String, Object> wrapper = Json.obj();
            wrapper.put("type", "function");
            wrapper.put("function", function);
            result.add(wrapper);
        }
        return result;
    }

    /**
     * 执行工具调用，任何异常都转换为文本返回，避免单次工具失败中断整个会话。
     */
    public String execute(String name, String argumentsJson) {
        Tool tool = tools.get(name);
        if (tool == null) {
            return "错误：未找到工具 " + name + "，可用工具：" + String.join(", ", tools.keySet());
        }
        Map<String, Object> arguments;
        try {
            Object parsed = Json.parse(argumentsJson == null || argumentsJson.isEmpty() ? "{}" : argumentsJson);
            arguments = Json.asObject(parsed);
        } catch (RuntimeException e) {
            return "错误：工具 " + name + " 的参数不是合法 JSON —— " + e.getMessage();
        }
        try {
            return tool.execute(arguments);
        } catch (Exception e) {
            return "错误：工具 " + name + " 执行失败 —— " + e.getMessage();
        }
    }
}
