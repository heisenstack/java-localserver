package src;

import config.Config;
import config.ConfigLoader;
import server.Server;

public class Main {
    public static void main(String[] args) throws Exception {
        Config config = ConfigLoader.load("config.json");

        for (int port : config.ports) {
            Server server = new Server(config, port);
            server.start();
        }
    }
}
