package io.socket.engineio.client.executions;

import io.socket.engineio.client.Socket;
import okhttp3.OkHttpClient;

import java.net.URISyntaxException;

public class ConnectionFailure {

    public static void main(String[] args) throws URISyntaxException {
        final OkHttpClient client = new OkHttpClient();
        Socket.Options opts = new Socket.Options();
        opts.webSocketFactory = client;
        opts.callFactory = client;

        int port = Integer.parseInt(System.getenv("PORT"));
        port++;
        final Socket socket = new Socket("http://localhost:" + port, opts);
        socket.on(Socket.EVENT_CLOSE, args1 -> System.out.println("close")).on(Socket.EVENT_ERROR, args2 -> {
            System.out.println("error");
            client.dispatcher().executorService().shutdown();
        });
        socket.open();
    }
}
