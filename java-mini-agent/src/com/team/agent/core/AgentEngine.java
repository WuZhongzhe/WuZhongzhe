package com.team.agent.core;

import com.team.agent.llm.ChatMessage;
import com.team.agent.llm.LlmClient;
import com.team.agent.llm.LlmResponse;
import com.team.agent.llm.ToolCall;
import com.team.agent.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 智能体核心：把「理解意图」和「执行动作」串成一个可循环的决策过程。
 *
 * <p>流程为：组装上下文 → 请求模型 → 若模型要求调用工具则执行并把结果回灌 →
 * 再次请求模型，直到模型给出最终回复或达到最大轮数。
 */
public class AgentEngine {

    private static final String DEFAULT_SYSTEM_PROMPT = String.join("\n", Arrays.asList(
            "你是一个能说会做的中文智能助理，运行在本地 Java 智能体框架上。",
            "你可以调用工具完成真实操作：读写文件、查询天气、维护待办清单、计算表达式、获取时间。",
            "原则：",
            "1. 需要真实数据或副作用操作时必须调用工具，不要凭空编造结果；",
            "2. 用简洁的中文回答，涉及文件时说明文件路径；",
            "3. 如果用户的要求不够明确，先用一句话确认再执行。"));

    private final LlmClient llmClient;
    private final ToolRegistry registry;
    private final ConversationStore store;
    private final String systemPrompt;
    private final int maxToolRounds;

    public AgentEngine(LlmClient llmClient, ToolRegistry registry, ConversationStore store, int maxToolRounds) {
        this(llmClient, registry, store, DEFAULT_SYSTEM_PROMPT, maxToolRounds);
    }

    public AgentEngine(LlmClient llmClient, ToolRegistry registry, ConversationStore store,
                       String systemPrompt, int maxToolRounds) {
        this.llmClient = llmClient;
        this.registry = registry;
        this.store = store;
        this.systemPrompt = systemPrompt;
        this.maxToolRounds = maxToolRounds;
    }

    /** 执行一次用户对话，返回最终回复与执行轨迹。 */
    public AgentResult chat(String conversationId, String userMessage) {
        List<String> trace = new ArrayList<>();
        List<String> toolsUsed = new ArrayList<>();
        List<ChatMessage> working = new ArrayList<>();
        working.add(ChatMessage.system(systemPrompt));
        working.addAll(store.history(conversationId));
        ChatMessage userChat = ChatMessage.user(userMessage);
        working.add(userChat);

        List<Object> tools = registry.toRequestTools();
        String reply = "";
        try {
            for (int round = 0; round <= maxToolRounds; round++) {
                LlmResponse response = llmClient.chat(working, tools);
                if (!response.hasToolCalls()) {
                    reply = response.content();
                    break;
                }
                working.add(ChatMessage.assistantWithCalls(response.content(), response.toolCalls()));
                for (ToolCall call : response.toolCalls()) {
                    String result = registry.execute(call.name(), call.arguments());
                    toolsUsed.add(call.name());
                    trace.add(describeCall(call, result));
                    working.add(ChatMessage.toolResult(call.id(), call.name(), result));
                }
                if (round == maxToolRounds) {
                    reply = "已达到单轮最大工具调用次数，下面是已获得的结果：\n" + lastToolResults(working);
                }
            }
        } catch (Exception e) {
            reply = "调用模型失败：" + e.getMessage();
            trace.add("模型调用异常：" + e.getMessage());
        }

        if (reply == null || reply.isEmpty()) {
            reply = "（模型没有返回内容）";
        }
        store.append(conversationId, userChat, ChatMessage.assistant(reply));
        return new AgentResult(reply, trace, toolsUsed);
    }

    private static String describeCall(ToolCall call, String result) {
        String preview = result.length() > 300 ? result.substring(0, 300) + "…" : result;
        return "调用 " + call.name() + " 参数 " + call.arguments() + " → " + preview;
    }

    private static String lastToolResults(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage message : messages) {
            if (message.role() == ChatMessage.Role.TOOL) {
                sb.append(message.toolName()).append("：").append(message.content()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /** 一次对话的结果，包含回复、工具轨迹与用到的工具名。 */
    public static class AgentResult {

        private final String reply;
        private final List<String> trace;
        private final List<String> toolsUsed;

        AgentResult(String reply, List<String> trace, List<String> toolsUsed) {
            this.reply = reply;
            this.trace = trace;
            this.toolsUsed = toolsUsed;
        }

        public String reply() {
            return reply;
        }

        public List<String> trace() {
            return trace;
        }

        public List<String> toolsUsed() {
            return toolsUsed;
        }
    }
}
