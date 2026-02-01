package src;

import src.Config;

import java.io.*;

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

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream is = process.getInputStream()) {
                byte[] buffer = new byte[1024];
                int r;
                while ((r = is.read(buffer)) != -1) {
                    out.write(buffer, 0, r);
                }
            }

            process.waitFor();

            HttpResponse res = new HttpResponse(200, "OK");
            res.addHeader("Content-Type", "text/html; charset=UTF-8");
            res.setBody(out.toByteArray());
            return res;

        } catch (Exception e) {
            return HttpResponse.internalError("CGI Error: " + e.getMessage());
        }
    }
}