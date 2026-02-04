package src.connection;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import src.http.HttpResponse;

public class Connection {
    private final SocketChannel channel;
    private final ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    private final long createdAt;
    private boolean requestComplete = false;
    
    private static final int BUFFER_SIZE = 8192;
    private static final long TIMEOUT_MS = 30000; // 30 seconds

    public Connection(SocketChannel channel) {
        this.channel = channel;
        this.readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.createdAt = System.currentTimeMillis();
    }

    public void read() throws Exception {
        int bytesRead = channel.read(readBuffer);
        
        if (bytesRead == -1) {
            throw new Exception("Client closed connection");
        }
        
        String request = new String(readBuffer.array(), 0, readBuffer.position());
        if (request.contains("\r\n\r\n")) {
            requestComplete = true;
        }
    }

    public void write() throws Exception {
        if (writeBuffer != null && writeBuffer.hasRemaining()) {
            channel.write(writeBuffer);
        }
    }

    public boolean isRequestComplete() {
        return requestComplete;
    }

    public boolean isWriteComplete() {
        return writeBuffer != null && !writeBuffer.hasRemaining();
    }

    public ByteBuffer getBuffer() {
        readBuffer.flip();
        return readBuffer;
    }

    public void setResponse(HttpResponse response) {
        this.writeBuffer = response.toByteBuffer();
    }

    public boolean isTimedOut(long currentTime) {
        return (currentTime - createdAt) > TIMEOUT_MS;
    }
}