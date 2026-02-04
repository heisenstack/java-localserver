package src.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {

    private final int statusCode;
    private final String reason;
    private final Map<String, String> headers = new HashMap<>();
    private byte[] body;

    public HttpResponse(int statusCode, String reason) {
        this.statusCode = statusCode;
        this.reason = reason;
        this.body = new byte[0];
    }

    public void setBody(byte[] body) {
        this.body = body;
        this.headers.put("Content-Length", String.valueOf(body.length));
    }

    public void setBody(String body) {
        setBody(body.getBytes(StandardCharsets.UTF_8));
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public ByteBuffer toByteBuffer() {
        StringBuilder sb = new StringBuilder();
        
        // Status line
        sb.append("HTTP/1.1 ").append(statusCode).append(" ").append(reason).append("\r\n");
        
        // Headers
        for (Map.Entry<String, String> header : headers.entrySet()) {
            sb.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
        
        // Empty line
        sb.append("\r\n");
        
        // Combine headers and body
        byte[] headerBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(headerBytes.length + body.length);
        buffer.put(headerBytes);
        buffer.put(body);
        buffer.flip();
        
        return buffer;
    }
}