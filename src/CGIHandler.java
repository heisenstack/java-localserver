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

            if (request.getBody() != null && request.getBody().length > 0) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(request.getBody());
                    os.flush();
                }
            }

            InputStream is = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

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

            int b;
            while ((b = is.read()) != -1) {
                bodyOut.write(b);
            }

            process.waitFor();

            HttpResponse res = new HttpResponse(200, "OK");

            for (Map.Entry<String, String> h : cgiHeaders.entrySet()) {
                res.addHeader(h.getKey(), h.getValue());
            }

            res.setBody(bodyOut.toByteArray());

            return res;

        } catch (Exception e) {
            return HttpResponse.internalError("CGI Error: " + e.getMessage());
        }
    }
}
