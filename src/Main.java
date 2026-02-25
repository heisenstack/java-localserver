package src;

import java.io.File;
import java.util.List;

import src.Config.Route;

public class Main {

    public static void main(String[] args) {
        try {
            
            List<Config> configs = ConfigLoader.load("config.json");

            for (Config config : configs) {
                for (Route r : config.getRoutes()) {
                    if (r.isCgi()) {
                        config.setCgiRoot(new File(r.getRoot()).getAbsolutePath());
                        break;
                    }
                }
            }

            Server server = new Server(configs);
            server.start();

        } catch (Exception e) {
            System.err.println("[FATAL] Server failed to start");
            e.printStackTrace();
        }
    }
}