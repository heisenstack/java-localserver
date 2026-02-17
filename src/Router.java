package src;

import src.http.HttpRequest;
import src.http.HttpResponse;
import src.http.MimeTypes;
import src.http.MultipartParser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import src.http.Session;

public class Router {
    

    public static HttpResponse route(HttpRequest request, Config config) {
    try {
        String method = request.getMethod();
        String path = request.getPath().split("\\?")[0];

        System.out.println("[REQUEST] " + method + " " + path);

        if (path.equals("/login")) {
            return handleLogin(request, config);
        }
        if (path.equals("/dashboard")) {
            return handleDashboard(request, config);
        }
        if (path.equals("/logout")) {
            return handleLogout(request, config);
        }

        Config.Route route = findRoute(path, config);
        if (route == null) {
            return error404(config);
        }
        
        if (!route.getAllowedMethods().isEmpty() &&
            !route.getAllowedMethods().contains(method)) {
            return error405(config);
        }

        if (route.isCgi()) {
            return CGIHandler.handle(request, config);
        }

        if (!route.getAllowedMethods().isEmpty() &&
            !route.getAllowedMethods().contains(method)) {
            return error405(config);
        }

        if (route.getRedirect() != null) {
            return redirect(route.getRedirect());
        }

        switch (method) {
            case "GET":
                return handleGet(path, route, config);
            case "POST":
                return handlePost(path, route, request, config); // Only non-CGI POST
            case "DELETE":
                return handleDelete(path, route, config);
            default:
                return error405(config);
        }

    } catch (Exception e) {
        e.printStackTrace();
        return error500(config);
    }
}

    private static Config.Route findRoute(String requestPath, Config config) {
        Config.Route bestMatch = null;
        int longestMatch = 0;
        
        for (Config.Route route : config.getRoutes()) {
            String routePath = route.getPath();
            
            if (requestPath.equals(routePath) || requestPath.startsWith(routePath)) {
                if (routePath.length() > longestMatch) {
                    longestMatch = routePath.length();
                    bestMatch = route;
                }
            }
        }
        
        return bestMatch;
    }
    

    private static HttpResponse handleGet(String requestPath, Config.Route route, Config config) {
        try {
            String root = route.getRoot();
            if (root == null) {
                root = ".";
            }
            
            String routePath = route.getPath();
            String relativePath = requestPath;
            if (requestPath.startsWith(routePath) && !routePath.equals("/")) {
                relativePath = requestPath.substring(routePath.length());
            }
            
            Path filePath = Paths.get(root, relativePath).normalize();
            File file = filePath.toFile();
            
            if (!filePath.startsWith(Paths.get(root).normalize())) {
                return error403(config);
            }
            
            if (!file.exists()) {
                return error404(config);
            }
            
            if (file.isDirectory()) {
                return handleDirectory(file, route, config);
            }
            
            return serveFile(file);
            
        } catch (Exception e) {
            e.printStackTrace();
            return error500(config);
        }
    }
    

    private static HttpResponse handleDirectory(File dir, Config.Route route, Config config) {
        try {
            String defaultFile = route.getDefaultFile();
            if (defaultFile != null) {
                File indexFile = new File(dir, defaultFile);
                if (indexFile.exists() && indexFile.isFile()) {
                    return serveFile(indexFile);
                }
            }
            
            if (route.isDirectoryListing()) {
                return generateDirectoryListing(dir);
            }
            
            return error403(config);
            
        } catch (Exception e) {
            e.printStackTrace();
            return error500(config);
        }
    }
    

