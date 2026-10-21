package com.team.agent.tools;

import java.util.Map;

/**
 * 智能体可调用的工具。
 * 每个工具以 JSON Schema 描述入参，智能体与 MCP 客户端都依据该描述决定如何调用。
 */
public interface Tool {

    /** 工具名称，对应 function calling 中的函数名，全局唯一。 */
    String name();

    /** 工具的自然语言描述，模型据此判断何时调用该工具。 */
    String description();

    /** 参数的 JSON Schema 定义。 */
    Map<String, Object> parameters();

    /**
     * 执行工具。
     *
     * @param arguments 已解析的参数，键为参数名
     * @return 交给模型的文本结果，需要简洁可读
     */
    String execute(Map<String, Object> arguments) throws Exception;
}
