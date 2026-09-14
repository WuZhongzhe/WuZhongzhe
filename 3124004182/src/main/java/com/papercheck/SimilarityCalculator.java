package com.papercheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 相似度计算类，核心算法是"词频向量 + 余弦相似度"。
 *
 * 做法：把两篇文章都转成一个 Map&lt;token, 出现次数&gt;，
 * 相当于两个高维向量，向量夹角的余弦值就是重复率。
 * 两篇文章完全一样时余弦值为 1，完全没有共同 token 时为 0。
 */
public final class SimilarityCalculator {

    private SimilarityCalculator() {
        // 工具类不允许实例化
    }

    /**
     * 计算两段文本的重复率。
     *
     * @param original 原文内容
     * @param copy     抄袭版内容
     * @return 0~1 之间的重复率
     */
    public static double calculate(String original, String copy) {
        List<String> originalTokens = TextProcessor.segment(original);
        List<String> copyTokens = TextProcessor.segment(copy);

        // 两边都是空文件，认为完全一样；只有一边为空，认为完全不一样
        if (originalTokens.isEmpty() && copyTokens.isEmpty()) {
            return 1.0;
        }
        if (originalTokens.isEmpty() || copyTokens.isEmpty()) {
            return 0.0;
        }

        Map<String, Integer> originalFreq = countFrequency(originalTokens);
        Map<String, Integer> copyFreq = countFrequency(copyTokens);

        double dotProduct = 0.0;   // 向量点积
        double originalNorm = 0.0; // 原文向量模长的平方
        double copyNorm = 0.0;     // 抄袭版向量模长的平方

        // 只遍历一遍原文的词频表，顺便把点积算出来
        for (Map.Entry<String, Integer> entry : originalFreq.entrySet()) {
            int count = entry.getValue();
            originalNorm += (double) count * count;
            Integer otherCount = copyFreq.get(entry.getKey());
            if (otherCount != null) {
                dotProduct += (double) count * otherCount;
            }
        }
        for (int count : copyFreq.values()) {
            copyNorm += (double) count * count;
        }

        if (originalNorm == 0.0 || copyNorm == 0.0) {
            return 0.0;
        }
        double rate = dotProduct / (Math.sqrt(originalNorm) * Math.sqrt(copyNorm));
        // 修正浮点数误差，保证结果落在 [0,1]
        if (rate < 0.0) {
            return 0.0;
        }
        return rate > 1.0 ? 1.0 : rate;
    }

    /** 统计 token 出现次数 */
    private static Map<String, Integer> countFrequency(List<String> tokens) {
        // 给初始容量，减少 HashMap 扩容次数
        Map<String, Integer> freq = new HashMap<>(tokens.size() * 2);
        for (String token : tokens) {
            Integer old = freq.get(token);
            freq.put(token, old == null ? 1 : old + 1);
        }
        return freq;
    }
}