    private static HttpResponse serveFile(File file) throws IOException {
        byte[] content = Files.readAllBytes(file.toPath());
        String mimeType = MimeTypes.getMimeType(file.getName());
        
        HttpResponse response = new HttpResponse(200, "OK");
        response.addHeader("Content-Type", mimeType);
        response.setBody(content);
        
        return response;
    }
    

private static HttpResponse generateDirectoryListing(File dir) {
    StringBuilder html = new StringBuilder();
    html.append("<​!DOCTYPE html>");
    html.append("<​html>");
    html.append("<head><title>Index of ").append(dir.getName()).append("</title></head>");
    html.append("<​body>");
    html.append("<h1>Index of /").append(dir.getName()).append("<​/h1>");
    html.append("<​hr>");
    html.append("<​ul>");
    
    File[] files = dir.listFiles();
    if (files != null) {
        for (File file : files) {
            String name = file.getName();
            if (file.isDirectory()) {
                name += "/";
            }
            html.append("<li><a href=\"").append(name).append("\">")
                .append(name).append("</a></li>");
        }
    }
    
    html.append("<​/ul>");
    html.append("<​hr>");
    html.append("<​/body>");
    html.append("<​/html>");
    
    return HttpResponse.ok(html.toString());
}
 private static HttpResponse handlePost(String path, Config.Route route, 
                                      HttpRequest request, Config config) {
    try {
        System.out.println("[POST] Path: " + path);
        
        byte[] body = request.getBody();
        if (body != null && body.length > config.getClientBodySizeLimit()) {
            return error413(config);
        }
        
        List<MultipartParser.Part> parts = request.getMultipartParts();
        List<String> uploadedFiles = new ArrayList<>();
        
        if (parts != null && !parts.isEmpty()) {
            File uploadsDir = new File("uploads");
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs();
            }
            
            for (MultipartParser.Part part : parts) {
                if (part.isFile() && part.getData().length > 0) {
                    String filename = sanitizeFilename(part.getFilename());
                    File uploadFile = new File(uploadsDir, filename);
                    
                    try (FileOutputStream fos = new FileOutputStream(uploadFile)) {
                        fos.write(part.getData());
                    }
                    
                    uploadedFiles.add(filename);
                    System.out.println("[UPLOAD] Saved: " + filename);
                }
            }
        }
        
        Map<String, String> formData = request.getFormData();
        
        String html = "<html><head><title>Upload Success</title>" +
                     "<link rel='stylesheet' href='/style.css'></head><body>" +
                     "<h1>Upload Successful!</h1>" +
                     "<p><strong>Path:</strong> " + path + "<​/p>";
        
        if (!uploadedFiles.isEmpty()) {
            html += "<h2>Uploaded Files:</h2><ul>";
            for (String f : uploadedFiles) {
                html += "<li><a href='/uploads/" + f + "' target='_blank'>" + f + "</a></li>";
            }
            html += "<​/ul>";
        }
        
        if (!formData.isEmpty()) {
            html += "<h2>Form Data:</h2><ul>";
            for (Map.Entry<String, String> e : formData.entrySet()) {
                html += "<li><strong>" + e.getKey() + ":</strong> " + e.getValue() + "<​/li>";
            }
            html += "<​/ul>";
        }
        
        html += "<hr><p><a href='/'>Back to Home</a></p></body></html>";
        
        HttpResponse response = new HttpResponse(200, "OK");
        response.addHeader("Content-Type", "text/html; charset=UTF-8");
        response.setBody(html.getBytes(StandardCharsets.UTF_8));
        
        return response;
        
    } catch (Exception e) {
        e.printStackTrace();
        return error500(config);
    }
}


// private static String escapeHtml(String text) {
//     if (text == null) return "";
//     return text.replace("&", "&amp;")
//                .replace("<​", "&lt;")
//                .replace(">", "&gt;")
//                .replace("\"", "&quot;")
//                .replace("'", "&#x27;");
// }
    

    private static String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed";
        
        filename = filename.replaceAll("[/\\\\]", "_");
        
        filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        if (filename.length() > 255) {
            filename = filename.substring(0, 255);
        }
        
        return filename;
    }
    

