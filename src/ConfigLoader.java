package src;

public class ConfigLoader {
    public static Config load(String configPath) throws Exception {
        String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(configPath)));
        System.out.println("config: " + content);
        Config config = new Config();
        return config;
    }

}