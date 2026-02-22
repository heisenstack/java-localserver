// package src;

// import src.http.HttpRequest;
// import src.http.HttpResponse;

// import java.io.File;
// import java.nio.charset.StandardCharsets;

// public class TestCGI {
//     public static void main(String[] args) {
//         Config config = new Config();
//         config.setCgiRoot(new File("./cgi-bin").getAbsolutePath());

//         HttpRequest getReq = new HttpRequest();
//         getReq.setMethod("GET");
//         getReq.setPath("/hello.sh"); // request path

//         HttpResponse getRes = CgiProcess.handle(getReq, config);
//         System.out.println("===== GET Request =====");
//         System.out.println(new String(getRes.toByteBuffer().array(), StandardCharsets.UTF_8));

//         HttpRequest postReq = new HttpRequest();
//         postReq.setMethod("POST");
//         postReq.setPath("/hello.sh");
//         postReq.setBody("name=John&age=25".getBytes(StandardCharsets.UTF_8));

//         HttpResponse postRes = CgiProcess.handle(postReq, config);
//         System.out.println("===== POST Request =====");
//         System.out.println(new String(postRes.toByteBuffer().array(), StandardCharsets.UTF_8));
//     }
// }
