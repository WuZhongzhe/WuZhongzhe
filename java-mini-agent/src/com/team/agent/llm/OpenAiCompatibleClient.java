package com.team.agent.llm;

import com.team.agent.util.Http;
import com.team.agent.util.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 兼容 OpenAI Chat Completions 协议的大模型客户端。
 * DeepSeek、通义千问（兼容模式）、智谱、Moonshot、OpenAI 等服务只需更换地址与模型名即可接入。
 */
public class OpenAiCompatibleClient implements LlmClient {

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final double temperature;

    public OpenAiCompatibleClient(String baseUrl, String apiKey, String model, double temperature) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
    }

    @Override
    public String describe() {
        return model + "@" + baseUrl;
    }

    @Override
    public LlmResponse chat(List<ChatMessage> messages, List<Object> tools) throws Exception {
        List<Object> messageList = new ArrayList<>();
        for (ChatMessage message : messages) {
            messageList.add(message.toRequestMap());
        }
        Map<String, Object> body = Json.obj();
        body.put("model", model);
        body.put("messages", messageList);
        body.put("temperature", temperature);
        body.put("stream", Boolean.FALSE);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }
        String response = Http.post(baseUrl + "/chat/completions", Json.write(body), apiKey);
        Map<String, Object> parsed = Json.asObject(Json.parse(response));
        List<Object> choices = Json.asArray(parsed.get("choices"));
        if (choices.isEmpty()) {
            throw new IllegalStateException("模型返回内容为空：" + response);
        }
        Map<String, Object> message = Json.asObject(Json.asObject(choices.get(0)).get("message"));
        List<ToolCall> toolCalls = new ArrayList<>();
        Object rawCalls = message.get("tool_calls");
        if (rawCalls != null) {
            for (Object raw : Json.asArray(rawCalls)) {
                toolCalls.add(ToolCall.fromMap(Json.asObject(raw)));
            }
        }
        return new LlmResponse(Json.text(message, "content", ""), toolCalls);
    }
}
