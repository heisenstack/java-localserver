package src.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class RequestParser {

    public static HttpRequest parse(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        
        String raw = new String(bytes, StandardCharsets.UTF_8);
        String[] parts = raw.split("\r\n\r\n", 2);

        String[] lines = parts[0].split("\r\n");
        String[] start = lines[0].split(" ");

        HttpRequest req = new HttpRequest();
        req.setMethod(start[0]);
        req.setPath(start[1]);
        req.setVersion(start.length > 2 ? start[2] : "HTTP/1.1");

        for (int i = 1; i < lines.length; i++) {
            if (lines[i].contains(":")) {
                String[] h = lines[i].split(":", 2);
                req.addHeader(h[0], h[1]);
            }
        }

        return req;
    }
}