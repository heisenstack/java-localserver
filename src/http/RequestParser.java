package src.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class RequestParser {

    public static HttpRequest parse(ByteBuffer buffer) {
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        
        String raw = new String(data, StandardCharsets.UTF_8);
        
        int headerEndIndex = raw.indexOf("\r\n\r\n");
        if (headerEndIndex == -1) {
            throw new RuntimeException("Invalid HTTP request: no header end found");
        }
        
        String headerSection = raw.substring(0, headerEndIndex);
        String[] lines = headerSection.split("\r\n");
        
        String[] requestLine = lines[0].split(" ");
        if (requestLine.length < 3) {
            throw new RuntimeException("Invalid request line");
        }
        
        HttpRequest req = new HttpRequest();
        req.setMethod(requestLine[0]);
        req.setPath(requestLine[1]);
        req.setVersion(requestLine[2]);
        
        for (int i = 1; i < lines.length; i++) {
            int colonIndex = lines[i].indexOf(":");
            if (colonIndex > 0) {
                String key = lines[i].substring(0, colonIndex).trim();
                String value = lines[i].substring(colonIndex + 1).trim();
                req.addHeader(key, value);
            }
        }
        
        if (headerEndIndex + 4 < raw.length()) {
            int bodyStart = headerEndIndex + 4;
            byte[] bodyBytes = new byte[data.length - bodyStart];
            System.arraycopy(data, bodyStart, bodyBytes, 0, bodyBytes.length);
            req.setBody(bodyBytes);
        }
        
        parseCookies(req);
        
        parseQueryParams(req);
        
        req.parseBody();
        
        return req;
    }

    private static void parseCookies(HttpRequest req) {
        String cookie = req.getHeaders().get("cookie");
        if (cookie == null) return;

        for (String c : cookie.split(";")) {
            String[] kv = c.trim().split("=", 2);
            if (kv.length == 2) {
                req.addCookie(kv[0], kv[1]);
            }
        }
    }
    
    private static void parseQueryParams(HttpRequest req) {
        String path = req.getPath();
        int queryIndex = path.indexOf("?");
        
        if (queryIndex != -1) {
            String actualPath = path.substring(0, queryIndex);
            String queryString = path.substring(queryIndex + 1);
            
            req.setPath(actualPath);
            
            for (String param : queryString.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2) {
                    req.addQueryParam(urlDecode(kv[0]), urlDecode(kv[1]));
                } else if (kv.length == 1) {
                    req.addQueryParam(urlDecode(kv[0]), "");
                }
            }
        }
    }
    
    private static String urlDecode(String value) {
        try {
            return java.net.URLDecoder.decode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}