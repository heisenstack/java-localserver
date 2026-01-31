package src;

import java.util.*;

public class Config {
    
    private List<Integer> ports;

    
    public Config() {
        this.ports = new ArrayList<>();
    }
    public List<Integer> getPorts() {
        return ports;
    }
    public void setPorts(List<Integer> ports) {
        this.ports = ports;
    }
    
}