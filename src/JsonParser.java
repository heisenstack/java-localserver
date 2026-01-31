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
        return result;
    }

}
