package src;

import java.io.File;

import src.Config.Route;

public class Main {
    public static void main(String[] args) {
        try {
            Config config = ConfigLoader.load("config.json"); 
            new Server(config).start();
        } catch (Exception e) {
            System.err.println("[FATAL] Server failed to start");
            e.printStackTrace();
        }
    }
}