package src.connection;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import src.http.HttpResponse;

public class Connection {
    private static final int BUFFER_SIZE = 8192;
    private static final long TIMEOUT_MS = 30000; // 30 seconds

    public Connection(SocketChannel channel) {
    }

    public void read() throws Exception {
    }

    public void write() throws Exception {

    }

}