package src;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ConfigLoader {

    public static List<Config> load(String configPath) throws Exception {

        String content = Files.readString(Paths.get(configPath));

        JsonParser parser = new JsonParser(content);
        Map<String, Object> rootJson = parser.parseObject();

        List<Object> serversArray =
                JsonParser.getArray(rootJson, "servers");

        List<Config> configs = new ArrayList<>();

        for (Object serverObj : serversArray) {

            @SuppressWarnings("unchecked")
            Map<String, Object> json =
                    (Map<String, Object>) serverObj;

            Config config = new Config();

            config.setHost(
                    JsonParser.getString(json, "host", "localhost"));

            List<Object> portsArray =
                    JsonParser.getArray(json, "ports");

            if (portsArray.isEmpty()) {
                config.addPort(8080);
            } else {
                for (Object port : portsArray) {
                    config.addPort(((Number) port).intValue());
                }
            }

            config.setClientBodySizeLimit(
                    JsonParser.getInt(json,
                            "client_max_body_size", 1048576)
            );

            Map<String, Object> errorPages =
                    JsonParser.getObject(json, "error_pages");

            for (Map.Entry<String, Object> entry :
                    errorPages.entrySet()) {

                int statusCode =
                        Integer.parseInt(entry.getKey());

                config.addErrorPage(
                        statusCode,
                        entry.getValue().toString()
                );
            }

            List<Object> routesArray =
                    JsonParser.getArray(json, "routes");

            for (Object routeObj : routesArray) {

                @SuppressWarnings("unchecked")
                Map<String, Object> routeMap =
                        (Map<String, Object>) routeObj;

                Config.Route route =
                        parseRoute(routeMap);

                config.addRoute(route);
            }

            configs.add(config);
        }

        return configs;
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
        
        Object cgi = json.get("cgi");
        if (cgi instanceof Boolean) {
            route.setCgi((Boolean) cgi);
        }

        return route;
    }
}
