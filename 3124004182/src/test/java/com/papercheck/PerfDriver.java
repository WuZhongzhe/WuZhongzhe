package com.papercheck;

import com.papercheck.exception.FileProcessException;


public class PerfDriver {

    /** 把原文重复多少遍当成大文本 */
    private static final int REPEAT = 4000;

    /** 重复计算多少次 */
    private static final int LOOP = 150;

    private static final boolean WAIT_AFTER_RUN = false;

    public static void main(String[] args) {
        String path;
        if (args != null && args.length > 0 && !args[0].trim().isEmpty()) {
            path = args[0];
        } else {
            path = "sample/orig.txt";
            System.out.println("没有传入路径，自动使用项目自带的样例文件：" + path);
        }

        try {
            String base = FileUtil.readFile(path);

            // 造一份大原文
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < REPEAT; i++) {
                builder.append(base);
            }
            String bigOriginal = builder.toString();

            // 造一份"抄袭版"
            String bigCopy = bigOriginal.replace("星期天", "周天").replace("天气晴", "天气晴朗");

            System.out.println("原文长度：" + bigOriginal.length() + "，抄袭版长度：" + bigCopy.length());

            // 先跑一次预热，让 JIT 把代码编译好，避免把编译耗时算进来
            double warm = SimilarityCalculator.calculate(bigOriginal, bigCopy);
            System.out.println("预热结果：" + warm);

            // 正式计时
            long start = System.currentTimeMillis();
            double last = 0.0;
            for (int i = 0; i < LOOP; i++) {
                last = SimilarityCalculator.calculate(bigOriginal, bigCopy);
            }
            long cost = System.currentTimeMillis() - start;
            System.out.println("重复率：" + last + "，循环 " + LOOP + " 次总耗时：" + cost + " ms");

            if (WAIT_AFTER_RUN) {
                waitForScreenshot();
            } else {
                System.out.println("（算完直接退出，JProfiler 会按\"JVM 退出操作\"的设置处理本次数据）");
            }
        } catch (FileProcessException e) {
            System.out.println("读取原文失败：" + e.getMessage());
            System.out.println("两种改法，任选其一：");
            System.out.println("  1. Eclipse 里 Run -> Run Configurations -> Arguments");
            System.out.println("     -> Program arguments 填原文文件完整路径，例如 D:/tests/orig.txt");
            System.out.println("  2. 把原文文件放到项目的 sample/ 目录下并改名为 orig.txt");
            System.out.println("注意：Java 字符串里反斜杠是转义符，");
            System.out.println("      路径要写 D:/tests/orig.txt 或 D:\\\\tests\\\\orig.txt，不能只写一个反斜杠。");
        }
    }

    private static void waitForScreenshot() {
        System.out.println();
        System.out.println("==============================================");
        System.out.println(" 计算已结束，但程序故意不退出，JVM 保持运行");
        System.out.println(" 请现在切到 JProfiler 窗口做这几件事：");
        System.out.println("   1. 点工具栏的停止记录按钮（红色方块）");
        System.out.println("   2. 左侧点 CPU 视图 -> Hot Spots，按 Self time 排序，截图");
        System.out.println("   3. 左侧点 CPU 视图 -> Call Tree，展开截图");
        System.out.println("   4. 需要的话再截一张 实时内存 的图");
        System.out.println(" 截完图回到 Eclipse 控制台，点一下控制台区域，按回车键结束");
        System.out.println("==============================================");
        try {
            System.in.read();
        } catch (Exception e) {
            // 万一控制台不让输入，就一直等着，直接关 JProfiler / 停止程序即可
            try {
                Thread.sleep(600000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
