package src;

import java.util.*;

// import src.Config.Route;

public class Config {

    private List<Integer> ports;
    private String host;
    private String cgiRoot;
    private List<Route> routes;
    private Map<String, String> errorPages;

    public Config() {
        this.ports = new ArrayList<>();
        this.host = "localhost";
        this.routes = new ArrayList<>();
        this.errorPages = new HashMap<>();
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
    
    public String getCgiRoot() {
        return cgiRoot;
    }

    public void setCgiRoot(String cgiRoot) {
        this.cgiRoot = cgiRoot;
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

    public Map<String, String> getErrorPages() {
        return errorPages;
    }

    public void setErrorPages(Map<String, String> errorPages) {
        this.errorPages = errorPages;
    }

    public static class Route {
        private String path;
        private String root;
        private List<String> allowedMethods;
        private String defaultFile;
        private boolean directoryListing;

        public Route() {
            this.allowedMethods = new ArrayList<>();
            this.directoryListing = false;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
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

        public boolean isDirectoryListing() {
            return directoryListing;
        }

        public void setDirectoryListing(boolean directoryListing) {
            this.directoryListing = directoryListing;
        }
    }
}