package com.team.agent.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量 JSON 解析与序列化工具。
 * 项目不引入第三方依赖，与大模型、MCP 客户端交互时需要的 JSON 处理全部由该类完成。
 */
public final class Json {

    private Json() {
    }

    /** JSON 中的 null 值占位对象，用于区分「键不存在」与「值为 null」。 */
    public static final Object NULL = new Object() {
        @Override
        public String toString() {
            return "null";
        }
    };

    public static Map<String, Object> obj() {
        return new LinkedHashMap<>();
    }

    public static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (!parser.end()) {
            throw new JsonException("JSON 解析失败：位置 " + parser.pos + " 之后存在多余内容");
        }
        return value;
    }

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(out, value);
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        throw new JsonException("期望 JSON 对象，实际为：" + value);
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object value) {
        if (value instanceof List) {
            return (List<Object>) value;
        }
        throw new JsonException("期望 JSON 数组，实际为：" + value);
    }

    public static String text(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null || value == Json.NULL ? null : String.valueOf(value);
    }

    public static String text(Map<String, Object> map, String key, String defaultValue) {
        String value = text(map, key);
        return value == null ? defaultValue : value;
    }

    private static void writeValue(StringBuilder out, Object value) {
        if (value == null || value == NULL) {
            out.append("null");
        } else if (value instanceof String) {
            writeString(out, (String) value);
        } else if (value instanceof Boolean) {
            out.append(value.toString());
        } else if (value instanceof Number) {
            out.append(value.toString());
        } else if (value instanceof Map) {
            writeObject(out, (Map<?, ?>) value);
        } else if (value instanceof List) {
            writeArray(out, (List<?>) value);
        } else {
            writeString(out, String.valueOf(value));
        }
    }

    private static void writeObject(StringBuilder out, Map<?, ?> map) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeString(out, String.valueOf(entry.getKey()));
            out.append(':');
            writeValue(out, entry.getValue());
        }
        out.append('}');
    }

    private static void writeArray(StringBuilder out, List<?> list) {
        out.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            writeValue(out, list.get(i));
        }
        out.append(']');
    }

    private static void writeString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }

    private static final class Parser {

        private final String text;
        private int pos;

        Parser(String text) {
            this.text = text;
        }

        boolean end() {
            return pos >= text.length();
        }

        void skipWhitespace() {
            while (pos < text.length()) {
                char c = text.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        Object readValue() {
            skipWhitespace();
            if (end()) {
                throw new JsonException("JSON 解析失败：内容意外结束");
            }
            char c = text.charAt(pos);
            switch (c) {
                case '{':
                    return readObject();
                case '[':
                    return readArray();
                case '"':
                    return readString();
                case 't':
                    expect("true");
                    return Boolean.TRUE;
                case 'f':
                    expect("false");
                    return Boolean.FALSE;
                case 'n':
                    expect("null");
                    return NULL;
                default:
                    return readNumber();
            }
        }

        private Map<String, Object> readObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++;
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                if (peek() != ':') {
                    throw new JsonException("JSON 解析失败：缺少冒号 @" + pos);
                }
                pos++;
                map.put(key, readValue());
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    pos++;
                    continue;
                }
                if (c == '}') {
                    pos++;
                    return map;
                }
                throw new JsonException("JSON 解析失败：对象未正确结束 @" + pos);
            }
        }

        private List<Object> readArray() {
            List<Object> list = new ArrayList<>();
            pos++;
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(readValue());
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    pos++;
                    continue;
                }
                if (c == ']') {
                    pos++;
                    return list;
                }
                throw new JsonException("JSON 解析失败：数组未正确结束 @" + pos);
            }
        }

        private String readString() {
            if (peek() != '"') {
                throw new JsonException("JSON 解析失败：期望字符串 @" + pos);
            }
            pos++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (end()) {
                    throw new JsonException("JSON 解析失败：字符串未闭合");
                }
                char c = text.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char esc = text.charAt(pos++);
                switch (esc) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                        pos += 4;
                        break;
                    default:
                        throw new JsonException("JSON 解析失败：非法转义 \\" + esc);
                }
            }
        }

        private Object readNumber() {
            int start = pos;
            while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) {
                pos++;
            }
            String raw = text.substring(start, pos);
            if (raw.isEmpty()) {
                throw new JsonException("JSON 解析失败：非法字符 '" + text.charAt(pos) + "' @" + pos);
            }
            if (raw.indexOf('.') < 0 && raw.indexOf('e') < 0 && raw.indexOf('E') < 0) {
                return Long.parseLong(raw);
            }
            return Double.parseDouble(raw);
        }

        private char peek() {
            if (end()) {
                throw new JsonException("JSON 解析失败：内容意外结束");
            }
            return text.charAt(pos);
        }

        private void expect(String literal) {
            if (!text.startsWith(literal, pos)) {
                throw new JsonException("JSON 解析失败：无法识别的字面量 @" + pos);
            }
            pos += literal.length();
        }
    }

    public static class JsonException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public JsonException(String message) {
            super(message);
        }
    }
}
