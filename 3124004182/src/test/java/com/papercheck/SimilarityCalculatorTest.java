package com.papercheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SimilarityCalculator 的单元测试，验证各种情况下的重复率是否符合直觉。
 */
class SimilarityCalculatorTest {

    private static final double DELTA = 0.01;

    @Test
    @DisplayName("完全相同的文章重复率为1")
    void testIdentical() {
        String text = "今天是星期天，天气晴，今天晚上我要去看电影。";
        assertEquals(1.0, SimilarityCalculator.calculate(text, text), DELTA);
    }

    @Test
    @DisplayName("完全不同的文章重复率为0")
    void testCompletelyDifferent() {
        double rate = SimilarityCalculator.calculate("我今天要去打篮球", "西红柿炒鸡蛋很好吃");
        assertEquals(0.0, rate, DELTA);
    }

    @Test
    @DisplayName("作业示例：同义改写后重复率仍较高")
    void testSampleOfRequirement() {
        String original = "今天是星期天，天气晴，今天晚上我要去看电影。";
        String copy = "今天是周天，天气晴朗，我晚上要去看电影。";
        double rate = SimilarityCalculator.calculate(original, copy);
        assertTrue(rate > 0.5, "改写后的重复率应该大于0.5，实际为" + rate);
        assertTrue(rate < 1.0);
    }

    @Test
    @DisplayName("两篇都是空文件时重复率为1")
    void testBothEmpty() {
        assertEquals(1.0, SimilarityCalculator.calculate("", ""), DELTA);
        assertEquals(1.0, SimilarityCalculator.calculate(null, null), DELTA);
    }

    @Test
    @DisplayName("只有一篇是空文件时重复率为0")
    void testOneSideEmpty() {
        assertEquals(0.0, SimilarityCalculator.calculate("有内容", ""), DELTA);
        assertEquals(0.0, SimilarityCalculator.calculate("", "有内容"), DELTA);
    }

    @Test
    @DisplayName("增删改少量内容后重复率仍然很高")
    void testAddAndDelete() {
        String original = "软件工程是一门研究用工程化方法构建和维护有效的、实用的和高质量的软件的学科";
        String copy = "软件工程是一门研究用工程化的方法构建并且维护有效的、实用的和高质量软件的学科";
        double rate = SimilarityCalculator.calculate(original, copy);
        assertTrue(rate > 0.8, "少量增删改重复率应大于0.8，实际为" + rate);
    }

    @Test
    @DisplayName("计算结果与两篇文章的先后顺序无关")
    void testSymmetric() {
        String a = "北京上海的天气都不错";
        String b = "北京上海天气不错";
        assertEquals(SimilarityCalculator.calculate(a, b),
                SimilarityCalculator.calculate(b, a), DELTA);
    }

    @Test
    @DisplayName("重复率不会超过1也不会小于0")
    void testRateInRange() {
        double rate = SimilarityCalculator.calculate("哈哈哈哈", "哈哈哈");
        assertTrue(rate >= 0.0 && rate <= 1.0);
    }
}
