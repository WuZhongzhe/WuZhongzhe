package com.team.agent.core;

import com.team.agent.llm.ChatMessage;
import com.team.agent.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 多轮对话记忆。
 * 每个会话保留最近的若干条消息，并在磁盘上持久化，服务重启后仍可接着聊。
 */
public class ConversationStore {

    private final int windowSize;
    private final Path directory;
    private final Map<String, List<ChatMessage>> conversations = new LinkedHashMap<>();

    public ConversationStore(int windowSize, Path directory) {
        this.windowSize = windowSize;
        this.directory = directory;
    }

    /** 启动时加载磁盘上的历史会话。 */
    public void load() {
        if (directory == null || !Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> stream = Files.list(directory)) {
            List<Path> files = stream.filter(path -> path.toString().endsWith(".json")).collect(Collectors.toList());
            for (Path file : files) {
                String id = file.getFileName().toString().replace(".json", "");
                String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                List<Object> raw = Json.asArray(Json.parse(text));
                List<ChatMessage> messages = new ArrayList<>();
                for (Object item : raw) {
                    messages.add(ChatMessage.fromMap(Json.asObject(item)));
                }
                conversations.put(id, messages);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("加载历史会话失败：" + e.getMessage());
        }
    }

    public synchronized List<ChatMessage> history(String conversationId) {
        List<ChatMessage> messages = conversations.get(conversationId);
        if (messages == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public synchronized void append(String conversationId, ChatMessage... messages) {
        List<ChatMessage> history = conversations.computeIfAbsent(conversationId, key -> new ArrayList<>());
        history.addAll(Arrays.asList(messages));
        while (history.size() > windowSize) {
            history.remove(0);
        }
        persist(conversationId, history);
    }

    public synchronized void clear(String conversationId) {
        conversations.remove(conversationId);
        if (directory != null) {
            try {
                Files.deleteIfExists(directory.resolve(conversationId + ".json"));
            } catch (IOException ignored) {
                // 历史文件删除失败不影响会话继续使用
            }
        }
    }

    public synchronized List<String> conversationIds() {
        return new ArrayList<>(conversations.keySet());
    }

    /** 只持久化用户与助手的文本消息，工具调用过程不写入磁盘。 */
    private void persist(String conversationId, List<ChatMessage> messages) {
        if (directory == null) {
            return;
        }
        List<Object> payload = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (message.role() == ChatMessage.Role.SYSTEM || message.role() == ChatMessage.Role.TOOL) {
                continue;
            }
            payload.add(message.toStorageMap());
        }
        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve(conversationId + ".json"),
                    Json.write(payload).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("保存会话失败：" + e.getMessage());
        }
    }
}
