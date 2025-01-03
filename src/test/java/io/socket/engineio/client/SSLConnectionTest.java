package io.socket.engineio.client;

import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class SSLConnectionTest extends Connection {

    private static OkHttpClient sOkHttpClient;

    private Socket socket;

    static {
        try {
            prepareOkHttpClient();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void prepareOkHttpClient() throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance("JKS");
        File file = new File("src/test/resources/keystore.jks");
        ks.load(Files.newInputStream(file.toPath()), "password".toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, "password".toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        sOkHttpClient = new OkHttpClient.Builder()
                .hostnameVerifier((hostname, sslSession) -> hostname.equals("localhost"))
                .sslSocketFactory(sslContext.getSocketFactory(),
                        (X509TrustManager) tmf.getTrustManagers()[0])
                .build();
    }

    @After
    public void tearDown() {
        Socket.setDefaultOkHttpCallFactory(null);
        Socket.setDefaultOkHttpWebSocketFactory(null);
    }

    @Override
    Socket.Options createOptions() {
        Socket.Options opts = super.createOptions();
        opts.secure = true;
        return opts;
    }

    @Override
    String[] createEnv() {
        return new String[] {"DEBUG=engine*", "PORT=" + PORT, "SSL=1"};
    }

    @Test(timeout = TIMEOUT)
    public void connect() throws Exception {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        Socket.Options opts = createOptions();
        opts.callFactory = sOkHttpClient;
        opts.webSocketFactory = sOkHttpClient;
        socket = new Socket(opts);
        socket.on(Socket.EVENT_OPEN, args -> socket.on(Socket.EVENT_MESSAGE, args1 -> values.offer(args1[0])));
        socket.open();

        assertThat((String)values.take(), is("hi"));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void upgrade() throws Exception {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        Socket.Options opts = createOptions();
        opts.callFactory = sOkHttpClient;
        opts.webSocketFactory = sOkHttpClient;
        socket = new Socket(opts);
        socket.on(Socket.EVENT_OPEN, args -> socket.on(Socket.EVENT_UPGRADE, args1 -> {
            socket.send("hi");
            socket.on(Socket.EVENT_MESSAGE, args2 -> values.offer(args2[0]));
        }));
        socket.open();

        assertThat((String)values.take(), is("hi"));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void defaultSSLContext() throws Exception {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        Socket.setDefaultOkHttpWebSocketFactory(sOkHttpClient);
        Socket.setDefaultOkHttpCallFactory(sOkHttpClient);
        socket = new Socket(createOptions());
        socket.on(Socket.EVENT_OPEN, args -> socket.on(Socket.EVENT_MESSAGE, args1 -> values.offer(args1[0])));
        socket.open();

        assertThat((String)values.take(), is("hi"));
        socket.close();
    }
}
