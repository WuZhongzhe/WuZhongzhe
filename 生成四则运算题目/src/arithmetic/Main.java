package arithmetic;

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
        System.out.println("参数解析完成：题目个数 " + n + "，数值范围 " + r);
        System.out.println("生成题目的部分还没写，下一次提交补上");
    }

    private static void printHelp() {
        System.out.println("用法：Myapp.exe -n 题目个数 -r 数值范围");
        System.out.println("  例：Myapp.exe -n 10 -r 10  生成 10 道 10 以内的四则运算题");
        System.out.println("  -r 必须给定，-n 不写的话默认生成 10 道");
    }
}