private static HttpResponse handleDelete(String path, Config.Route route, Config config) {
    try {
        System.out.println("[DELETE] Path: " + path);
        
        String root = route.getRoot();
        if (root == null) {
            root = ".";
        }
        
        String routePath = route.getPath();
        String relativePath = path;
        if (path.startsWith(routePath) && !routePath.equals("/")) {
            relativePath = path.substring(routePath.length());
        }
        
        Path filePath = Paths.get(root, relativePath).normalize();
        File file = filePath.toFile();
        
        if (!filePath.startsWith(Paths.get(root).normalize())) {
            System.out.println("[DELETE] Forbidden: directory traversal attempt");
            return error403(config);
        }
        
        if (!file.exists()) {
            System.out.println("[DELETE] Not found: " + file.getAbsolutePath());
            return error404(config);
        }
        
        if (file.isDirectory()) {
            System.out.println("[DELETE] Forbidden: cannot delete directory");
            return error403(config);
        }
        
        boolean deleted = file.delete();
        
        if (deleted) {
            System.out.println("[DELETE] Successfully deleted: " + file.getName());
            
            String html = "<html><head><title>File Deleted</title>" +
                         "<link rel='stylesheet' href='/style.css'></head><body>" +
                         "<h1>File Deleted Successfully</h1>" +
                         "<p><strong>File:</strong> " + file.getName() + "<​/p>" +
                         "<p>The file has been permanently deleted.</p>" +
                         "<hr><p><a href='/uploads/'>View Uploads</a> | " +
                         "<a href='/'>Back to Home</a></p></body></html>";
            
            HttpResponse response = new HttpResponse(200, "OK");
            response.addHeader("Content-Type", "text/html; charset=UTF-8");
            response.setBody(html.getBytes(StandardCharsets.UTF_8));
            return response;
        } else {
            System.out.println("[DELETE] Failed to delete: " + file.getName());
            return error500(config);
        }
        
    } catch (Exception e) {
        e.printStackTrace();
        return error500(config);
    }
}

    private static HttpResponse redirect(String location) {
        HttpResponse response = new HttpResponse(301, "Moved Permanently");
        response.addHeader("Location", location);
        response.setBody("<h1>Redirecting...</h1>");
        return response;
    }
    

    private static HttpResponse error403(Config config) {
        return loadErrorPage(403, "Forbidden", config);
    }
    
    private static HttpResponse error404(Config config) {
        return loadErrorPage(404, "Not Found", config);
    }
    
    private static HttpResponse error405(Config config) {
        return loadErrorPage(405, "Method Not Allowed", config);
    }
    
    private static HttpResponse error500(Config config) {
        return loadErrorPage(500, "Internal Server Error", config);
    }
    
    private static HttpResponse error413(Config config) {
        return loadErrorPage(413, "Payload Too Large", config);
    }
    
private static HttpResponse loadErrorPage(int statusCode, String reasonPhrase, Config config) {
    try {
        String errorPagePath = config.getErrorPages().get(statusCode);
        if (errorPagePath != null) {
            File errorFile = new File(errorPagePath);
            if (errorFile.exists()) {
                byte[] content = Files.readAllBytes(errorFile.toPath());
                HttpResponse response = new HttpResponse(statusCode, reasonPhrase);
                response.addHeader("Content-Type", "text/html; charset=UTF-8");
                response.setBody(content);
                return response;
            }
        }
    } catch (Exception e) {
    }
    
    String html = "<​!DOCTYPE html>" +
                 "<​html>" +
                 "<head><title>" + statusCode + " " + reasonPhrase + "</title></head>" +
                 "<​body>" +
                 "<​h1>" + statusCode + " " + reasonPhrase + "<​/h1>" +
                 "<​/body>" +
                 "<​/html>";
    
    HttpResponse response = new HttpResponse(statusCode, reasonPhrase);
    response.addHeader("Content-Type", "text/html; charset=UTF-8");
    response.setBody(html);
    return response;
}

// private static Session getOrCreateSession(HttpRequest request, HttpResponse response) {
//     String sessionId = request.getCookie("SESSIONID");
//     Session session = Session.getSession(sessionId);
    
//     if (session == null) {
//         session = Session.createSession();
//         response.addSessionCookie("SESSIONID", session.getId());
//     }
    
//     return session;
// }

