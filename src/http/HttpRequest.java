package src.http;

import java.util.*;

public class HttpRequest {

    private String method;
    private String path;
    private String version;
    private byte[] body;

    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> cookies = new HashMap<>();
    private final Map<String, String> queryParams = new HashMap<>();
    private final Map<String, String> formData = new HashMap<>();
    private List<MultipartParser.Part> multipartParts = new ArrayList<>();

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

    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
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

    public String getCookie(String name) {
        return cookies.get(name);
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void addQueryParam(String key, String value) {
        queryParams.put(key, value);
    }

    public Map<String, String> getFormData() {
        return formData;
    }

    public List<MultipartParser.Part> getMultipartParts() {
        return multipartParts;
    }

    public void setMultipartParts(List<MultipartParser.Part> parts) {
        this.multipartParts = parts;
    }

    public void parseBody() {
        if (body == null || body.length == 0) return;

        String transferEncoding = getHeader("transfer-encoding");

        if (transferEncoding != null && transferEncoding.equalsIgnoreCase("chunked")) {
        try {
            body = decodeChunkedBody(body);
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to decode chunked body: " + e.getMessage());
            return;
        }
    }

        String contentType = getHeader("content-type");

        if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
            parseQueryString(new String(body), formData);
        } else if (contentType != null && contentType.contains("multipart/form-data")) {
            parseMultipart();
        }
    }
    
    private byte[] decodeChunkedBody(byte[] chunkedBody) throws Exception {
    java.io.ByteArrayOutputStream decoded = new java.io.ByteArrayOutputStream();
    int pos = 0;

    while (pos < chunkedBody.length) {
        int lineEnd = findCRLF(chunkedBody, pos);
        if (lineEnd == -1) {
            throw new Exception("Invalid chunked encoding: missing size line");
        }

        String sizeLine = new String(chunkedBody, pos, lineEnd - pos)
                .split(";")[0]
                .trim();

        int chunkSize;
        try {
            chunkSize = Integer.parseInt(sizeLine, 16);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid chunk size: " + sizeLine);
        }

        pos = lineEnd + 2;

        if (chunkSize == 0) {
            while (pos < chunkedBody.length) {
                int trailerEnd = findCRLF(chunkedBody, pos);
                if (trailerEnd == -1 || trailerEnd == pos) {
                    pos += 2;
                    break;
                }
                pos = trailerEnd + 2;
            }
            break;
        }

        if (pos + chunkSize > chunkedBody.length) {
            throw new Exception("Incomplete chunk data");
        }

        decoded.write(chunkedBody, pos, chunkSize);

        pos += chunkSize;

        if (pos + 1 >= chunkedBody.length ||
            chunkedBody[pos] != '\r' ||
            chunkedBody[pos + 1] != '\n') {
            throw new Exception("Invalid chunk ending");
        }
        pos += 2;
    }

    return decoded.toByteArray();
}

    private int findCRLF(byte[] data, int start) {
        for (int i = start; i < data.length - 1; i++) {
           if (data[i] == '\r' && data[i + 1] == '\n') {
               return i;
            }
        }
               return -1;
    }

    private void parseMultipart() {
        String contentType = getHeader("content-type");
        if (contentType == null) return;

        String boundary = null;
        for (String part : contentType.split(";")) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                boundary = part.substring(9);
                break;
            }
        }

        if (boundary != null && body != null) {
            try {
                multipartParts = MultipartParser.parse(body, boundary);
                for (MultipartParser.Part part : multipartParts) {
                    if (!part.isFile()) {
                        formData.put(part.getName(), new String(part.getData()));
                    }
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Multipart parse failed: " + e.getMessage());
            }
        }
    }

    private void parseQueryString(String query, Map<String, String> target) {
        if (query == null || query.isEmpty()) return;

        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    target.put(
                        java.net.URLDecoder.decode(kv[0], "UTF-8"),
                        java.net.URLDecoder.decode(kv[1], "UTF-8")
                    );
                } catch (Exception ignored) {}
            }
        }
    }

    public String getQueryString() {
    if (this.path == null) return "";

    int idx = this.path.indexOf("?");
    if (idx == -1) return "";

    return this.path.substring(idx + 1);
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "method='" + method + '\n' +
                "path='" + path + '\n' +
                "version='" + version + '\n' +
                "headers=" + headers + '\n' +
                "cookies=" + cookies + '\n' +
                "queryParams=" + queryParams + '\n' +
                "formData=" + formData  + '\n' +
                "multipartParts=" + multipartParts.size() +
                '}';
    }
}
