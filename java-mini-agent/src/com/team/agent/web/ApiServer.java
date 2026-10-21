package com.team.agent.web;

import com.team.agent.core.AgentEngine;
import com.team.agent.core.ConversationStore;
import com.team.agent.llm.LlmClient;
import com.team.agent.mcp.McpServer;
import com.team.agent.tools.Tool;
import com.team.agent.tools.ToolRegistry;
import com.team.agent.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * 基于 JDK 内置 HttpServer 的接口层，为网页前端和 MCP 客户端提供服务。
 */
public class ApiServer {

    private final AgentEngine engine;
    private final ToolRegistry registry;
    private final ConversationStore store;
    private final McpServer mcpServer;
    private final LlmClient llmClient;
    private final Path webRoot;

    public ApiServer(AgentEngine engine, ToolRegistry registry, ConversationStore store,
                     McpServer mcpServer, LlmClient llmClient, Path webRoot) {
        this.engine = engine;
        this.registry = registry;
        this.store = store;
        this.mcpServer = mcpServer;
        this.llmClient = llmClient;
        this.webRoot = webRoot;
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleStatic);
        server.createContext("/api/chat", this::handleChat);
        server.createContext("/api/tools", this::handleTools);
        server.createContext("/api/conversations", this::handleConversations);
        server.createContext("/api/reset", this::handleReset);
        server.createContext("/mcp", this::handleMcp);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("智能体已启动：http://localhost:" + port);
        System.out.println("当前模型：" + llmClient.describe());
    }

    private void handleChat(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"仅支持 POST\"}");
            return;
        }
        Map<String, Object> body = readBody(exchange);
        String conversationId = Json.text(body, "conversationId");
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = UUID.randomUUID().toString().substring(0, 8);
        }
        String message = Json.text(body, "message", "");
        AgentEngine.AgentResult result = engine.chat(conversationId, message);
        Map<String, Object> response = Json.obj();
        response.put("conversationId", conversationId);
        response.put("reply", result.reply());
        response.put("trace", result.trace());
        response.put("toolsUsed", result.toolsUsed());
        sendJson(exchange, 200, Json.write(response));
    }

    private void handleTools(HttpExchange exchange) throws IOException {
        List<Object> tools = new ArrayList<>();
        for (Tool tool : registry.all()) {
            Map<String, Object> item = Json.obj();
            item.put("name", tool.name());
            item.put("description", tool.description());
            tools.add(item);
        }
        Map<String, Object> response = Json.obj();
        response.put("tools", tools);
        response.put("model", llmClient.describe());
        sendJson(exchange, 200, Json.write(response));
    }

    private void handleConversations(HttpExchange exchange) throws IOException {
        Map<String, Object> response = Json.obj();
        response.put("conversations", store.conversationIds());
        sendJson(exchange, 200, Json.write(response));
    }

    private void handleReset(HttpExchange exchange) throws IOException {
        Map<String, Object> body = readBody(exchange);
        String conversationId = Json.text(body, "conversationId", "");
        store.clear(conversationId);
        sendJson(exchange, 200, "{\"ok\":true}");
    }

    private void handleMcp(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"仅支持 POST\"}");
            return;
        }
        String body = new String(readBytes(exchange), StandardCharsets.UTF_8);
        String response = mcpServer.handle(body);
        if (response == null) {
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
            return;
        }
        sendJson(exchange, 200, response);
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }
        Path target = webRoot.resolve(path.substring(1)).normalize();
        if (!target.startsWith(webRoot) || !Files.isRegularFile(target)) {
            sendJson(exchange, 404, "{\"error\":\"资源不存在\"}");
            return;
        }
        byte[] bytes = Files.readAllBytes(target);
        exchange.getResponseHeaders().add("Content-Type", contentType(target.getFileName().toString()));
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String contentType(String fileName) {
        if (fileName.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (fileName.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (fileName.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (fileName.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    private static Map<String, Object> readBody(HttpExchange exchange) throws IOException {
        String text = new String(readBytes(exchange), StandardCharsets.UTF_8);
        if (text.trim().isEmpty()) {
            return Json.obj();
        }
        return Json.asObject(Json.parse(text));
    }

    private static byte[] readBytes(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int length;
            while ((length = in.read(chunk)) > 0) {
                buffer.write(chunk, 0, length);
            }
            return buffer.toByteArray();
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
