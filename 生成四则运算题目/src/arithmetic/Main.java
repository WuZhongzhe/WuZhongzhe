package arithmetic;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
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
                n = Integer.parseInt(args[i + 1]);
                i++;
            } else if ("-r".equals(args[i]) && i + 1 < args.length) {
                r = Integer.parseInt(args[i + 1]);
                i++;
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
        String exercises = "";
        String answers = "";
        for (int i = 0; i < n; i++) {
            Expression.Node node = null;
            // 不合格的表达式会返回 null，重试直到拿到一个能用的
            while (node == null) {
                int opCount = 1 + rnd.nextInt(3);
                node = Expression.generate(rnd, opCount, r);
            }
            exercises = exercises + (i + 1) + ". " + Expression.toString(node) + " =" + "\r\n";
            answers = answers + (i + 1) + ". " + node.value + "\r\n";
        }
        write("Exercises.txt", exercises);
        write("Answers.txt", answers);
        System.out.println("生成了 " + n + " 道题目，已写入 Exercises.txt 和 Answers.txt");
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

    private static void printHelp() {
        System.out.println("用法：Myapp.exe -n 题目个数 -r 数值范围");
        System.out.println("  例：Myapp.exe -n 10 -r 10  生成 10 道 10 以内的四则运算题");
        System.out.println("  -r 必须给定，-n 不写的话默认生成 10 道");
    }
}
