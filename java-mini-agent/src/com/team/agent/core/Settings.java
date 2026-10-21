package com.team.agent.core;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 运行配置，优先读取环境变量，未配置时使用默认值。
 */
public class Settings {

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int port;
    private final int maxToolRounds;
    private final int memoryWindow;
    private final Path workspace;
    private final Path webRoot;

    public Settings() {
        this.baseUrl = env("AGENT_BASE_URL", "https://api.deepseek.com");
        this.apiKey = firstNonEmpty(env("AGENT_API_KEY", ""), env("DEEPSEEK_API_KEY", ""),
                env("OPENAI_API_KEY", ""));
        this.model = env("AGENT_MODEL", "deepseek-flash");
        this.temperature = Double.parseDouble(env("AGENT_TEMPERATURE", "0.3"));
        this.port = Integer.parseInt(env("AGENT_PORT", "8080"));
        this.maxToolRounds = Integer.parseInt(env("AGENT_MAX_ROUNDS", "5"));
        this.memoryWindow = Integer.parseInt(env("AGENT_MEMORY", "20"));
        this.workspace = Paths.get(env("AGENT_WORKSPACE", "workspace")).toAbsolutePath().normalize();
        this.webRoot = Paths.get(env("AGENT_WEB", "web")).toAbsolutePath().normalize();
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String apiKey() {
        return apiKey;
    }

    public String model() {
        return model;
    }

    public double temperature() {
        return temperature;
    }

    public int port() {
        return port;
    }

    public int maxToolRounds() {
        return maxToolRounds;
    }

    public int memoryWindow() {
        return memoryWindow;
    }

    public Path workspace() {
        return workspace;
    }

    public Path webRoot() {
        return webRoot;
    }

    public boolean offline() {
        return apiKey == null || apiKey.trim().isEmpty();
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}
