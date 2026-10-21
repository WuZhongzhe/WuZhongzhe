package com.team.agent.tools;

import com.team.agent.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 待办事项的持久化存储，数据以 JSON 保存在工作区中。
 */
public class TodoStore {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Path file;

    public TodoStore(Path file) {
        this.file = file;
    }

    public synchronized List<Map<String, Object>> load() throws IOException {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        if (text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Object> raw = Json.asArray(Json.parse(text));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : raw) {
            result.add(Json.asObject(item));
        }
        return result;
    }

    public synchronized void save(List<Map<String, Object>> items) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(file, Json.write(items).getBytes(StandardCharsets.UTF_8));
    }

    public synchronized Map<String, Object> add(String content) throws IOException {
        List<Map<String, Object>> items = load();
        Map<String, Object> item = Json.obj();
        item.put("content", content);
        item.put("done", Boolean.FALSE);
        item.put("createdAt", LocalDateTime.now().format(FORMATTER));
        items.add(item);
        save(items);
        return item;
    }

    public static String render(List<Map<String, Object>> items) {
        if (items.isEmpty()) {
            return "当前没有待办事项。";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            boolean done = Boolean.TRUE.equals(item.get("done"));
            sb.append(i + 1).append(". [").append(done ? "x" : " ").append("] ")
                    .append(item.get("content"))
                    .append("（创建于 ").append(item.get("createdAt")).append("）\n");
        }
        return sb.toString().trim();
    }
}
