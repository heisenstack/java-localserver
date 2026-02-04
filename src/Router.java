package src;

import src.http.HttpRequest;
import src.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Router {
    
    public static HttpResponse route(HttpRequest request, Config config) {
        String method = request.getMethod();
        String path = request.getPath();
        
        System.out.println("[REQUEST] " + method + " " + path);
        
        for (Config.Route route : config.getRoutes()) {
            if (matchesRoute(path, route.getPath())) {
                if (!route.getAllowedMethods().contains(method)) {
                    return createErrorResponse(405, "Method Not Allowed", config);
                }
                
                System.out.println("[MATCH] Route matched: " + route.getPath());
                return serveFile(request, route, config);
            }
        }
        
        System.out.println("[404] No route matched for: " + path);
        return createErrorResponse(404, "Not Found", config);
    }
    
    private static HttpResponse serveFile(HttpRequest request, Config.Route route, Config config) {
        try {
            String requestPath = request.getPath();
            
            if (!route.getPath().equals("/")) {
                requestPath = requestPath.substring(route.getPath().length());
            }
            
            if (requestPath.isEmpty() || requestPath.equals("/")) {
                if (route.getDefaultFile() != null) {
                    requestPath = "/" + route.getDefaultFile();
                } else {
                    requestPath = "/index.html";
                }
            }
            
            String rootDir = route.getRoot();
            if (!rootDir.startsWith("src/")) {
                rootDir = "src/" + rootDir;
            }
            
            Path filePath = Paths.get(rootDir + requestPath).normalize();
            
            System.out.println("[FILE] Attempting to serve: " + filePath);
            
            if (!filePath.startsWith(Paths.get(rootDir).normalize())) {
                System.out.println("[403] Directory traversal attempt blocked");
                return createErrorResponse(403, "Forbidden", config);
            }
            
            if (!Files.exists(filePath)) {
                System.out.println("[404] File not found: " + filePath);
                return createErrorResponse(404, "Not Found", config);
            }
            
            if (Files.isDirectory(filePath)) {
                System.out.println("[403] Directory access denied");
                return createErrorResponse(403, "Forbidden", config);
            }
            
            byte[] content = Files.readAllBytes(filePath);
            
            String contentType = getContentType(filePath.toString());
            
            HttpResponse response = new HttpResponse(200, "OK");
            response.setBody(content);
            response.addHeader("Content-Type", contentType);
            response.addHeader("Server", "LocalServer/1.0");
            
            System.out.println("[200] Served: " + filePath + " (" + content.length + " bytes)");
            return response;
            
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to serve file: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse(500, "Internal Server Error", config);
        }
    }
    
    private static HttpResponse createErrorResponse(int statusCode, String reason, Config config) {
        HttpResponse response = new HttpResponse(statusCode, reason);
        
        String errorPagePath = config.getErrorPages().get(String.valueOf(statusCode));
        if (errorPagePath != null) {
            try {
                Path errorFile = Paths.get("src/" + errorPagePath);
                if (Files.exists(errorFile)) {
                    byte[] content = Files.readAllBytes(errorFile);
                    response.setBody(content);
                    response.addHeader("Content-Type", "text/html");
                    return response;
                }
            } catch (Exception e) {
                System.err.println("[WARN] Could not load error page: " + errorPagePath);
            }
        }
        
        String html = "<!DOCTYPE html>\n" +
                     "<html>\n" +
                     "<head><title>" + statusCode + " " + reason + "</title></head>\n" +
                     "<body>\n" +
                     "<​h1>" + statusCode + " " + reason + "</h1>\n" +
                     "<hr>\n" +
                     "<p>LocalServer/1.0</p>\n" +
                     "</body>\n" +
                     "<​/html>";
        
        response.setBody(html);
        response.addHeader("Content-Type", "text/html");
        return response;
    }
    
    private static String getContentType(String filePath) {
        String lower = filePath.toLowerCase();
        
        // Text
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".xml")) return "application/xml; charset=utf-8";
        if (lower.endsWith(".txt")) return "text/plain; charset=utf-8";
        
        // Images
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".webp")) return "image/webp";
        
        // Fonts
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".ttf")) return "font/ttf";
        if (lower.endsWith(".otf")) return "font/otf";
        
        // Other
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".zip")) return "application/zip";
        
        return "application/octet-stream";
    }
    
    private static boolean matchesRoute(String requestPath, String routePath) {
        if (routePath.equals("/")) {
            return true;
        }
        return requestPath.equals(routePath) || requestPath.startsWith(routePath + "/");
    }
}