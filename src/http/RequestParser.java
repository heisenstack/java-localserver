// package src.http;

// import java.nio.ByteBuffer;
// import java.nio.charset.StandardCharsets;

// public class RequestParser {

//     public static HttpRequest parse(ByteBuffer buffer) {
//         String raw = new String(buffer.array(), StandardCharsets.UTF_8);
//         String[] parts = raw.split("\r\n\r\n", 2);

//         String[] lines = parts[0].split("\r\n");
//         String[] start = lines[0].split(" ");

//         HttpRequest req = new HttpRequest();
//         req.setMethod(start[0]);
//         req.setPath(start[1]);
//         req.setVersion(start[2]);

//         for (int i = 1; i < lines.length; i++) {
//             String[] h = lines[i].split(":", 2);
//             req.addHeader(h[0], h[1]);
//         }

//         if (parts.length == 2)
//             req.setBody(parts[1].getBytes());

//         parseCookies(req);
//         return req;
//     }

//     private static void parseCookies(HttpRequest req) {
//         String cookie = req.getHeaders().get("cookie");
//         if (cookie == null) return;

//         for (String c : cookie.split(";")) {
//             String[] kv = c.trim().split("=", 2);
//             if (kv.length == 2)
//                 req.addCookie(kv[0], kv[1]);
//         }
//     }
// }
