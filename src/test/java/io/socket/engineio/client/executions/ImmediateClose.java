package io.socket.engineio.client.executions;

import io.socket.engineio.client.Socket;
import okhttp3.OkHttpClient;

import java.net.URISyntaxException;

public class ImmediateClose {

    public static void main(String[] args) throws URISyntaxException {
        final OkHttpClient client = new OkHttpClient();
        Socket.Options opts = new Socket.Options();
        opts.webSocketFactory = client;
        opts.callFactory = client;

        final Socket socket = new Socket("http://localhost:" + System.getenv("PORT"), opts);
        socket.on(Socket.EVENT_OPEN, args1 -> System.out.println("open")).on(Socket.EVENT_CLOSE, args2 -> {
            System.out.println("close");
            client.dispatcher().executorService().shutdown();
        });
        socket.open();
        socket.close();
    }
}
