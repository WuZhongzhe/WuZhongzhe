package com.papercheck;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.papercheck.exception.FileProcessException;

public final class FileUtil {

    /** 统一使用 UTF-8 读写，避免中文乱码 */
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private FileUtil() {
    }

    /**
     * 读取文本文件内容。
     *
     * @param path 文件绝对路径
     * @return 文件全部内容（不含换行符）
     * @throws FileProcessException 文件不存在 / 是文件夹 / 不可读 / 读取失败
     */
    public static String readFile(String path) throws FileProcessException {
        checkReadable(path);
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path), DEFAULT_CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        } catch (IOException e) {
            throw new FileProcessException("读取文件失败：" + path, e);
        }
        return content.toString();
    }

    /**
     * 把内容写入文件，答案文件所在目录不存在时会自动创建。
     *
     * @param path    答案文件绝对路径
     * @param content 要写入的内容
     * @throws FileProcessException 路径为空 / 写入失败
     */
    public static void writeFile(String path, String content) throws FileProcessException {
        if (path == null || path.trim().isEmpty()) {
            throw new FileProcessException("答案文件路径不能为空");
        }
        try {
            Path out = Paths.get(path);
            Path parent = out.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(out, DEFAULT_CHARSET)) {
                writer.write(content);
            }
        } catch (IOException e) {
            throw new FileProcessException("写入文件失败：" + path, e);
        }
    }

    /** 读之前先检查路径合不合法，把问题尽早暴露出来 */
    private static void checkReadable(String path) throws FileProcessException {
        if (path == null || path.trim().isEmpty()) {
            throw new FileProcessException("文件路径不能为空");
        }
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            throw new FileProcessException("文件不存在：" + path);
        }
        if (!Files.isRegularFile(filePath)) {
            throw new FileProcessException("路径不是一个文件：" + path);
        }
        if (!Files.isReadable(filePath)) {
            throw new FileProcessException("文件没有读取权限：" + path);
        }
    }
}
