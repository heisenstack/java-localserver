package src.connection;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import src.http.HttpResponse;
import src.Config;

public class Connection {

    private static final int INITIAL_BUFFER_SIZE = 8192;
    private static final int MAX_BUFFER_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long TIMEOUT_MS = 30000;
    private static final int TEMP_FILE_THRESHOLD = 1 * 1024 * 1024; // 1MB

    private final SocketChannel channel;
    private final Config config;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;

    private long lastActivityAt;
    private boolean requestComplete = false;
    private boolean writeComplete = false;

    private int headerEndPosition = -1;
    private long expectedContentLength = -1; 
    private boolean isChunked = false;

    private File tempBodyFile;
    private FileOutputStream tempBodyOut;

    public Connection(SocketChannel channel, Config config) {
        this.channel = channel;
        this.config = config;
        this.readBuffer = ByteBuffer.allocate(INITIAL_BUFFER_SIZE);
        this.lastActivityAt = System.currentTimeMillis();
    }
    
    public Config getConfig() { 
        return config; 
    }

    public void read() throws IOException {
        if (!readBuffer.hasRemaining()) expandBuffer();

        int bytesRead = channel.read(readBuffer);
        if (bytesRead == -1) throw new IOException("Client closed connection");

        if (bytesRead > 0) lastActivityAt = System.currentTimeMillis();

        checkRequestComplete();
    }

    private void expandBuffer() throws IOException {
        int newCapacity = readBuffer.capacity() * 2;
        if (newCapacity > MAX_BUFFER_SIZE) throw new IOException("Request too large");

        ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
        readBuffer.flip();
        newBuffer.put(readBuffer);
        readBuffer = newBuffer;
    }

    private void checkRequestComplete() throws IOException {
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
                expectedContentLength = -2;
            } else {
                expectedContentLength = extractContentLength(headers);
            }

            if (expectedContentLength > TEMP_FILE_THRESHOLD) {
                tempBodyFile = File.createTempFile("http_body_", ".tmp");
                tempBodyFile.deleteOnExit();
                tempBodyOut = new FileOutputStream(tempBodyFile);
            }
        }

        if (headerEndPosition == -1) return;

        if (isChunked) {
            if (dataStr.contains("\r\n0\r\n\r\n")) {
                requestComplete = true;
                closeTempFile();
            }
            return;
        }

        long bodyStart = headerEndPosition;
        long bodyLength = data.length - bodyStart;

        if (bodyLength > 0) {
            if (tempBodyOut != null) {
                tempBodyOut.write(data, (int) bodyStart, (int) bodyLength);
            }
        }

        if (expectedContentLength >= 0 && bodyLength >= expectedContentLength) {
            requestComplete = true;
            closeTempFile();
        }

        if (expectedContentLength == 0) {
            requestComplete = true;
        }
    }

    private void closeTempFile() throws IOException {
        if (tempBodyOut != null) {
            tempBodyOut.close();
            tempBodyOut = null;
        }
    }

    private long extractContentLength(String headers) {
        String[] lines = headers.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("content-length:")) {
                try { return Long.parseLong(line.substring(15).trim()); }
                catch (Exception e) { return -1; }
            }
        }
        return 0;
    }

    public File getTempBodyFile() { return tempBodyFile; }

    public void setResponse(HttpResponse response) { this.writeBuffer = response.toByteBuffer(); }

    public void write() throws IOException {
        if (writeBuffer == null) throw new IOException("No response to write");

        channel.write(writeBuffer);
        if (!writeBuffer.hasRemaining()) writeComplete = true;
    }

    public boolean isRequestComplete() { return requestComplete; }
    public boolean isWriteComplete() { return writeComplete; }
    public boolean isTimedOut(long now) { return (now - lastActivityAt) > TIMEOUT_MS; }
    public SocketChannel getChannel() { return channel; }
    public boolean isContentLengthTooLarge() { return expectedContentLength > MAX_BUFFER_SIZE; }
    public long getContentLength() { return expectedContentLength; }

    public ByteBuffer getBuffer() throws IOException {
        if (tempBodyFile != null) {
            throw new IOException("Body stored in temp file, not in RAM");
        }
        int currentPos = readBuffer.position();
        readBuffer.flip();
        byte[] data = new byte[readBuffer.remaining()];
        readBuffer.get(data);
        readBuffer.clear();
        readBuffer.position(currentPos);
        return ByteBuffer.wrap(data);
    }
}
