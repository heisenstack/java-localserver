package src;

import src.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class CgiProcess {

    public enum State { WRITING_STDIN, READING, DONE, ERROR, TIMEOUT }

    private static final int     MAX_BODY_SIZE  = 10 * 1024 * 1024; // 10MB
    private static final long    CGI_TIMEOUT_MS = 20_000;
    private static final Pattern SAFE_PATH      = Pattern.compile("^[a-zA-Z0-9._/-]+$");
    private static final Pattern SAFE_HEADER    = Pattern.compile("^[a-zA-Z0-9\\-]+$");

    private final Process          process;
    private final InputStream      stdout;
    private final OutputStream     stdin;

    private final byte[] requestBody;
    private int          bytesWritten = 0;

    private final ByteArrayOutputStream rawOutput  = new ByteArrayOutputStream();
    private final byte[]                readBuffer = new byte[8192];

    private Map<String, String> cgiHeaders = new LinkedHashMap<>();
    private byte[]              cgiBody    = null;
    private int                 statusCode = 200;
    private String              statusText = "OK";

    private State    state;
    private final long startTime = System.currentTimeMillis();

    public CgiProcess(HttpRequest request, Config config) throws Exception {
        ProcessBuilder pb = buildProcess(request, config);
        this.process     = pb.start();
        this.stdout      = process.getInputStream();
        this.stdin       = process.getOutputStream();
        this.requestBody = (request.getBody() != null) ? request.getBody() : new byte[0];

        if (requestBody.length > 0) {
            this.state = State.WRITING_STDIN;
        } else {
            stdin.close();
            this.state = State.READING;
        }
    }

    public void tick() {
        if (isTimedOut()) {
            process.destroyForcibly();
            state = State.TIMEOUT;
            return;
        }

        try {
            switch (state) {
                case WRITING_STDIN -> tickWriteStdin();
                case READING       -> tickRead();
                default            -> {}
            }
        } catch (Exception e) {
            System.err.println("[CGI tick error] " + e.getMessage());
            process.destroyForcibly();
            state = State.ERROR;
        }
    }

    private void tickWriteStdin() throws IOException {
        int remaining = requestBody.length - bytesWritten;

        if (remaining <= 0) {
            stdin.close();
            state = State.READING;
            return;
        }

        int chunkSize = Math.min(4096, remaining);
        stdin.write(requestBody, bytesWritten, chunkSize);
        stdin.flush();
        bytesWritten += chunkSize;

        if (bytesWritten >= requestBody.length) {
            stdin.close();
            state = State.READING;
        }
    }

   
    private void tickRead() throws Exception {
        
        int available = stdout.available();

        if (available > 0) {
            int toRead = Math.min(available, readBuffer.length);
            int len    = stdout.read(readBuffer, 0, toRead);
            if (len > 0) {
                rawOutput.write(readBuffer, 0, len);
                if (rawOutput.size() > MAX_BODY_SIZE) {
                    process.destroyForcibly();
                    state = State.ERROR;
                    return;
                }
            }
        }

        
        if (process.waitFor(0, TimeUnit.MILLISECONDS)) {
        
            int len;
            while ((len = stdout.read(readBuffer)) != -1) {
                rawOutput.write(readBuffer, 0, len);
            }
            parseOutput();
            state = State.DONE;
        }
    }

    private void parseOutput() {
        String raw      = rawOutput.toString(StandardCharsets.UTF_8);
        int    sepIdx   = raw.indexOf("\r\n\r\n");
        int    sepLen   = 4;

        if (sepIdx == -1) {
            sepIdx = raw.indexOf("\n\n");
            sepLen = 2;
        }

        if (sepIdx == -1) {
           
            cgiBody = rawOutput.toByteArray();
            return;
        }

        String headerSection = raw.substring(0, sepIdx);
        String bodySection   = raw.substring(sepIdx + sepLen);
        cgiBody              = bodySection.getBytes(StandardCharsets.UTF_8);

        for (String line : headerSection.split("\r?\n")) {
            int idx = line.indexOf(":");
            if (idx == -1) continue;
            String key   = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            if (SAFE_HEADER.matcher(key).matches())
                cgiHeaders.put(key, value);
        }

        if (cgiHeaders.containsKey("Status")) {
            String[] parts = cgiHeaders.get("Status").split(" ", 2);
            try {
                statusCode = Integer.parseInt(parts[0]);
                if (parts.length > 1) statusText = parts[1];
            } catch (NumberFormatException ignored) {}
        }
    }

    public HttpResponse buildResponse() {
        HttpResponse response = new HttpResponse(statusCode, statusText);
        for (Map.Entry<String, String> h : cgiHeaders.entrySet()) {
            if (!h.getKey().equalsIgnoreCase("Status"))
                response.addHeader(h.getKey(), h.getValue());
        }
        response.setBody(cgiBody != null ? cgiBody : new byte[0]);
        return response;
    }

    private static ProcessBuilder buildProcess(HttpRequest request, Config config)
            throws IOException {

        String relativePath = request.getPath().replaceFirst("^/cgi-bin/?", "");

        if (relativePath.contains("..") || relativePath.contains("~"))
            throw new SecurityException("Invalid path");

        if (!SAFE_PATH.matcher(relativePath).matches())
            throw new SecurityException("Invalid characters in path");

        File root   = new File(config.getCgiRoot()).getCanonicalFile();
        File script = new File(root, relativePath).getCanonicalFile();

        if (!script.getPath().startsWith(root.getPath() + File.separator))
            throw new SecurityException("Path traversal detected");

        if (!script.exists() || !script.canExecute())
            throw new FileNotFoundException("Script not found or not executable");

        ProcessBuilder pb = new ProcessBuilder("/bin/bash", script.getAbsolutePath());
        pb.redirectErrorStream(true); 

        Map<String, String> env = pb.environment();
        env.clear(); 

        env.put("REQUEST_METHOD",    sanitize(request.getMethod()));
        env.put("SCRIPT_NAME",       sanitize(request.getPath()));
        env.put("SERVER_NAME",       sanitize(config.getHost()));
        env.put("SERVER_PORT",       config.getPorts().isEmpty() ? "8080"
                                     : String.valueOf(config.getPorts().get(0)));
        env.put("SERVER_PROTOCOL",   "HTTP/1.1");
        env.put("GATEWAY_INTERFACE", "CGI/1.1");
        env.put("QUERY_STRING",      sanitize(
                                     request.getQueryString() != null ? request.getQueryString() : ""));
        env.put("CONTENT_TYPE",      sanitize(
                                     request.getHeader("Content-Type") != null
                                     ? request.getHeader("Content-Type") : ""));
        env.put("CONTENT_LENGTH",    String.valueOf(
                                     request.getBody() != null ? request.getBody().length : 0));
        env.put("PATH",              "/usr/local/bin:/usr/bin:/bin");

        for (Map.Entry<String, String> h : request.getHeaders().entrySet()) {
            if (!SAFE_HEADER.matcher(h.getKey()).matches()) continue;
            env.put("HTTP_" + h.getKey().toUpperCase().replace("-", "_"),
                    sanitize(h.getValue()));
        }

        return pb;
    }

    private static String sanitize(String v) {
        return v == null ? "" : v.replaceAll("[\\r\\n\\x00]", "").trim();
    }

    public void  destroy()   { process.destroyForcibly(); }
    public State getState()  { return state; }
    public boolean isDone()    { return state == State.DONE;    }
    public boolean isError()   { return state == State.ERROR;   }
    public boolean isTimeout() { return state == State.TIMEOUT; }

    private boolean isTimedOut() {
        return System.currentTimeMillis() - startTime > CGI_TIMEOUT_MS;
    }
}