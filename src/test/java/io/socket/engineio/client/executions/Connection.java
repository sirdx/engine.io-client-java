package io.socket.engineio.client.executions;

import io.socket.emitter.Emitter;
import io.socket.engineio.client.Socket;
import okhttp3.OkHttpClient;

import java.net.URISyntaxException;

public class Connection {

    public static void main(String[] args) throws URISyntaxException {
        final OkHttpClient client = new OkHttpClient();
        Socket.Options opts = new Socket.Options();
        opts.webSocketFactory = client;
        opts.callFactory = client;

        final Socket socket = new Socket("http://localhost:" + System.getenv("PORT"), opts);
        socket.on(Socket.EVENT_OPEN, args1 -> {
            System.out.println("open");
            socket.close();
        });
        socket.on(Socket.EVENT_CLOSE, args2 -> client.dispatcher().executorService().shutdown());
        socket.open();
    }
}
