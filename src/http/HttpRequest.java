package src.http;

import java.util.*;

public class HttpRequest {

    private String method;
    private String path;
    private String version;
    private final Map<String, String> headers = new HashMap<>();
    private byte[] body;
    private final Map<String, String> cookies = new HashMap<>();

    public String getMethod() { 
        return method; 
    }

    public void setMethod(String m) { 
        this.method = m; 
    }

    public String getPath() { 
        return path; 
    }

    public void setPath(String p) { 
        this.path = p; 
    }

    public String getVersion() { 
        return version; 
    }

    public void setVersion(String v) { 
        this.version = v; 
    }

    public Map<String, String> getHeaders() { 
        return headers; 
    }

    public void addHeader(String k, String v) {
        headers.put(k.toLowerCase(), v);
    }

    public byte[] getBody() { 
        return body; 
    }

    public void setBody(byte[] b) { 
        this.body = b; 
    }

    public Map<String, String> getCookies() { 
        return cookies; 
    }

    public void addCookie(String k, String v) {
        cookies.put(k, v);
    }
}
