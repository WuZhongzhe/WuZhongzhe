package arithmetic;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Random;

/**
 * 程序入口。
 * 生成题目：Myapp.exe -n 10 -r 10
 */
public class Main {

    public static void main(String[] args) {
        int n = -1;
        int r = -1;
        for (int i = 0; i < args.length; i++) {
            if ("-n".equals(args[i]) && i + 1 < args.length) {
                n = toInt(args[++i]);
            } else if ("-r".equals(args[i]) && i + 1 < args.length) {
                r = toInt(args[++i]);
            } else {
                System.out.println("无法识别的参数：" + args[i]);
                printHelp();
                return;
            }
        }
        if (r < 0) {
            System.out.println("参数错误：-r 必须给定");
            printHelp();
            return;
        }
        if (n < 0) {
            n = 10;
        }
        generate(n, r);
    }

    private static void generate(int n, int r) {
        Random rnd = new Random();
        HashSet<String> appeared = new HashSet<String>();
        StringBuilder exercises = new StringBuilder();
        StringBuilder answers = new StringBuilder();
        String line = System.lineSeparator();
        int count = 0;
        long times = 0;
        long maxTimes = 500L * n + 10000;
        while (count < n && times < maxTimes) {
            times++;
            int opCount = 1 + rnd.nextInt(3);
            Expression.Node node = Expression.generate(rnd, opCount, r);
            if (node == null) {
                continue;
            }
            String key = Expression.canonical(node);
            if (appeared.contains(key)) {
                continue;
            }
            appeared.add(key);
            count++;
            exercises.append(count).append(". ").append(Expression.toString(node)).append(" =").append(line);
            answers.append(count).append(". ").append(node.value).append(line);
        }
        write("Exercises.txt", exercises.toString());
        write("Answers.txt", answers.toString());
        System.out.println("生成了 " + count + " 道题目，已写入 Exercises.txt 和 Answers.txt");
    }

    private static void write(String fileName, String content) {
        try {
            OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8");
            w.write(content);
            w.close();
        } catch (Exception e) {
            System.out.println("写文件 " + fileName + " 失败：" + e.getMessage());
        }
    }

    /** 参数不是数字的话返回 -1，不让它直接抛异常 */
    private static int toInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            System.out.println("参数 " + s + " 不是数字");
            return -1;
        }
    }

    private static void printHelp() {
        System.out.println("用法：Myapp.exe -n 题目个数 -r 数值范围");
        System.out.println("  例：Myapp.exe -n 10 -r 10  生成 10 道 10 以内的四则运算题");
        System.out.println("  -r 必须给定，-n 不写的话默认生成 10 道");
    }
}
