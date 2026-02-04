package src.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpResponse {

    private String version = "HTTP/1.1";
    private int statusCode;
    private String reasonPhrase;

    private final Map<String, String> headers = new LinkedHashMap<>();
    private byte[] body;

    public HttpResponse(int statusCode, String reasonPhrase) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void setBody(byte[] body) {
        this.body = body;
        addHeader("Content-Length", String.valueOf(body.length));
    }

    public void setBody(String body) {
        setBody(body.getBytes(StandardCharsets.UTF_8));
    }

    public int getStatusCode() {
        return statusCode;
    }

   public ByteBuffer toByteBuffer() {
    StringBuilder response = new StringBuilder();

    response.append(version)
            .append(" ")
            .append(statusCode)
            .append(" ")
            .append(reasonPhrase)
            .append("\r\n");

    for (Map.Entry<String, String> h : headers.entrySet()) {
        response.append(h.getKey())
                .append(": ")
                .append(h.getValue())
                .append("\r\n");
    }

    response.append("\r\n");

    byte[] headerBytes = response.toString().getBytes(StandardCharsets.UTF_8);
    
    System.out.println("[DEBUG] Response headers:\n" + response.toString());

    if (body == null) {
        return ByteBuffer.wrap(headerBytes);
    }

    ByteBuffer buffer = ByteBuffer.allocate(headerBytes.length + body.length);
    buffer.put(headerBytes);
    buffer.put(body);
    buffer.flip();

    return buffer;
}

    public static HttpResponse ok(String body) {
        HttpResponse res = new HttpResponse(200, "OK");
        res.addHeader("Content-Type", "text/html; charset=UTF-8");
        res.setBody(body);
        return res;
    }

    public static HttpResponse notFound(String body) {
        HttpResponse res = new HttpResponse(404, "Not Found");
        res.addHeader("Content-Type", "text/html; charset=UTF-8");
        res.setBody(body);
        return res;
    }

    public static HttpResponse internalError(String body) {
        HttpResponse res = new HttpResponse(500, "Internal Server Error");
        res.addHeader("Content-Type", "text/html; charset=UTF-8");
        res.setBody(body);
        return res;
    }

    public void addCookie(String name, String value, int maxAge, String path) {
        StringBuilder cookie = new StringBuilder();
        cookie.append(name).append("=").append(value);

        if (maxAge >= 0) {
            cookie.append("; Max-Age=").append(maxAge);
        }

        if (path != null) {
            cookie.append("; Path=").append(path);
        } else {
            cookie.append("; Path=/");
        }

        cookie.append("; HttpOnly");

        addHeader("Set-Cookie", cookie.toString());
    }

    public void addSessionCookie(String name, String value) {
        addCookie(name, value, -1, "/");
    }

    public void deleteCookie(String name) {
        addCookie(name, "", 0, "/");
    }
    
}
