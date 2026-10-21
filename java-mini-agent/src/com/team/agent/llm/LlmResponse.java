package com.team.agent.llm;

import java.util.Collections;
import java.util.List;

/**
 * 大模型返回结果，包含文本内容与本轮希望执行的工具调用。
 */
public class LlmResponse {

    private final String content;
    private final List<ToolCall> toolCalls;

    public LlmResponse(String content, List<ToolCall> toolCalls) {
        this.content = content == null ? "" : content;
        this.toolCalls = toolCalls == null ? Collections.emptyList() : toolCalls;
    }

    public String content() {
        return content;
    }

    public List<ToolCall> toolCalls() {
        return toolCalls;
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
