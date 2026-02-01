package src.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HttpResponse {

    private final int statusCode;
    private final String reason;

    public HttpResponse(int statusCode, String reason) {
        this.statusCode = statusCode;
        this.reason = reason;
    }

    public ByteBuffer toByteBuffer() {
        String response =
                "HTTP/1.1 " + statusCode + " " + reason + "\r\n" +
                "Content-Length: 0\r\n" +
                "\r\n";

        return ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
    }
}