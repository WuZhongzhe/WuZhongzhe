package com.papercheck;

import java.util.Locale;

import com.papercheck.exception.ArgumentException;
import com.papercheck.exception.FileProcessException;
import com.papercheck.exception.PaperCheckException;

/**
 * 程序入口。
 * 用法：java -jar main.jar [原文文件] [抄袭版论文的文件] [答案文件]
 */
public class Main {

    /** 命令行参数个数：原文、抄袭版、答案文件 */
    private static final int ARG_COUNT = 3;

    public static void main(String[] args) {
        try {
            checkArgs(args);
            String originalText = FileUtil.readFile(args[0]);
            String copyText = FileUtil.readFile(args[1]);
            double rate = SimilarityCalculator.calculate(originalText, copyText);
            FileUtil.writeFile(args[2], formatRate(rate));
            System.out.println("查重完成，重复率：" + formatRate(rate) + "，结果已写入 " + args[2]);
        } catch (PaperCheckException e) {
            // 可预期的错误，打印提示后以退出码 1 结束，不让异常栈打出来
            System.err.println("错误：" + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("程序发生未知错误：" + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * 校验命令行参数，参数个数不对时抛出 {@link ArgumentException}。
     * 另外文件读写前也会检查路径，路径问题会抛 {@link FileProcessException}。
     */
    static void checkArgs(String[] args) throws ArgumentException {
        if (args == null || args.length != ARG_COUNT) {
            throw new ArgumentException("参数个数错误！正确用法：java -jar main.jar [原文文件] [抄袭版文件] [答案文件]");
        }
        for (String arg : args) {
            if (arg == null || arg.trim().isEmpty()) {
                throw new ArgumentException("参数不能为空字符串！正确用法：java -jar main.jar [原文文件] [抄袭版文件] [答案文件]");
            }
        }
    }

    /** 把重复率格式化成保留两位小数的字符串 */
    static String formatRate(double rate) {
        double fixed = rate;
        if (fixed < 0.0) {
            fixed = 0.0;
        } else if (fixed > 1.0) {
            fixed = 1.0;
        }
        return String.format(Locale.ROOT, "%.2f", fixed);
    }
}
