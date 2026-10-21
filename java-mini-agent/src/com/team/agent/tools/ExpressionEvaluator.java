package com.team.agent.tools;

/**
 * 递归下降实现的四则运算求值器，支持 + - * / % ^ 与括号。
 * 不使用脚本引擎，避免额外依赖与执行任意脚本的风险。
 */
public final class ExpressionEvaluator {

    private final String source;
    private int pos;

    private ExpressionEvaluator(String source) {
        this.source = source;
    }

    public static double evaluate(String expression) {
        ExpressionEvaluator evaluator = new ExpressionEvaluator(expression);
        double value = evaluator.parseExpression();
        evaluator.skipWhitespace();
        if (evaluator.pos < evaluator.source.length()) {
            throw new IllegalArgumentException("存在无法识别的字符：" + evaluator.source.charAt(evaluator.pos));
        }
        return value;
    }

    /** 加减运算，优先级最低。 */
    private double parseExpression() {
        double value = parseTerm();
        while (true) {
            skipWhitespace();
            char operator = peek();
            if (operator == '+') {
                pos++;
                value += parseTerm();
            } else if (operator == '-') {
                pos++;
                value -= parseTerm();
            } else {
                return value;
            }
        }
    }

    /** 乘除与取模。 */
    private double parseTerm() {
        double value = parsePower();
        while (true) {
            skipWhitespace();
            char operator = peek();
            if (operator == '*' || operator == '/' || operator == '%') {
                pos++;
                double right = parsePower();
                if (operator == '*') {
                    value *= right;
                } else if (operator == '/') {
                    value /= right;
                } else {
                    value %= right;
                }
            } else {
                return value;
            }
        }
    }

    /** 幂运算，右结合。 */
    private double parsePower() {
        double base = parseFactor();
        skipWhitespace();
        if (peek() == '^') {
            pos++;
            return Math.pow(base, parsePower());
        }
        return base;
    }

    /** 数字、括号与一元正负号。 */
    private double parseFactor() {
        skipWhitespace();
        char c = peek();
        if (c == '+') {
            pos++;
            return parseFactor();
        }
        if (c == '-') {
            pos++;
            return -parseFactor();
        }
        if (c == '(') {
            pos++;
            double value = parseExpression();
            skipWhitespace();
            if (peek() != ')') {
                throw new IllegalArgumentException("括号不匹配，位置 " + pos);
            }
            pos++;
            return value;
        }
        int start = pos;
        while (isNumberPart(peek())) {
            pos++;
        }
        if (pos == start) {
            throw new IllegalArgumentException("缺少运算数，位置 " + pos);
        }
        return Double.parseDouble(source.substring(start, pos));
    }

    private static boolean isNumberPart(char c) {
        return (c >= '0' && c <= '9') || c == '.';
    }

    private char peek() {
        return pos < source.length() ? source.charAt(pos) : '\0';
    }

    private void skipWhitespace() {
        while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) {
            pos++;
        }
    }
}
