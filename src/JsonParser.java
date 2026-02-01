package src;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonParser {
    private String json;
    private int index;

    public JsonParser(String json) {
        this.json = json;
        this.index = 0;
    }

    public Map<String, Object> parseObject() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        skipWhitespace();

        if (peek() != '{') {
            throw new RuntimeException("Expected '{' at position " + index);
        }
        next();
        skipWhitespace();

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

}
