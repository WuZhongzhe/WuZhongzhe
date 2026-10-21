package com.team.agent.llm;

import java.util.List;

/**
 * 大模型客户端抽象，屏蔽不同模型服务之间的差异。
 */
public interface LlmClient {

    /**
     * 发起一次对话。
     *
     * @param messages 完整上下文，包含系统提示、历史消息与工具结果
     * @param tools    以 OpenAI tools 格式描述的工具列表
     * @return 模型的回复，可能包含待执行的工具调用
     */
    LlmResponse chat(List<ChatMessage> messages, List<Object> tools) throws Exception;

    /** 客户端名称，用于在前端与日志中展示当前使用的模型。 */
    String describe();
}
