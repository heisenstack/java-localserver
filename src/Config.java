package src;

import java.util.*;

public class Config {
    
    private List<Integer> ports;
    private String host;

    
    public Config() {
        this.ports = new ArrayList<>();
        this.host = "localhost";
    }
    public List<Integer> getPorts() {
        return ports;
    }
    public void setPorts(List<Integer> ports) {
        this.ports = ports;
    }
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
        }
    
}