package com.team.agent.tools;

import com.team.agent.util.Json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 工具入参的 JSON Schema 构造器，用于向大模型和 MCP 客户端描述参数。
 */
public final class Schema {

    private final Map<String, Object> properties = Json.obj();
    private final List<String> required = new ArrayList<>();

    public static Schema create() {
        return new Schema();
    }

    public Schema property(String name, String type, String description) {
        Map<String, Object> property = Json.obj();
        property.put("type", type);
        property.put("description", description);
        properties.put(name, property);
        return this;
    }

    public Schema required(String... names) {
        required.addAll(Arrays.asList(names));
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> schema = Json.obj();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }
}
