package src;

import java.util.List;
import java.util.Map;

public class ConfigLoader {
    public static Config load(String configPath) throws Exception {
        String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(configPath)));
        // System.out.println("[DEBUG] Config file loaded");
        JsonParser parser = new JsonParser(content);
        Map<String, Object> jsonMap = parser.parseObject();

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

        Object errorPagesObj = jsonMap.get("error_pages");
        if (errorPagesObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> errorPagesMap = (Map<String, Object>) errorPagesObj;
            for (Map.Entry<String, Object> entry : errorPagesMap.entrySet()) {
                config.getErrorPages().put(entry.getKey(), entry.getValue().toString());
            }
        }

        List<Object> routesArray = JsonParser.getArray(jsonMap, "routes");
        System.out.println("[DEBUG] Routes array size: " + routesArray.size());

        for (Object routeObj : routesArray) {
            @SuppressWarnings("unchecked")
            Map<String, Object> routeMap = (Map<String, Object>) routeObj;
            Config.Route route = parseRoute(routeMap);
            config.addRoute(route);
            System.out.println("[DEBUG] Added route: " + route.getPath() +
                    " root=" + route.getRoot() +
                    " methods=" + route.getAllowedMethods());
        }
        config.setClientBodySizeLimit(
                JsonParser.getInt(jsonMap, "client_max_body_size", 1048576));

        Map<String, Object> errorPages = JsonParser.getObject(jsonMap, "error_pages");
        for (Map.Entry<String, Object> entry : errorPages.entrySet()) {
            int statusCode = Integer.parseInt(entry.getKey());
            String filePath = entry.getValue().toString();
            config.addErrorPage(statusCode, filePath);
        }
        // System.out.println("[DEBUG] Total routes loaded: " +
        // config.getRoutes().size());
        return config;
    }

    private static Config.Route parseRoute(Map<String, Object> json) {
        Config.Route route = new Config.Route();
        route.setPath(JsonParser.getString(json, "path", "/"));
        route.setRoot(JsonParser.getString(json, "root", "www"));

        List<Object> methods = JsonParser.getArray(json, "methods");
        for (Object method : methods) {
            route.addAllowedMethod(method.toString());
        }

        String defaultFile = JsonParser.getString(json, "index", null);
        if (defaultFile != null) {
            route.setDefaultFile(defaultFile);
        }

        Object dirListing = json.get("directory_listing");
        if (dirListing instanceof Boolean) {
            route.setDirectoryListing((Boolean) dirListing);
        }

        return route;
    }
}