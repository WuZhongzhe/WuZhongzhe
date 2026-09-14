package com.papercheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.papercheck.exception.ArgumentException;

class MainTest {

    @Test
    @DisplayName("参数个数正确时不抛异常")
    void testCheckArgsOk() throws ArgumentException {
        Main.checkArgs(new String[]{"a.txt", "b.txt", "c.txt"});
    }

    @Test
    @DisplayName("参数个数不对时抛出 ArgumentException")
    void testCheckArgsWrongCount() {
        assertThrows(ArgumentException.class, () -> Main.checkArgs(new String[]{"a.txt"}));
        assertThrows(ArgumentException.class, () -> Main.checkArgs(new String[]{"a.txt", "b.txt"}));
        assertThrows(ArgumentException.class, () -> Main.checkArgs(null));
    }

    @Test
    @DisplayName("参数里有空字符串时抛出 ArgumentException")
    void testCheckArgsBlank() {
        assertThrows(ArgumentException.class, () -> Main.checkArgs(new String[]{"a.txt", "", "c.txt"}));
    }

    @Test
    @DisplayName("重复率格式化成两位小数")
    void testFormatRate() {
        assertEquals("1.00", Main.formatRate(1.0));
        assertEquals("0.00", Main.formatRate(0.0));
        assertEquals("0.86", Main.formatRate(0.856));
        // 越界的值会被夹到 [0,1]
        assertEquals("1.00", Main.formatRate(1.5));
        assertEquals("0.00", Main.formatRate(-0.2));
    }

    @Test
    @DisplayName("端到端：传入三个文件路径能写出答案")
    void testMainEndToEnd(@TempDir Path tempDir) throws Exception {
        Path original = tempDir.resolve("orig.txt");
        Path copy = tempDir.resolve("orig_add.txt");
        Path answer = tempDir.resolve("ans.txt");
        Files.write(original, "今天是星期天，天气晴，今天晚上我要去看电影。".getBytes(StandardCharsets.UTF_8));
        Files.write(copy, "今天是周天，天气晴朗，我晚上要去看电影。".getBytes(StandardCharsets.UTF_8));

        Main.main(new String[]{original.toString(), copy.toString(), answer.toString()});

        assertTrue(Files.exists(answer));
        String result = new String(Files.readAllBytes(answer), StandardCharsets.UTF_8);
        // 答案应该是形如 0.85 的两位小数
        assertTrue(result.matches("0\\.\\d{2}|1\\.00"), "答案格式不对：" + result);
    }
}
