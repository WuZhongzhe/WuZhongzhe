package com.papercheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 文本预处理与分词工具类。
 *
 * 思路：中文没有空格，用第三方分词库（HanLP、jieba）虽然准，
 * 但是要额外引入好几 MB 的依赖，打出来的 jar 包又大又慢，对这次作业来说没必要。
 * 所以我用的是"字符 2-gram"：把一个中文串按相邻两个字切，
 * 例如"天气晴朗"切成 ["天气","气晴","晴朗"]。
 * 增删改操作只会影响局部的几个 gram，整体重合度依然很高，正好符合查重场景。
 */
public final class TextProcessor {

    /** 只保留中文、英文字母和数字，其余（标点、空白、换行）全部当成分隔符 */
    private static final Pattern NOT_WORD = Pattern.compile("[^\\u4e00-\\u9fa5a-zA-Z0-9]+");

    /** n-gram 的 n，取 2 */
    private static final int N_GRAM = 2;

    private TextProcessor() {
        // 工具类不允许实例化
    }

    /**
     * 把一段文本切成词元（token）列表。
     *
     * @param text 原始文本
     * @return token 列表，文本为空时返回空列表
     */
    public static List<String> segment(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return tokens;
        }
        // 统一转小写，英文的 Today 和 today 就当成同一个词
        String cleaned = NOT_WORD.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ");
        String[] words = cleaned.trim().split(" ");
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (isChinese(word.charAt(0))) {
                tokens.addAll(toNGram(word));
            } else {
                // 英文单词或数字整体作为一个 token，不切碎
                tokens.add(word);
            }
        }
        return tokens;
    }

    /** 判断一个字符是不是中文 */
    private static boolean isChinese(char c) {
        return c >= '\u4e00' && c <= '\u9fa5';
    }

    /** 把一个中文串切成 2-gram，长度不足 2 时就把单个字当成一个 token */
    private static List<String> toNGram(String word) {
        List<String> grams = new ArrayList<>(word.length());
        if (word.length() <= N_GRAM) {
            grams.add(word);
            return grams;
        }
        for (int i = 0; i <= word.length() - N_GRAM; i++) {
            grams.add(word.substring(i, i + N_GRAM));
        }
        return grams;
    }
}
