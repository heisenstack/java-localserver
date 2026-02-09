package src;

import src.http.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CGIHandler {

    private static final int CGI_TIMEOUT_SECONDS = 10;

    public static HttpResponse handle(HttpRequest request, Config config) {

        try {
            String relativePath = request.getPath().replaceFirst("^/cgi-bin/?", "");

            File root = new File(config.getCgiRoot()).getCanonicalFile();
            File script = new File(root, relativePath).getCanonicalFile();

            if (!script.exists() || !script.canExecute()) {
                return HttpResponse.notFound("CGI script not found");
            }

            ProcessBuilder pb = new ProcessBuilder("/bin/bash", script.getAbsolutePath());
            pb.redirectErrorStream(true);

            Map<String, String> env = pb.environment();

            env.put("REQUEST_METHOD", request.getMethod());
            env.put("SCRIPT_NAME", request.getPath());
            env.put("SERVER_NAME", config.getHost());
            env.put("SERVER_PORT",
                    config.getPorts().isEmpty() ? "8080" :
                            String.valueOf(config.getPorts().get(0)));
            env.put("SERVER_PROTOCOL", "HTTP/1.1");
            env.put("QUERY_STRING", request.getQueryString() != null ? request.getQueryString() : "");
            env.put("CONTENT_LENGTH", request.getBody() != null ? String.valueOf(request.getBody().length) : "0");
            env.put("CONTENT_TYPE", request.getHeader("Content-Type") != null ? request.getHeader("Content-Type") : "");
            env.put("GATEWAY_INTERFACE", "CGI/1.1");

            // Forward HTTP Headers
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                String name = header.getKey().toUpperCase().replace("-", "_");
                env.put("HTTP_" + name, header.getValue());
            }

            System.out.println("[CGI] Script absolute path: " + script.getAbsolutePath());
            System.out.println("[CGI] Exists: " + script.exists() + ", Can execute: " + script.canExecute());

            Process process = pb.start();
            System.out.println("[CGI] Process started successfully");

            if (request.getBody() != null && request.getBody().length > 0) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(request.getBody());
                    os.flush();
                }
            } else {
                process.getOutputStream().close();
            }

            InputStream processOut = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(processOut));

            Map<String, String> cgiHeaders = new HashMap<>();
            ByteArrayOutputStream bodyOut = new ByteArrayOutputStream();

            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) break;
                int idx = line.indexOf(":");
                if (idx != -1) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    cgiHeaders.put(key, value);
                }
            }

            // Read Body
            int b;
            while ((b = processOut.read()) != -1) {
                bodyOut.write(b);
            }

            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errReader.readLine()) != null) {
                System.out.println("[CGI ERR] " + line);
            }

            boolean finished = process.waitFor(CGI_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return HttpResponse.internalError("CGI Timeout");
            }

            int status = 200;
            String statusText = "OK";
            if (cgiHeaders.containsKey("Status")) {
                String statusLine = cgiHeaders.get("Status");
                String[] parts = statusLine.split(" ", 2);
                try {
                    status = Integer.parseInt(parts[0]);
                    if (parts.length > 1) statusText = parts[1];
                } catch (Exception ignored) {}
            }

            HttpResponse response = new HttpResponse(status, statusText);
            for (Map.Entry<String, String> h : cgiHeaders.entrySet()) {
                if (!h.getKey().equalsIgnoreCase("Status")) {
                    response.addHeader(h.getKey(), h.getValue());
                }
            }
            response.setBody(bodyOut.toByteArray());
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponse.internalError("CGI Error: " + e.getMessage());
        }
    }
}
