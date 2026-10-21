package com.team.agent.tools;

import com.team.agent.util.Json;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 计算工具，让智能体具备稳定的数值计算能力，而不是依赖模型心算。
 */
public class MathTools {

    public List<Tool> tools() {
        return Arrays.asList(new Calculate());
    }

    public static class Calculate implements Tool {

        @Override
        public String name() {
            return "calculate";
        }

        @Override
        public String description() {
            return "计算数学表达式，支持 + - * / % ^ 与括号，例如 (12 + 8) * 3 / 2。";
        }

        @Override
        public Map<String, Object> parameters() {
            return Schema.create()
                    .property("expression", "string", "待计算的数学表达式")
                    .required("expression")
                    .build();
        }

        @Override
        public String execute(Map<String, Object> arguments) {
            String expression = Json.text(arguments, "expression");
            if (expression == null || expression.trim().isEmpty()) {
                return "缺少必要参数：expression";
            }
            try {
                double value = ExpressionEvaluator.evaluate(expression);
                if (value == (long) value) {
                    return expression + " = " + (long) value;
                }
                return expression + " = " + value;
            } catch (RuntimeException e) {
                return "表达式无法计算：" + expression + "（" + e.getMessage() + "）";
            }
        }
    }
}
