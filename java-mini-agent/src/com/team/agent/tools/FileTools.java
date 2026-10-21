package com.team.agent.tools;

import com.team.agent.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件类工具：在工作区内创建、读取、追加、罗列和删除文件。
 * 所有路径都被限制在工作区目录内，避免智能体越权访问系统文件。
 */
public class FileTools {

    private final Path root;

    public FileTools(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public List<Tool> tools() {
        return Arrays.asList(new WriteFile(), new ReadFile(), new AppendFile(), new ListFiles(), new DeleteFile());
    }

    /** 把用户给出的相对路径解析到工作区内，越界时抛出异常。 */
    private Path resolve(String relativePath) {
        Path target = root.resolve(relativePath == null ? "" : relativePath).normalize().toAbsolutePath();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("不允许访问工作区之外的路径：" + relativePath);
        }
        return target;
    }

    private static String require(Map<String, Object> arguments, String key) {
        String value = Json.text(arguments, key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("缺少必要参数：" + key);
        }
        return value;
    }

    /** 写入文件，父目录不存在时自动创建。 */
    public class WriteFile implements Tool {

        @Override
        public String name() {
            return "write_file";
        }

        @Override
        public String description() {
            return "把文本内容写入工作区中的指定文件，文件不存在则创建，存在则覆盖。用于生成笔记、报告、清单等文件。";
        }

        @Override
        public Map<String, Object> parameters() {
            return Schema.create()
                    .property("path", "string", "相对工作区的文件路径，例如 notes/plan.md")
                    .property("content", "string", "要写入的完整文本内容")
                    .required("path", "content")
                    .build();
        }

        @Override
        public String execute(Map<String, Object> arguments) throws IOException {
            Path file = resolve(require(arguments, "path"));
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(file, require(arguments, "content").getBytes(StandardCharsets.UTF_8));
            return "已写入文件 " + root.relativize(file) + "（" + Files.size(file) + " 字节）";
        }
    }

    /** 读取文件内容。 */
    public class ReadFile implements Tool {

        @Override
        public String name() {
            return "read_file";
        }

        @Override
        public String description() {
            return "读取工作区中指定文件的文本内容，用于查看或确认已有文件。";
        }

        @Override
        public Map<String, Object> parameters() {
            return Schema.create()
                    .property("path", "string", "相对工作区的文件路径")
                    .required("path")
                    .build();
        }

        @Override
        public String execute(Map<String, Object> arguments) throws IOException {
            Path file = resolve(require(arguments, "path"));
            if (!Files.exists(file)) {
                return "文件不存在：" + root.relativize(file);
            }
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        }
    }

    /** 向文件末尾追加内容。 */
    public class AppendFile implements Tool {

        @Override
        public String name() {
            return "append_file";
        }

        @Override
        public String description() {
            return "在已有文件末尾追加一段文本，文件不存在时等同于创建文件，适合记录日志或逐条待办。";
        }

        @Override
        public Map<String, Object> parameters() {
            return Schema.create()
                    .property("path", "string", "相对工作区的文件路径")
                    .property("content", "string", "要追加的文本内容")
                    .required("path", "content")
                    .build();
        }

        @Override
        public String execute(Map<String, Object> arguments) throws IOException {
            Path file = resolve(require(arguments, "path"));
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(file, require(arguments, "content").getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return "已追加内容到 " + root.relativize(file);
        }
    }

    /** 列出工作区（或子目录）中的文件。 */
    public class ListFiles implements Tool {

        @Override
        public String name() {
            return "list_files";
        }

        @Override
        public String description() {
            return "列出工作区目录中的文件和子目录，默认只展示一层。";
        }

        @Override
        public Map<String, Object> parameters() {
            return Schema.create()
                    .property("path", "string", "相对工作区的子目录，留空表示工作区根目录")
                    .build();
        }

        @Override
        public String execute(Map<String, Object> arguments) throws IOException {
            String relative = Json.text(arguments, "path", "");
            Path dir = resolve(relative);
            if (!Files.isDirectory(dir)) {
                return "目录不存在：" + relative;
            }
            try (Stream<Path> stream = Files.list(dir)) {
                List<String> items = stream
                        .map(path -> (Files.isDirectory(path) ? "[目录] " : "[文件] ") + root.relativize(path))
                        .sorted()
                        .collect(Collectors.toList());
                if (items.isEmpty()) {
                    return "目录为空：" + (relative.isEmpty() ? "." : relative);
                }
                return String.join("\n", items);
            }
        }
    }

    /** 删除工作区中的文件。 */
    public class DeleteFile implements Tool {

        @Override
        public String name() {
            return "delete_file";
        }

        @Override
        public String description() {
            return "删除工作区中的指定文件。";
        }

        @Override
        public Map<String, Object> parameters() {
            return Schema.create()
                    .property("path", "string", "相对工作区的文件路径")
                    .required("path")
                    .build();
        }

        @Override
        public String execute(Map<String, Object> arguments) throws IOException {
            Path file = resolve(require(arguments, "path"));
            if (!Files.exists(file)) {
                return "文件不存在，无需删除：" + root.relativize(file);
            }
            Files.delete(file);
            return "已删除文件 " + root.relativize(file);
        }
    }
}
