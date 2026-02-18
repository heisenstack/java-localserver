package src.http;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class RequestParser {

    public static HttpRequest parse(ByteBuffer buffer) throws Exception {
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

        byte[] bodyBytes = null;

        String transferEncoding = req.getHeader("transfer-encoding");
        String contentLength = req.getHeader("content-length");

        if (transferEncoding != null && transferEncoding.equalsIgnoreCase("chunked")) {
            bodyBytes = readChunkedBody(ByteBuffer.wrap(data, headerEndIndex + 4, data.length - (headerEndIndex + 4)));
        } else if (contentLength != null) {
            int len = Integer.parseInt(contentLength);
            bodyBytes = readFixedLengthBody(ByteBuffer.wrap(data, headerEndIndex + 4, data.length - (headerEndIndex + 4)), len);
        }

        req.setBody(bodyBytes);

        parseCookies(req);
        parseQueryParams(req);
        req.parseBody();

        return req;
    }

    // ======= chunked =======
    private static byte[] readChunkedBody(ByteBuffer buffer) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (buffer.hasRemaining()) {
            String line = readLine(buffer);
            if (line == null) break;
            int chunkSize = Integer.parseInt(line.trim(), 16);
            if (chunkSize == 0) break;

            byte[] chunk = new byte[chunkSize];
            buffer.get(chunk);
            out.write(chunk);

            if (buffer.remaining() >= 2) buffer.position(buffer.position() + 2);
        }
        return out.toByteArray();
    }

    private static byte[] readFixedLengthBody(ByteBuffer buffer, int length) {
        byte[] body = new byte[length];
        buffer.get(body, 0, Math.min(length, buffer.remaining()));
        return body;
    }

    private static String readLine(ByteBuffer buffer) {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == '\r') continue;
            if (b == '\n') break;
            line.write(b);
        }
        return line.toString(StandardCharsets.UTF_8);
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
