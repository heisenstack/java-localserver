package src;

import src.http.HttpRequest;

public class TestCGI {
    public static void main(String[] args) {
        Config config = new Config();
        config.setCgiRoot("./cgi-bin");

        HttpRequest req = new HttpRequest();
        req.setMethod("GET");
        req.setPath("/hello.sh");

        System.out.println(
            new String(
                CGIHandler.handle(req, config).toByteBuffer().array()
            )
        );
    }
}
