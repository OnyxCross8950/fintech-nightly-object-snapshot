package dev.infrai.fintech;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private Json() {}

    static String write(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return '"' + escape(text) + '"';
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            List<String> fields = new ArrayList<>();
            map.forEach((key, item) -> fields.add(write(key.toString()) + ":" + write(item)));
            return "{" + String.join(",", fields) + "}";
        }
        if (value instanceof Iterable<?> items) {
            List<String> encoded = new ArrayList<>();
            items.forEach(item -> encoded.add(write(item)));
            return "[" + String.join(",", encoded) + "]";
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
    }

    static Object read(String text) {
        Parser parser = new Parser(text);
        Object value = parser.value();
        parser.space();
        if (parser.index != text.length()) throw new IllegalArgumentException("Trailing JSON content");
        return value;
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static final class Parser {
        private final String text;
        private int index;
        private Parser(String text) { this.text = text; }

        private void space() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++;
        }

        private Object value() {
            space();
            if (index >= text.length()) throw new IllegalArgumentException("Expected JSON value");
            char c = text.charAt(index);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (text.startsWith("true", index)) { index += 4; return true; }
            if (text.startsWith("false", index)) { index += 5; return false; }
            if (text.startsWith("null", index)) { index += 4; return null; }
            return number();
        }

        private Map<String, Object> object() {
            Map<String, Object> result = new LinkedHashMap<>();
            index++;
            space();
            if (take('}')) return result;
            do {
                space();
                String key = string();
                space();
                require(':');
                result.put(key, value());
                space();
            } while (take(','));
            require('}');
            return result;
        }

        private List<Object> array() {
            List<Object> result = new ArrayList<>();
            index++;
            space();
            if (take(']')) return result;
            do { result.add(value()); space(); } while (take(','));
            require(']');
            return result;
        }

        private String string() {
            require('"');
            StringBuilder result = new StringBuilder();
            while (index < text.length()) {
                char c = text.charAt(index++);
                if (c == '"') return result.toString();
                if (c == '\\') {
                    char escaped = text.charAt(index++);
                    if (escaped == 'u') {
                        result.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                        index += 4;
                    } else {
                        result.append(switch (escaped) {
                            case '"', '\\', '/' -> escaped;
                            case 'b' -> '\b'; case 'f' -> '\f'; case 'n' -> '\n';
                            case 'r' -> '\r'; case 't' -> '\t';
                            default -> throw new IllegalArgumentException("Invalid JSON escape");
                        });
                    }
                } else result.append(c);
            }
            throw new IllegalArgumentException("Unclosed JSON string");
        }

        private Number number() {
            int start = index;
            while (index < text.length() && "-+0123456789.eE".indexOf(text.charAt(index)) >= 0) index++;
            String token = text.substring(start, index);
            return token.contains(".") || token.contains("e") || token.contains("E")
                    ? Double.parseDouble(token) : Long.parseLong(token);
        }

        private boolean take(char expected) {
            if (index < text.length() && text.charAt(index) == expected) { index++; return true; }
            return false;
        }

        private void require(char expected) {
            if (!take(expected)) throw new IllegalArgumentException("Expected '" + expected + "'");
        }
    }
}

