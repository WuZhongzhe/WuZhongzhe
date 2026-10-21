package com.team.agent.tools;

import com.team.agent.util.Json;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 待办事项工具，让智能体可以维护一份跨会话的任务清单。
 */
public class TodoTools {

    private final TodoStore store;

    public TodoTools(TodoStore store) {
        this.store = store;
    }

    public List<Tool> tools() {
        return Arrays.asList(new AddTodo(), new ListTodos(), new CompleteTodo(), new RemoveTodo());
    }

    public class AddTodo implements Tool {

        @Override
        public String name() {
            return "add_todo";
        }

        @Override
        public String description() {
            return "向待办清单中添加一条事项，例如“周五前提交实验报告”。";
        }

        @Override
        public Map<String, Object> parameters() {
            return Schema.create()
                    .property("content", "string", "待办事项的内容")
                    .required("content")
                    .build();
        }

        @Override
        public String execute(Map<String, Object> arguments) throws IOException {
            String content = Json.text(arguments, "content");
            if (content == null || content.trim().isEmpty()) {
                return "缺少必要参数：content";
            }
            store.add(content.trim());
            return "已添加待办：" + content.trim() + "\n" + TodoStore.render(store.load());
        }
    }

    public class ListTodos implements Tool {

        @Override
        public String name() {
            return "list_todos";
        }

        @Override
        public String description() {
            return "查看当前所有待办事项及其完成状态。";
        }

        @Override
        public Map<String, Object> parameters() {
            return Schema.create().build();
        }

        @Override
        public String execute(Map<String, Object> arguments) throws IOException {
            return TodoStore.render(store.load());
        }
    }

    public class CompleteTodo implements Tool {

        @Override
        public String name() {
            return "complete_todo";
        }

        @Override
        public String description() {
            return "把某条待办标记为已完成，可以按序号或内容关键字匹配。";
        }

        @Override
        public Map<String, Object> parameters() {
            return Schema.create()
                    .property("keyword", "string", "待办序号（如 1）或内容关键字")
                    .required("keyword")
                    .build();
        }

        @Override
        public String execute(Map<String, Object> arguments) throws IOException {
            String keyword = Json.text(arguments, "keyword", "").trim();
            List<Map<String, Object>> items = store.load();
            Map<String, Object> target = match(items, keyword);
            if (target == null) {
                return "没有匹配到待办：" + keyword;
            }
            target.put("done", Boolean.TRUE);
            store.save(items);
            return "已完成：" + target.get("content") + "\n" + TodoStore.render(items);
        }
    }

    public class RemoveTodo implements Tool {

        @Override
        public String name() {
            return "remove_todo";
        }

        @Override
        public String description() {
            return "删除某条待办事项，按序号或内容关键字匹配。";
        }

        @Override
        public Map<String, Object> parameters() {
            return Schema.create()
                    .property("keyword", "string", "待办序号（如 1）或内容关键字")
                    .required("keyword")
                    .build();
        }

        @Override
        public String execute(Map<String, Object> arguments) throws IOException {
            String keyword = Json.text(arguments, "keyword", "").trim();
            List<Map<String, Object>> items = store.load();
            Map<String, Object> target = match(items, keyword);
            if (target == null) {
                return "没有匹配到待办：" + keyword;
            }
            items.remove(target);
            store.save(items);
            return "已删除：" + target.get("content") + "\n" + TodoStore.render(items);
        }
    }

    /** 先按序号匹配，再按关键字模糊匹配。 */
    private static Map<String, Object> match(List<Map<String, Object>> items, String keyword) {
        try {
            int index = Integer.parseInt(keyword) - 1;
            if (index >= 0 && index < items.size()) {
                return items.get(index);
            }
        } catch (NumberFormatException ignored) {
            // 不是序号时退化为关键字匹配
        }
        for (Map<String, Object> item : items) {
            if (String.valueOf(item.get("content")).contains(keyword)) {
                return item;
            }
        }
        return null;
    }
}
