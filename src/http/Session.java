package src.http;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Session {
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes
    
    private final String id;
    private final long createdAt;
    private long lastAccessedAt;
    private final Map<String, Object> attributes;
    
    private Session(String id) {
        this.id = id;
        this.createdAt = System.currentTimeMillis();
        this.lastAccessedAt = createdAt;
        this.attributes = new HashMap<>();
    }
    

    public static Session createSession() {
        String id = generateSessionId();
        Session session = new Session(id);
        sessions.put(id, session);
        System.out.println("[SESSION] Created: " + id);
        return session;
    }
    

    public static Session getSession(String id) {
        if (id == null) return null;
        
        Session session = sessions.get(id);
        if (session == null) return null;
        
        if (session.isExpired()) {
            sessions.remove(id);
            System.out.println("[SESSION] Expired: " + id);
            return null;
        }
        
        session.lastAccessedAt = System.currentTimeMillis();
        return session;
    }
    

    public static void destroySession(String id) {
        sessions.remove(id);
        System.out.println("[SESSION] Destroyed: " + id);
    }
    

    private static String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
    

    private boolean isExpired() {
        return (System.currentTimeMillis() - lastAccessedAt) > SESSION_TIMEOUT;
    }
    

    public String getId() {
        return id;
    }
    
 
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
    

    public void removeAttribute(String key) {
        attributes.remove(key);
    }
    

    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }
    

    public static void cleanupExpiredSessions() {
        // long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                System.out.println("[SESSION] Cleaned up expired: " + entry.getKey());
                return true;
            }
            return false;
        });
    }

public static int getSessionCount() {
    return sessions.size();
}
}