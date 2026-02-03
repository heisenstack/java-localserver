package src;

import java.util.List;
import java.util.Map;

public class ConfigLoader {
    public static Config load(String configPath) throws Exception {
        String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(configPath)));
        // System.out.println("config: " + content);
        JsonParser parser = new JsonParser(content);
        Map<String, Object> jsonMap = parser.parseObject();
        // System.out.println("Parsed config: " + jsonMap);
        Config config = new Config();
        config.setHost(JsonParser.getString(jsonMap, "host", "localhost"));
        List<Object> portsArray = JsonParser.getArray(jsonMap, "ports");
        if (portsArray.isEmpty()) {
            config.addPort(8080);
        } else {
            for (Object port : portsArray) {
                config.addPort(((Number) port).intValue());
            }
        }
        List<Object> routesArray = JsonParser.getArray(jsonMap, "routes");
        for (Object routeObj : routesArray) {
            @SuppressWarnings("unchecked")
            Map<String, Object> routeMap = (Map<String, Object>) routeObj;
            Config.Route route = parseRoute(routeMap);
            config.addRoute(route);
        }

        return config;
    }

}