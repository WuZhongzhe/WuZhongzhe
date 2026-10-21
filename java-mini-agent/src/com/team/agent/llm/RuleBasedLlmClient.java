package com.team.agent.llm;

import com.team.agent.util.Json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则式模型客户端，用于没有配置 API Key 时的离线演示与自动化测试。
 * 它按关键词判断用户意图并生成工具调用，调用结果仍由 AgentEngine 真实执行，
 * 因此离线模式下依然可以完整演示“理解意图 → 调用工具 → 汇总结果”的链路。
 */
public class RuleBasedLlmClient implements LlmClient {

    private static final Pattern CITY_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z]{2,10}?)(?:的)?天气");
    private static final Pattern FILE_PATTERN = Pattern.compile("[\\w\\u4e00-\\u9fa5./-]+\\.(?:txt|md|json|csv|log)");
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("[0-9(][0-9+\\-*/().^%\\s]*[0-9)]");
    private static final List<String> KNOWN_CITIES = Arrays.asList(
            "广州", "北京", "上海", "深圳", "杭州", "成都", "武汉", "西安", "南京", "重庆", "苏州", "长沙");

    @Override
    public String describe() {
        return "离线规则引擎（未配置 API Key）";
    }

    @Override
    public LlmResponse chat(List<ChatMessage> messages, List<Object> tools) {
        ChatMessage last = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        if (last != null && last.role() == ChatMessage.Role.TOOL) {
            return new LlmResponse(summarize(messages), null);
        }
        String input = last == null ? "" : String.valueOf(last.content());
        ToolCall call = detectToolCall(input, messages);
        if (call == null) {
            return new LlmResponse(plainReply(input), null);
        }
        return new LlmResponse("", new ArrayList<>(Arrays.asList(call)));
    }

    /** 工具已执行完毕，把结果整理成自然语言回复。 */
    private String summarize(List<ChatMessage> messages) {
        ChatMessage last = messages.get(messages.size() - 1);
        StringBuilder sb = new StringBuilder();
        sb.append("已调用工具 ").append(last.toolName()).append("，结果如下：\n").append(last.content());
        return sb.toString();
    }

    private String plainReply(String input) {
        return "当前运行在离线演示模式（未配置大模型 API Key），我只能按关键词触发工具。"
                + "你可以试试：『广州天气怎么样』『帮我算 (12+8)*3』『把这段话写到 notes/hello.md』"
                + "『加个待办：周五交实验报告』『看看待办清单』『现在几点』。"
                + (input.isEmpty() ? "" : "（收到：" + input + "）");
    }

    private ToolCall detectToolCall(String input, List<ChatMessage> history) {
        // 历史中出现过的文件路径，用于理解"里面""刚才那个文件"这类省略说法
        String rememberedPath = lastFilePath(history);
        if (input.contains("天气")) {
            String city = extractCity(input);
            return new ToolCall(null, "get_weather", Json.write(arguments("city", city)));
        }
        if (input.contains("待办") || input.contains("任务清单") || input.contains("提醒我")) {
            if (input.contains("完成") || input.contains("做完") || input.contains("搞定")) {
                return new ToolCall(null, "complete_todo", Json.write(arguments("keyword", firstNumberOrText(input))));
            }
            if (input.contains("删除") || input.contains("去掉")) {
                return new ToolCall(null, "remove_todo", Json.write(arguments("keyword", firstNumberOrText(input))));
            }
            if (input.contains("清单") || input.contains("列表") || input.contains("有哪些") || input.contains("看看")) {
                return new ToolCall(null, "list_todos", "{}");
            }
            return new ToolCall(null, "add_todo", Json.write(arguments("content", extractTodoContent(input))));
        }
        if (input.contains("文件") || mentionsFile(input)) {
            String path = extractFilePath(input, rememberedPath);
            if (input.contains("删")) {
                return new ToolCall(null, "delete_file", Json.write(arguments("path", path)));
            }
            if (input.contains("读") || input.contains("查看") || input.contains("打开") || input.contains("内容")
                    || input.contains("里面") || input.contains("写了什么") || input.contains("刚才")) {
                return new ToolCall(null, "read_file", Json.write(arguments("path", path)));
            }
            if (input.contains("列") || input.contains("有哪些") || input.contains("看看")) {
                return new ToolCall(null, "list_files", "{}");
            }
            Map<String, Object> arguments = Json.obj();
            arguments.put("path", path);
            arguments.put("content", "由智能体自动生成的示例内容：\n" + input);
            return new ToolCall(null, "write_file", Json.write(arguments));
        }
        if (input.contains("几点") || input.contains("几号") || input.contains("今天") || input.contains("时间")) {
            return new ToolCall(null, "current_time", "{}");
        }
        if (input.contains("系统") || input.contains("运行环境")) {
            return new ToolCall(null, "system_info", "{}");
        }
        Matcher matcher = EXPRESSION_PATTERN.matcher(input);
        if (matcher.find()) {
            String expression = matcher.group().trim();
            if (expression.matches(".*[+\\-*/^%].*")) {
                return new ToolCall(null, "calculate", Json.write(arguments("expression", expression)));
            }
        }
        return null;
    }

    /** 判断这句话是否隐含地指向文件，例如"看看里面写了什么"。 */
    private static boolean mentionsFile(String input) {
        return input.contains("里面") || input.contains("写了什么") || input.contains("刚才")
                || input.contains("这个文件") || input.contains("那个文件");
    }

    /** 从历史消息中找出最近一次出现的文件路径，模拟模型对上下文的指代理解。 */
    private static String lastFilePath(List<ChatMessage> history) {
        String found = "";
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage message = history.get(i);
            String text = message.content() == null ? "" : message.content();
            Matcher matcher = FILE_PATTERN.matcher(text);
            while (matcher.find()) {
                found = matcher.group();
            }
            if (!found.isEmpty()) {
                return found;
            }
        }
        return found;
    }

    private static Map<String, Object> arguments(String key, String value) {
        Map<String, Object> map = Json.obj();
        map.put(key, value);
        return map;
    }

    private static String extractCity(String input) {
        for (String city : KNOWN_CITIES) {
            if (input.contains(city)) {
                return city;
            }
        }
        Matcher matcher = CITY_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "广州";
    }

    private static String extractFilePath(String input, String rememberedPath) {
        Matcher matcher = FILE_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        // 本轮没写出文件名时，回退到上下文中最近提到的文件
        if (rememberedPath != null && !rememberedPath.isEmpty()) {
            return rememberedPath;
        }
        return "notes/agent-demo.md";
    }

    private static String extractTodoContent(String input) {
        String text = input.replaceAll("^(帮我|请|麻烦)?(加|添加|新增|记一下|提醒我)(个|一条)?(待办|任务)?[:：]?", "");
        text = text.replace("待办", "").replace("任务", "").trim();
        return text.isEmpty() ? "未命名待办" : text;
    }

    private static String firstNumberOrText(String input) {
        Matcher matcher = Pattern.compile("\\d+").matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        return extractTodoContent(input);
    }
}
