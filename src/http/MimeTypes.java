package src.http;

import java.util.HashMap;
import java.util.Map;

public class MimeTypes {
    
    private static final Map<String, String> mimeTypes = new HashMap<>();
    
    static {
        // Text
        mimeTypes.put("html", "text/html");
        mimeTypes.put("htm", "text/html");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("js", "application/javascript");
        mimeTypes.put("json", "application/json");
        mimeTypes.put("xml", "application/xml");
        mimeTypes.put("txt", "text/plain");
        
        // Images
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("svg", "image/svg+xml");
        mimeTypes.put("ico", "image/x-icon");
        mimeTypes.put("webp", "image/webp");
        
        // Fonts
        mimeTypes.put("woff", "font/woff");
        mimeTypes.put("woff2", "font/woff2");
        mimeTypes.put("ttf", "font/ttf");
        mimeTypes.put("otf", "font/otf");
        
        // Other
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("zip", "application/zip");
        mimeTypes.put("mp4", "video/mp4");
        mimeTypes.put("mp3", "audio/mpeg");
    }
    

    public static String getMimeType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "application/octet-stream"; 
        }
        
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return mimeTypes.getOrDefault(extension, "application/octet-stream");
    }
}