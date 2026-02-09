package src.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {

    private static final String HTTP_VERSION = "HTTP/1.1";

    private final int statusCode;
    private final String reason;
    private final Map<String, String> headers = new HashMap<>();
    private byte[] body = new byte[0];

    public HttpResponse(int statusCode, String reason) {
        this.statusCode = statusCode;
        this.reason = reason;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setBody(byte[] body) {
        this.body = body;
        headers.put("Content-Length", String.valueOf(body.length));
    }

    public void setBody(String body) {
        setBody(body.getBytes(StandardCharsets.UTF_8));
        headers.putIfAbsent("Content-Type", "text/plain; charset=UTF-8");
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void addCookie(String name, String value, int maxAge, String path) {
        StringBuilder cookie = new StringBuilder();
        cookie.append(name).append("=").append(value);

        if (maxAge >= 0) {
            cookie.append("; Max-Age=").append(maxAge);
        }

        cookie.append("; Path=").append(path != null ? path : "/");
        cookie.append("; HttpOnly");

        headers.put("Set-Cookie", cookie.toString());
    }

    public void addSessionCookie(String name, String value) {
        addCookie(name, value, -1, "/");
    }

    public void deleteCookie(String name) {
        addCookie(name, "", 0, "/");
    }

    public ByteBuffer toByteBuffer() {
        headers.putIfAbsent("Date",
            ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.RFC_1123_DATE_TIME)
        );
        headers.putIfAbsent("Connection", "close");

        StringBuilder response = new StringBuilder();

        response.append(HTTP_VERSION)
                .append(" ")
                .append(statusCode)
                .append(" ")
                .append(reason)
                .append("\r\n");

        for (Map.Entry<String, String> h : headers.entrySet()) {
            response.append(h.getKey())
                    .append(": ")
                    .append(h.getValue())
                    .append("\r\n");
        }

        response.append("\r\n");

        byte[] headerBytes = response.toString().getBytes(StandardCharsets.UTF_8);

        if (body == null || body.length == 0) {
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
        res.setBody(body);
        return res;
    }

    public static HttpResponse notFound(String body) {
        HttpResponse res = new HttpResponse(404, "Not Found");
        res.setBody(body);
        return res;
    }

    public static HttpResponse internalError(String body) {
        HttpResponse res = new HttpResponse(500, "Internal Server Error");
        res.setBody(body);
        return res;
    }

}
