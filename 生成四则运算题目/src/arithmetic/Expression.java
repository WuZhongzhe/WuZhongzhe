package arithmetic;

import java.util.Random;

/**
 * 算术表达式，用二叉树存：叶子结点是数字，其它结点是运算符。
 */
public class Expression {

    /** 表达式的结点 */
    public static class Node {
        public Fraction value;   // 这棵子树的运算结果
        public char op;          // 叶子结点为 0
        public Node left;
        public Node right;

        public Node(Fraction value) {
            this.value = value;
            this.op = 0;
        }

        public Node(Fraction value, char op, Node left, Node right) {
            this.value = value;
            this.op = op;
            this.left = left;
            this.right = right;
        }
    }

    private static final char[] OPS = {'+', '-', '×', '÷'};

    /**
     * 随机生成一个含有 opCount 个运算符的表达式，数值范围由 range 控制。
     * 中途不满足要求（减法出负数、除以 0 等）就返回 null，让调用方重试。
     */
    public static Node generate(Random rnd, int opCount, int range) {
        if (opCount == 0) {
            return new Node(randomNumber(rnd, range));
        }
        char op = OPS[rnd.nextInt(OPS.length)];
        int leftCount = rnd.nextInt(opCount);
        int rightCount = opCount - 1 - leftCount;
        Node left = generate(rnd, leftCount, range);
        if (left == null) {
            return null;
        }
        Node right = generate(rnd, rightCount, range);
        if (right == null) {
            return null;
        }
        // 右子树的运算符和父结点相同的话括号是多余的，重来
        if (right.op == op) {
            return null;
        }
        Fraction value;
        if (op == '+') {
            value = left.value.add(right.value);
        } else if (op == '-') {
            if (left.value.compareTo(right.value) < 0) {
                return null;
            }
            value = left.value.sub(right.value);
        } else if (op == '×') {
            value = left.value.mul(right.value);
        } else {
            if (right.value.isZero()) {
                return null;
            }
            value = left.value.div(right.value);
        }
        // 结果太大就不像小学题了
        if (value.toDouble() > 10000 || value.getDen() > 10000) {
            return null;
        }
        return new Node(value, op, left, right);
    }

    /** 随机生成一个自然数或者真分数，都小于 range */
    private static Fraction randomNumber(Random rnd, int range) {
        if (range <= 1) {
            return new Fraction(0);
        }
        if (range <= 2 || rnd.nextInt(2) == 0) {
            return new Fraction(rnd.nextInt(range));
        }
        int den = 2 + rnd.nextInt(range - 2);
        int num = 1 + rnd.nextInt(den - 1);
        return new Fraction(num, den);
    }

    /** 转成题目字符串，数字和运算符之间用空格隔开 */
    public static String toString(Node n) {
        if (n.op == 0) {
            return n.value.toString();
        }
        int level = priority(n.op);
        return child(n.left, level, false) + " " + n.op + " " + child(n.right, level, true);
    }

    private static String child(Node c, int parentLevel, boolean isRight) {
        if (c.op == 0) {
            return c.value.toString();
        }
        int level = priority(c.op);
        if (level < parentLevel || (isRight && level == parentLevel)) {
            return "(" + toString(c) + ")";
        }
        return toString(c);
    }

    private static int priority(char op) {
        return (op == '×' || op == '÷') ? 2 : 1;
    }

    /**
     * 规范化字符串，用来判断两道题是不是重复的：
     * 对 + 和 × 的左右子树按字典序排好再拼接，这样 1+2 和 2+1、
     * 3+(2+1) 和 (1+2)+3 得到的串是一样的。
     */
    public static String canonical(Node n) {
        if (n.op == 0) {
            return n.value.toString();
        }
        String l = canonical(n.left);
        String r = canonical(n.right);
        if (n.op == '+' || n.op == '×') {
            if (l.compareTo(r) > 0) {
                String t = l;
                l = r;
                r = t;
            }
        }
        return n.op + "(" + l + "," + r + ")";
    }
}
