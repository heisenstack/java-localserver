package src;

import java.util.*;

public class Config {

    private List<Integer> ports;
    private String host;
    private List<Route> routes;

    public Config() {
        this.ports = new ArrayList<>();
        this.host = "localhost";
        this.routes = new ArrayList<>();
    }

    public void addPort(int port) {
        this.ports.add(port);
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public void setPorts(List<Integer> ports) {
        this.ports = ports;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void addRoute(Route route) {
        this.routes.add(route);
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }

    public static class Route {
        private String path;
        private List<String> allowedMethods;
        private String defaultFile;

        public Route() {
            this.allowedMethods = new ArrayList<>();
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public void addAllowedMethod(String method) {
            if (this.allowedMethods == null) {
                this.allowedMethods = new ArrayList<>();
            }
            this.allowedMethods.add(method);
        }

        public String getDefaultFile() {
            return defaultFile;
        }

        public void setDefaultFile(String defaultFile) {
            this.defaultFile = defaultFile;
        }
    }
}