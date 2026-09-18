package arithmetic;

/**
 * 分数类，内部统一用 num / den 表示，den 恒为正数，构造时自动约分。
 */
public class Fraction {

    private long num;
    private long den;

    public Fraction(long num) {
        this(num, 1);
    }

    public Fraction(long num, long den) {
        if (den == 0) {
            throw new ArithmeticException("den can not be zero");
        }
        if (den < 0) {
            num = -num;
            den = -den;
        }
        long g = gcd(Math.abs(num), Math.abs(den));
        this.num = num / g;
        this.den = den / g;
    }

    public long getNum() {
        return num;
    }

    public long getDen() {
        return den;
    }

    public boolean isZero() {
        return num == 0;
    }

    public Fraction add(Fraction o) {
        return new Fraction(num * o.den + o.num * den, den * o.den);
    }

    public Fraction sub(Fraction o) {
        return new Fraction(num * o.den - o.num * den, den * o.den);
    }

    public Fraction mul(Fraction o) {
        return new Fraction(num * o.num, den * o.den);
    }

    public Fraction div(Fraction o) {
        if (o.num == 0) {
            throw new ArithmeticException("divide by zero");
        }
        return new Fraction(num * o.den, den * o.num);
    }

    // 题目里的数都不会太大，直接用 double 比大小
    public int compareTo(Fraction o) {
        double d = toDouble() - o.toDouble();
        if (d > 1e-9) {
            return 1;
        }
        if (d < -1e-9) {
            return -1;
        }
        return 0;
    }

    public double toDouble() {
        return (double) num / den;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Fraction)) {
            return false;
        }
        Fraction f = (Fraction) o;
        return num == f.num && den == f.den;
    }

    public int hashCode() {
        return (int) (num * 31 + den);
    }

    /**
     * 整数输出成 3，真分数输出成 3/5，假分数输出成 2'3/8
     */
    public String toString() {
        if (den == 1) {
            return String.valueOf(num);
        }
        if (num < den) {
            return num + "/" + den;
        }
        long integer = num / den;
        long rest = num % den;
        return integer + "'" + rest + "/" + den;
    }

    /** 解析 3/5、2'3/8、12 这三种写法 */
    public static Fraction parse(String s) {
        String t = s.trim();
        if (t.indexOf('\'') >= 0) {
            String[] p = t.split("'");
            Fraction f = parse(p[1]);
            long integer = Long.parseLong(p[0]);
            return new Fraction(integer * f.den + f.num, f.den);
        }
        if (t.indexOf('/') >= 0) {
            String[] p = t.split("/");
            return new Fraction(Long.parseLong(p[0]), Long.parseLong(p[1]));
        }
        return new Fraction(Long.parseLong(t));
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a == 0 ? 1 : a;
    }
}
