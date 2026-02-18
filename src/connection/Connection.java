package src.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import src.http.HttpResponse;

public class Connection {

    private static final int INITIAL_BUFFER_SIZE = 8192;
    private static final int MAX_BUFFER_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long TIMEOUT_MS = 30000;

    private final SocketChannel channel;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;

    private long lastActivityAt;
    private boolean requestComplete = false;
    private boolean writeComplete = false;

    private int headerEndPosition = -1;
    private long expectedContentLength = -1; 
    private boolean isChunked = false;

    public Connection(SocketChannel channel) {
        this.channel = channel;
        this.readBuffer = ByteBuffer.allocate(INITIAL_BUFFER_SIZE);
        this.lastActivityAt = System.currentTimeMillis();
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
            throw new IOException("Request too large");
        }

        ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
        readBuffer.flip();
        newBuffer.put(readBuffer);
        readBuffer = newBuffer;

        System.out.println("[DEBUG] Buffer expanded to " + newCapacity);
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
            if (headerEnd == -1) return;

            headerEndPosition = headerEnd + 4;

            String headers = dataStr.substring(0, headerEnd).toLowerCase();

            if (headers.contains("transfer-encoding: chunked")) {
                isChunked = true;
                expectedContentLength = -2; // special flag
                System.out.println("[DEBUG] Chunked request detected");
            } else {
                expectedContentLength = extractContentLength(headers);
                System.out.println("[DEBUG] Content-Length: " + expectedContentLength);
            }
        }

        if (headerEndPosition == -1) return;

        // ===== HANDLE CHUNKED =====
        if (isChunked) {
            if (dataStr.contains("\r\n0\r\n\r\n")) {
                requestComplete = true;
                System.out.println("[DEBUG] Chunked request complete");
            } else {
                System.out.println("[DEBUG] Waiting for chunked body...");
            }
            return;
        }

        // ===== HANDLE NORMAL =====
        if (expectedContentLength == 0) {
            requestComplete = true;
            System.out.println("[DEBUG] No body request complete");
            return;
        }

        long currentBodyLength = data.length - headerEndPosition;

        if (expectedContentLength > 0) {
            if (currentBodyLength >= expectedContentLength) {
                requestComplete = true;
                System.out.println("[DEBUG] Request complete (Content-Length)");
            } else {
                System.out.println("[DEBUG] Waiting body: " +
                        currentBodyLength + "/" + expectedContentLength);
            }
        }
    }

    private long extractContentLength(String headers) {
        String[] lines = headers.split("\r\n");

        for (String line : lines) {
            if (line.startsWith("content-length:")) {
                try {
                    return Long.parseLong(line.substring(15).trim());
                } catch (Exception e) {
                    return -1;
                }
            }
        }

        return 0;
    }

    public void setResponse(HttpResponse response) {
        this.writeBuffer = response.toByteBuffer();
    }

    public void write() throws IOException {
        if (writeBuffer == null) {
            throw new IOException("No response to write");
        }

        int bytesWritten = channel.write(writeBuffer);
        
        if (bytesWritten > 0) {
            lastActivityAt = System.currentTimeMillis();
            System.out.println("[DEBUG] Wrote " + bytesWritten + " bytes, remaining: " + writeBuffer.remaining());
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

    public ByteBuffer getBuffer() {
        int currentPos = readBuffer.position();
        readBuffer.flip();

        byte[] data = new byte[readBuffer.remaining()];
        readBuffer.get(data);

        readBuffer.clear();
        readBuffer.position(currentPos);

        return ByteBuffer.wrap(data);
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public boolean isContentLengthTooLarge() {
        return expectedContentLength > MAX_BUFFER_SIZE;
    }
}
