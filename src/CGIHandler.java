package src;

import src.http.*;

import java.io.*;
import java.util.*;

public class CGIHandler {

    public static HttpResponse handle(HttpRequest request, Config config) {

        try {
            String scriptPath = config.getCgiRoot() + request.getPath();
            File script = new File(scriptPath);

            if (!script.exists() || !script.canExecute()) {
                return HttpResponse.notFound("CGI script not found");
            }

            ProcessBuilder pb = new ProcessBuilder(scriptPath);
            pb.environment().put("REQUEST_METHOD", request.getMethod());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            if (request.getBody() != null) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(request.getBody());
                }
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            Map<String, String> cgiHeaders = new LinkedHashMap<>();
            ByteArrayOutputStream bodyOut = new ByteArrayOutputStream();

            boolean headersEnded = false;
            while ((line = reader.readLine()) != null) {
                if (!headersEnded) {
                    if (line.isEmpty()) {
                        headersEnded = true;
                    } else {
                        int idx = line.indexOf(':');
                        if (idx != -1) {
                            String key = line.substring(0, idx).trim();
                            String value = line.substring(idx + 1).trim();
                            cgiHeaders.put(key, value);
                        }
                    }
                } else {
                    bodyOut.write((line + "\n").getBytes());
                }
            }

            process.waitFor();

            HttpResponse res = new HttpResponse(200, "OK");

            for (Map.Entry<String, String> h : cgiHeaders.entrySet()) {
                res.addHeader(h.getKey(), h.getValue());
            }

            res.addHeader("Content-Type", cgiHeaders.getOrDefault("Content-Type", "text/html; charset=UTF-8"));
            res.setBody(bodyOut.toByteArray());

            return res;

        } catch (Exception e) {
            return HttpResponse.internalError("CGI Error: " + e.getMessage());
        }
    }
}
