package com.papercheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TextProcessor 的单元测试，主要验证分词和清洗逻辑。
 */
class TextProcessorTest {

    @Test
    @DisplayName("中文句子切成2-gram")
    void testSegmentChinese() {
        List<String> tokens = TextProcessor.segment("天气晴朗");
        assertEquals(3, tokens.size());
        assertEquals("天气", tokens.get(0));
        assertEquals("气晴", tokens.get(1));
        assertEquals("晴朗", tokens.get(2));
    }

    @Test
    @DisplayName("标点符号和空白应被过滤掉")
    void testSegmentRemovePunctuation() {
        List<String> tokens = TextProcessor.segment("今天是星期天，天气晴！");
        // 逗号和感叹号会变成分隔符，整句被切成"今天是星期天"和"天气晴"两段
        assertTrue(tokens.contains("星期"));
        assertTrue(!tokens.contains("天，"), "分词结果中不应包含标点");
    }

    @Test
    @DisplayName("空字符串和null返回空列表")
    void testSegmentEmpty() {
        assertTrue(TextProcessor.segment("").isEmpty());
        assertTrue(TextProcessor.segment("   ").isEmpty());
        assertTrue(TextProcessor.segment(null).isEmpty());
    }

    @Test
    @DisplayName("单个汉字作为整体保留")
    void testSegmentSingleChineseChar() {
        List<String> tokens = TextProcessor.segment("我");
        assertEquals(1, tokens.size());
        assertEquals("我", tokens.get(0));
    }

    @Test
    @DisplayName("英文单词不切碎且大小写统一")
    void testSegmentEnglishAndCase() {
        List<String> tokens = TextProcessor.segment("Hello WORLD hello");
        assertEquals(3, tokens.size());
        // 全部被转成小写，三个词都被当成同一个 token
        assertEquals("hello", tokens.get(0));
        assertEquals("world", tokens.get(1));
        assertEquals("hello", tokens.get(2));
    }

    @Test
    @DisplayName("数字与中文混排时整体作为一个token")
    void testSegmentMixedNumberAndChinese() {
        List<String> tokens = TextProcessor.segment("2021年6月");
        // 首字符是数字，走"非中文整体保留"分支，所以整串是一个 token
        assertEquals(1, tokens.size());
        assertEquals("2021年6月", tokens.get(0));
    }

    @Test
    @DisplayName("纯数字串各自作为独立token")
    void testSegmentPureNumber() {
        List<String> tokens = TextProcessor.segment("2021年 6 18");
        assertTrue(tokens.contains("2021年"));
        assertTrue(tokens.contains("6"));
        assertTrue(tokens.contains("18"));
    }
}
