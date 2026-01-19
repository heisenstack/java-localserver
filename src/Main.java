package src;

import java.nio.channels.*;
import java.net.InetSocketAddress;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            Config config = ConfigLoader.load("config.json");
            
            Server server = new Server(config);
            server.start();
            
            System.out.println("Server started successfully");
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}