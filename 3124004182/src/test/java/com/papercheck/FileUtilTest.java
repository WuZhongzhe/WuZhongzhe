package com.papercheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.papercheck.exception.FileProcessException;

/**
 * FileUtil 的单元测试，用 @TempDir 造临时目录，测完自动清理。
 */
class FileUtilTest {

    @Test
    @DisplayName("写入后能原样读出来")
    void testWriteAndRead(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("a.txt");
        FileUtil.writeFile(file.toString(), "今天是星期天");
        assertEquals("今天是星期天", FileUtil.readFile(file.toString()));
    }

    @Test
    @DisplayName("答案文件所在目录不存在时自动创建")
    void testWriteCreateParentDir(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("sub").resolve("ans.txt");
        FileUtil.writeFile(file.toString(), "0.85");
        assertTrue(Files.exists(file));
        assertEquals("0.85", FileUtil.readFile(file.toString()));
    }

    @Test
    @DisplayName("读取不存在的文件抛出 FileProcessException")
    void testReadNotExistFile(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("no-such-file.txt");
        FileProcessException e = assertThrows(FileProcessException.class,
                () -> FileUtil.readFile(missing.toString()));
        assertTrue(e.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("传入文件夹路径而不是文件路径时报错")
    void testReadDirectory(@TempDir Path tempDir) {
        FileProcessException e = assertThrows(FileProcessException.class,
                () -> FileUtil.readFile(tempDir.toString()));
        assertTrue(e.getMessage().contains("不是一个文件"));
    }

    @Test
    @DisplayName("路径为空字符串时报错")
    void testEmptyPath() {
        assertThrows(FileProcessException.class, () -> FileUtil.readFile(""));
        assertThrows(FileProcessException.class, () -> FileUtil.writeFile("", "x"));
    }

    @Test
    @DisplayName("读取UTF-8中文不乱码")
    void testReadUtf8(@TempDir Path tempDir) throws IOException, FileProcessException {
        Path file = tempDir.resolve("utf8.txt");
        Files.write(file, "中文测试".getBytes(StandardCharsets.UTF_8));
        assertEquals("中文测试", FileUtil.readFile(file.toString()));
    }
}
