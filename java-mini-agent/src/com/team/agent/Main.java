package com.team.agent;

import com.team.agent.core.AgentEngine;
import com.team.agent.core.ConversationStore;
import com.team.agent.core.Settings;
import com.team.agent.llm.LlmClient;
import com.team.agent.llm.OpenAiCompatibleClient;
import com.team.agent.llm.RuleBasedLlmClient;
import com.team.agent.mcp.McpServer;
import com.team.agent.tools.FileTools;
import com.team.agent.tools.MathTools;
import com.team.agent.tools.SystemTools;
import com.team.agent.tools.Tool;
import com.team.agent.tools.ToolRegistry;
import com.team.agent.tools.TodoStore;
import com.team.agent.tools.TodoTools;
import com.team.agent.tools.WeatherTools;
import com.team.agent.web.ApiServer;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 启动类：装配工具、模型客户端、记忆与接口服务。
 *
 * <pre>
 *   javac -d out $(find src -name "*.java")
 *   java -cp out com.team.agent.Main
 * </pre>
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Settings settings = new Settings();
        Path workspace = settings.workspace();
        Files.createDirectories(workspace);

        ToolRegistry registry = new ToolRegistry();
        registerAll(registry, new FileTools(workspace).tools());
        registerAll(registry, new WeatherTools().tools());
        registerAll(registry, new MathTools().tools());
        registerAll(registry, new TodoTools(new TodoStore(workspace.resolve("todos.json"))).tools());
        registerAll(registry, new SystemTools().tools());

        LlmClient llmClient = settings.offline()
                ? new RuleBasedLlmClient()
                : new OpenAiCompatibleClient(settings.baseUrl(), settings.apiKey(),
                settings.model(), settings.temperature());

        ConversationStore store = new ConversationStore(settings.memoryWindow(), workspace.resolve("history"));
        store.load();

        AgentEngine engine = new AgentEngine(llmClient, registry, store, settings.maxToolRounds());
        McpServer mcpServer = new McpServer(registry, "java-mini-agent", "1.0.0");

        System.out.println("工作区目录：" + workspace);
        System.out.println("已注册工具：" + registry.all().size() + " 个");
        if (settings.offline()) {
            System.out.println("未检测到 API Key，已启用离线规则模式（工具仍会真实执行）");
        }

        new ApiServer(engine, registry, store, mcpServer, llmClient, settings.webRoot()).start(settings.port());
    }

    private static void registerAll(ToolRegistry registry, Iterable<Tool> tools) {
        for (Tool tool : tools) {
            registry.register(tool);
        }
    }
}
