package src.http;

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

        } catch (Exception e) {
            return HttpResponse.internalError("CGI Error: " + e.getMessage());
        }
    }
}
