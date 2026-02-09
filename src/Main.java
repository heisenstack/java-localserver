package src;

import java.io.File;

import src.Config.Route;

public class Main {
    public static void main(String[] args) {
        try {
            Config config = ConfigLoader.load("config.json");
            for (Route r : config.getRoutes()) {
              if (r.isCgi()) {
                  config.setCgiRoot(new File(r.getRoot()).getAbsolutePath());
                  break;
            }
    }
            new Server(config).start();
        } catch (Exception e) {
            System.err.println("[FATAL] Server failed to start");
            e.printStackTrace();
        }
    }
}