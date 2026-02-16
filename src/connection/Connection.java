package src.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import src.http.HttpResponse;

public class Connection {
    private static final int INITIAL_BUFFER_SIZE = 8192;
    private static final int MAX_BUFFER_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long TIMEOUT_MS = 30000; // 30 seconds
    
    private final SocketChannel channel;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    private final long createdAt;
    private long lastActivityAt;
    private boolean requestComplete = false;
    private boolean writeComplete = false;
    
    private long expectedContentLength = -1;
    private int headerEndPosition = -1;
    
    public Connection(SocketChannel channel) {
        this.channel = channel;
        this.readBuffer = ByteBuffer.allocate(INITIAL_BUFFER_SIZE);
        this.createdAt = System.currentTimeMillis();
        this.lastActivityAt = createdAt;
    }
    
    public void read() throws IOException {
        if (!readBuffer.hasRemaining()) {
            expandBuffer();
        }
        
        int bytesRead = channel.read(readBuffer);
        
        if (bytesRead == -1) {
            throw new IOException("Client closed connection");
        }
        
        if (bytesRead > 0) {
            lastActivityAt = System.currentTimeMillis();
            System.out.println("[DEBUG] Read " + bytesRead + " bytes, total: " + readBuffer.position());
        }
        
        checkRequestComplete();
    }
    
    private void expandBuffer() throws IOException {
        int newCapacity = readBuffer.capacity() * 2;
        if (newCapacity > MAX_BUFFER_SIZE) {
            throw new IOException("Request too large (exceeds " + MAX_BUFFER_SIZE + " bytes)");
        }
        
        System.out.println("[DEBUG] Expanding buffer from " + readBuffer.capacity() + " to " + newCapacity);
        
        ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
        readBuffer.flip();
        newBuffer.put(readBuffer);
        readBuffer = newBuffer;
    }
    
    private void checkRequestComplete() {
    int currentPos = readBuffer.position();
    readBuffer.flip();
    byte[] data = new byte[readBuffer.remaining()];
    readBuffer.get(data);
    readBuffer.clear();
    readBuffer.position(currentPos);
    
    String dataStr = new String(data);
    
    if (headerEndPosition == -1) {
        int headerEnd = dataStr.indexOf("\r\n\r\n");
        if (headerEnd == -1) {
            return;
        }
        
        headerEndPosition = headerEnd + 4;
        String headers = dataStr.substring(0, headerEnd);
        expectedContentLength = extractContentLength(headers);
        
        System.out.println("[DEBUG] Headers complete. Content-Length: " + expectedContentLength);
    }
    
    long currentBodyLength = data.length - headerEndPosition;  
    
    if (expectedContentLength == 0) {
        requestComplete = true;
        System.out.println("[DEBUG] Request complete (no body)");
    } else if (expectedContentLength > 0) {
        
        if (expectedContentLength > MAX_BUFFER_SIZE) {
            System.err.println("[ERROR] Content-Length " + expectedContentLength + 
                              " exceeds maximum buffer size " + MAX_BUFFER_SIZE);
            requestComplete = true; 
            return;
        }
        
        if (currentBodyLength >= expectedContentLength) {
            requestComplete = true;
            System.out.println("[DEBUG] Request complete. Body: " + currentBodyLength + "/" + expectedContentLength + " bytes");
        } else {
            System.out.println("[DEBUG] Waiting for body: " + currentBodyLength + "/" + expectedContentLength + " bytes");
        }
    } else {
        requestComplete = true;
        System.out.println("[DEBUG] Request complete (no Content-Length header)");
    }
}
    
private long extractContentLength(String headers) {  
    String[] lines = headers.split("\r\n");
    for (String line : lines) {
        if (line.toLowerCase().startsWith("content-length:")) {
            String value = line.substring("content-length:".length()).trim();
            try {
                long length = Long.parseLong(value);
                System.out.println("[DEBUG] Found Content-Length: " + length);
                
                if (length < 0) {
                    System.err.println("[ERROR] Invalid Content-Length (negative): " + length);
                    return -1;
                }
                
                return length;  
            } catch (NumberFormatException e) {
                System.err.println("[ERROR] Invalid Content-Length: " + value);
                return -1;
            }
        }
    }
    return 0; 
}
    
    public ByteBuffer getBuffer() {
        int currentPos = readBuffer.position();
        readBuffer.flip();
        byte[] data = new byte[readBuffer.remaining()];
        readBuffer.get(data);
        readBuffer.clear();
        readBuffer.position(currentPos);
        return ByteBuffer.wrap(data);
    }
    
    public void setResponse(HttpResponse response) {
        this.writeBuffer = response.toByteBuffer();
    }
    
    public void write() throws IOException {
    if (writeBuffer == null) {
        throw new IOException("No response to write");
    }
    
    if (writeBuffer.position() == 0) {
        int limit = Math.min(writeBuffer.remaining(), 500);
        byte[] preview = new byte[limit];
        writeBuffer.mark();
        writeBuffer.get(preview);
        writeBuffer.reset();
        System.out.println("[DEBUG] Sending response (first " + limit + " bytes):\n" + new String(preview));
    }
    
    int bytesWritten = channel.write(writeBuffer);
    
    if (bytesWritten > 0) {
        lastActivityAt = System.currentTimeMillis();
    }
    
    if (!writeBuffer.hasRemaining()) {
        writeComplete = true;
    }
}
    
    public boolean isRequestComplete() {
        return requestComplete;
    }
    
    public boolean isWriteComplete() {
        return writeComplete;
    }
    
    public boolean isTimedOut(long now) {
        return (now - lastActivityAt) > TIMEOUT_MS;
    }
    
    public SocketChannel getChannel() {
        return channel;
    }
    public boolean isContentLengthTooLarge() {
    return expectedContentLength > MAX_BUFFER_SIZE;
}
}