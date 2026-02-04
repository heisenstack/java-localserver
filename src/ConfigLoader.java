package src;

import java.util.List;
import java.util.Map;

public class ConfigLoader {
    public static Config load(String configPath) throws Exception {
        String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(configPath)));
        System.out.println("[DEBUG] Config file loaded");
        JsonParser parser = new JsonParser(content);
        Map<String, Object> jsonMap = parser.parseObject();
        System.out.println("[DEBUG] Parsed config: " + jsonMap);
        
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
        System.out.println("[DEBUG] Routes array size: " + routesArray.size());
        
        for (Object routeObj : routesArray) {
            @SuppressWarnings("unchecked")
            Map<String, Object> routeMap = (Map<String, Object>) routeObj;
            Config.Route route = parseRoute(routeMap);
            config.addRoute(route);
            System.out.println("[DEBUG] Added route: " + route.getPath() + " with methods: " + route.getAllowedMethods());
        }

        System.out.println("[DEBUG] Total routes loaded: " + config.getRoutes().size());
        return config;
    }

    private static Config.Route parseRoute(Map<String, Object> json) {
        Config.Route route = new Config.Route();
        route.setPath(JsonParser.getString(json, "path", "/"));
        
        List<Object> methods = JsonParser.getArray(json, "methods");
        System.out.println("[DEBUG] Parsing route with methods: " + methods);
        
        for (Object method : methods) {
            route.addAllowedMethod(method.toString());
        }

        String defaultFile = JsonParser.getString(json, "index", null);
        if (defaultFile != null) {
            route.setDefaultFile(defaultFile);
        }
        return route;
    }
}