private static HttpResponse handleLogin(HttpRequest request, Config config) {
    // System.out.println("[LOGIN] Handling login request" + request);
    String method = request.getMethod();
    
    if (method.equals("GET")) {
        String sessionId = request.getCookie("SESSIONID");
        Session session = Session.getSession(sessionId);
        
        if (session != null && session.hasAttribute("username")) {
            HttpResponse response = new HttpResponse(302, "Found");
            response.addHeader("Location", "/dashboard");
            response.setBody("Already logged in");
            System.out.println("[LOGIN] User already logged in, redirecting to dashboard");
            return response;
        }
        
        try {
            File loginFile = new File("www/login.html");
            byte[] content = Files.readAllBytes(loginFile.toPath());
            
            HttpResponse response = new HttpResponse(200, "OK");
            response.addHeader("Content-Type", "text/html; charset=UTF-8");
            response.setBody(content);
            return response;
        } catch (Exception e) {
            return error500(config);
        }
        
    } else if (method.equals("POST")) {
        Map<String, String> formData = request.getFormData();
        String username = formData.get("username");
        String password = formData.get("password");
        
        if (username != null && password != null && 
            username.equals("admin") && password.equals("password")) {
            
            HttpResponse response = new HttpResponse(302, "Found");
            
            Session session = Session.createSession();
            session.setAttribute("username", username);
            session.setAttribute("loginTime", System.currentTimeMillis());
            
            response.addSessionCookie("SESSIONID", session.getId());
            response.addHeader("Location", "/dashboard");
            response.setBody("Redirecting...");
            
            System.out.println("[LOGIN] User logged in: " + username);
            return response;
            
        } else {
            try {
                File failFile = new File("www/login-fail.html");
                if (failFile.exists()) {
                    byte[] content = Files.readAllBytes(failFile.toPath());
                    HttpResponse response = new HttpResponse(200, "OK");
                    response.addHeader("Content-Type", "text/html; charset=UTF-8");
                    response.setBody(content);
                    return response;
                }
            } catch (Exception e) {
            }
            
            return error500(config);
        }
    }
    
    return error405(config);
}


private static HttpResponse handleDashboard(HttpRequest request, Config config) {
    String sessionId = request.getCookie("SESSIONID");
    Session session = Session.getSession(sessionId);
    
    if (session == null || !session.hasAttribute("username")) {
        HttpResponse response = new HttpResponse(302, "Found");
        response.addHeader("Location", "/login");
        response.setBody("Redirecting to login...");
        return response;
    }
    
    try {
        String username = (String) session.getAttribute("username");
        long loginTime = (Long) session.getAttribute("loginTime");
        long sessionAge = (System.currentTimeMillis() - loginTime) / 1000;
        
        StringBuilder sessionData = new StringBuilder();
        for (Map.Entry<String, Object> entry : session.getAttributes().entrySet()) {
            sessionData.append("<li><strong>")
                      .append(entry.getKey())
                      .append(":</strong> ")
                      .append(entry.getValue())
                      .append("<​/li>");
        }
        
        File templateFile = new File("www/dashboard.html");
        String html = new String(Files.readAllBytes(templateFile.toPath()), StandardCharsets.UTF_8);
        
        html = html.replace("{{username}}", username);
        html = html.replace("{{sessionId}}", session.getId());
        html = html.replace("{{sessionAge}}", String.valueOf(sessionAge));
        html = html.replace("{{sessionData}}", sessionData.toString());
        
        HttpResponse response = new HttpResponse(200, "OK");
        response.addHeader("Content-Type", "text/html; charset=UTF-8");
        response.setBody(html.getBytes(StandardCharsets.UTF_8));
        return response;
        
    } catch (Exception e) {
        e.printStackTrace();
        return error500(config);
    }
}


private static HttpResponse handleLogout(HttpRequest request, Config config) {
    String sessionId = request.getCookie("SESSIONID");
    
    if (sessionId != null) {
        Session.destroySession(sessionId);
    }
    
    HttpResponse response = new HttpResponse(302, "Found");
    response.deleteCookie("SESSIONID");
    response.addHeader("Location", "/");
    response.setBody("Logging out...");
    
    System.out.println("[LOGOUT] User logged out");
    return response;
}
}