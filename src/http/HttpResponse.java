package src.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {

    private final int statusCode;
    private final String reason;
    private final Map<String, String> headers = new HashMap<>();
    private byte[] body = new byte[0];

    public HttpResponse(int statusCode, String reason) {
        this.statusCode = statusCode;
        this.reason = reason;
    }

    public void setBody(byte[] body) {
        this.body = body;
        headers.put("Content-Length", String.valueOf(body.length));
    }

    public void setBody(String body) {
        setBody(body.getBytes(StandardCharsets.UTF_8));
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public ByteBuffer toByteBuffer() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ")
          .append(statusCode)
          .append(" ")
          .append(reason)
          .append("\r\n");

        for (Map.Entry<String, String> h : headers.entrySet()) {
            sb.append(h.getKey()).append(": ").append(h.getValue()).append("\r\n");
        }

        sb.append("\r\n");

        byte[] head = sb.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(head.length + body.length);
        buffer.put(head);
        buffer.put(body);
        buffer.flip();
        return buffer;
    }

    public static HttpResponse notFound(String msg) {
        HttpResponse res = new HttpResponse(404, "Not Found");
        res.setBody(msg);
        res.addHeader("Content-Type", "text/plain");
        return res;
    }

    public static HttpResponse internalError(String msg) {
        HttpResponse res = new HttpResponse(500, "Internal Server Error");
        res.setBody(msg);
        res.addHeader("Content-Type", "text/plain");
        return res;
    }
}