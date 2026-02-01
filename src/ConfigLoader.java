package src;

import java.util.Map;

public class ConfigLoader {
    public static Config load(String configPath) throws Exception {
        String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(configPath)));
        System.out.println("config: " + content);
        JsonParser parser = new JsonParser(content);
        Map<String, Object> jsonMap = parser.parseObject();
        System.out.println("Parsed config: " + jsonMap);
        Config config = new Config();
        return config;
    }

}