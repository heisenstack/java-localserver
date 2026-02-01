package src;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonParser {
    private String json;
    private int index;

    public JsonParser(String json) {
        this.json = json;
        this.index = 0;
    }

    public Map<String, Object> parseObject(){
        Map<String, Object> result = new LinkedHashMap<>();
        skipWhitespace();

        if (peek() != '{') {
            throw new RuntimeException("Expected '{' at position " + index);
        }
        next();
        skipWhitespace();
        while (peek() != '}') {
            skipWhitespace();

            String key = parseString();

            skipWhitespace();
            if (next() != ':') {
                throw new RuntimeException("Expected ':' after key at position " + index);
            }

            skipWhitespace();

            Object value = parseValue();
            result.put(key, value);

            skipWhitespace();
            if (peek() == ',') {
                next();
            }
            skipWhitespace();
        }
        return result;
    }

    private char peek() {
        if (index >= json.length()) {
            throw new RuntimeException("Unexpected end of JSON");
        }
        return json.charAt(index);
    }

    private char next() {
        if (index >= json.length()) {
            throw new RuntimeException("Unexpected end of JSON");
        }
        return json.charAt(index++);
    }

    private void skipWhitespace() {
        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
            index++;
        }
    }

    private Object parseValue() {
        skipWhitespace();
        char c = peek();

        if (c == '"') {
            return parseString();
        } else if (c == '{') {
            return parseObject();
        } else if (c == '[') {
            return parseArray();
        } else if (c == 't' || c == 'f') {
            return parseBoolean();
        } else if (c == 'n') {
            return parseNull();
        } else if (c == '-' || Character.isDigit(c)) {
            return parseNumber();
        } else {
            throw new RuntimeException("Unexpected character '" + c + "' at position " + index);
        }
    }
    private String parseString() {
        skipWhitespace();
        if (next() != '"') {
            throw new RuntimeException("Expected '\"' at position " + index);
        }
        
        StringBuilder sb = new StringBuilder();
        while (peek() != '"') {
            char c = next();
            if (c == '\\') {
                char escaped = next();
                switch (escaped) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '\\': sb.append('\\'); break;
                    case '"': sb.append('"'); break;
                    default: sb.append(escaped);
                }
            } else {
                sb.append(c);
            }
        }
        next(); 
        return sb.toString();
    }
    private List<Object> parseArray() {
        List<Object> result = new ArrayList<>();
        
        skipWhitespace();
        if (next() != '[') {
            throw new RuntimeException("Expected '[' at position " + index);
        }
        
        skipWhitespace();
        while (peek() != ']') {
            skipWhitespace();
            result.add(parseValue());
            skipWhitespace();
            
            if (peek() == ',') {
                next();
            }
            skipWhitespace();
        }
        
        next(); 
        return result;
    }
        private Number parseNumber() {
        skipWhitespace();
        StringBuilder sb = new StringBuilder();
        
        if (peek() == '-') {
            sb.append(next());
        }
        
        while (index < json.length() && (Character.isDigit(peek()) || peek() == '.')) {
            sb.append(next());
        }
        
        String numStr = sb.toString();
        if (numStr.contains(".")) {
            return Double.parseDouble(numStr);
        } else {
            return Integer.parseInt(numStr);
        }
    }
        private Boolean parseBoolean() {
        skipWhitespace();
        if (json.startsWith("true", index)) {
            index += 4;
            return true;
        } else if (json.startsWith("false", index)) {
            index += 5;
            return false;
        } else {
            throw new RuntimeException("Expected boolean at position " + index);
        }
    }
        private Object parseNull() {
        skipWhitespace();
        if (json.startsWith("null", index)) {
            index += 4;
            return null;
        } else {
            throw new RuntimeException("Expected null at position " + index);
        }
    }
}
