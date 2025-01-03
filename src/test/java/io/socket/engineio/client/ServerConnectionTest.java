package io.socket.engineio.client;

import io.socket.engineio.client.transports.Polling;
import io.socket.engineio.client.transports.WebSocket;
import io.socket.thread.EventThread;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ServerConnectionTest extends Connection {

    private Socket socket;

    @Test(timeout = TIMEOUT)
    public void openAndClose() throws InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<>();

        socket = new Socket(createOptions());
        socket.on(Socket.EVENT_OPEN, args -> events.offer("onopen")).on(Socket.EVENT_CLOSE, args -> events.offer("onclose"));
        socket.open();

        assertThat(events.take(), is("onopen"));
        socket.close();
        assertThat(events.take(), is("onclose"));
    }

    @Test(timeout = TIMEOUT)
    public void messages() throws InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        socket = new Socket(createOptions());
        socket.on(Socket.EVENT_OPEN, args -> socket.send("hello")).on(Socket.EVENT_MESSAGE, args -> events.offer((String) args[0]));
        socket.open();

        assertThat(events.take(), is("hi"));
        assertThat(events.take(), is("hello"));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void handshake() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        socket = new Socket(createOptions());
        socket.on(Socket.EVENT_HANDSHAKE, args -> values.offer(args));
        socket.open();

        Object[] args = (Object[])values.take();
        assertThat(args.length, is(1));
        HandshakeData data = (HandshakeData)args[0];
        assertThat(data.sid, is(notNullValue()));
        assertThat(data.upgrades, is(not(emptyArray())));
        assertThat(data.pingTimeout, is(greaterThan((long) 0)));
        assertThat(data.pingInterval, is(greaterThan((long) 0)));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void upgrade() throws InterruptedException {
        final BlockingQueue<Object[]> events = new LinkedBlockingQueue<>();

        socket = new Socket(createOptions());
        socket.on(Socket.EVENT_UPGRADING, args -> events.offer(args));
        socket.on(Socket.EVENT_UPGRADE, args -> events.offer(args));
        socket.open();

        Object[] args1 = events.take();
        assertThat(args1.length, is(1));
        assertThat(args1[0], is(instanceOf(Transport.class)));
        Transport transport1 = (Transport)args1[0];
        assertThat(transport1, is(notNullValue()));

        Object[] args2 = events.take();
        assertThat(args2.length, is(1));
        assertThat(args2[0], is(instanceOf(Transport.class)));
        Transport transport2 = (Transport)args2[0];
        assertThat(transport2, is(notNullValue()));

        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void pollingHeaders() throws InterruptedException {
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

        Socket.Options opts = createOptions();
        opts.transports = new String[] {Polling.NAME};

        socket = new Socket(opts);
        socket.on(Socket.EVENT_TRANSPORT, args -> {
            Transport transport = (Transport)args[0];
            transport.on(Transport.EVENT_REQUEST_HEADERS, args1 -> {
                @SuppressWarnings("unchecked")
                Map<String, List<String>> headers = (Map<String, List<String>>) args1[0];
                headers.put("X-EngineIO", singletonList("foo"));
            }).on(Transport.EVENT_RESPONSE_HEADERS, args2 -> {
                @SuppressWarnings("unchecked")
                Map<String, List<String>> headers = (Map<String, List<String>>) args2[0];
                List<String> values = headers.get("X-EngineIO");
                messages.offer(values.get(0));
                messages.offer(values.get(1));
            });
        });
        socket.open();

        assertThat(messages.take(), is("hi"));
        assertThat(messages.take(), is("foo"));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void pollingHeaders_withExtraHeadersOption() throws InterruptedException {
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

        Socket.Options opts = createOptions();
        opts.transports = new String[] {Polling.NAME};
        opts.extraHeaders = singletonMap("X-EngineIO", singletonList("bar"));

        socket = new Socket(opts);
        socket.on(Socket.EVENT_TRANSPORT, args -> {
            Transport transport = (Transport)args[0];
            transport.on(Transport.EVENT_RESPONSE_HEADERS, args1 -> {
                @SuppressWarnings("unchecked")
                Map<String, List<String>> headers = (Map<String, List<String>>) args1[0];
                List<String> values = headers.get("X-EngineIO");
                messages.offer(values.get(0));
                messages.offer(values.get(1));
            });
        });
        socket.open();

        assertThat(messages.take(), is("hi"));
        assertThat(messages.take(), is("bar"));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void websocketHandshakeHeaders() throws InterruptedException {
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

        Socket.Options opts = createOptions();
        opts.transports = new String[] {WebSocket.NAME};

        socket = new Socket(opts);
        socket.on(Socket.EVENT_TRANSPORT, args -> {
            Transport transport = (Transport)args[0];
            transport.on(Transport.EVENT_REQUEST_HEADERS, args1 -> {
                @SuppressWarnings("unchecked")
                Map<String, List<String>> headers = (Map<String, List<String>>) args1[0];
                headers.put("X-EngineIO", singletonList("foo"));
            }).on(Transport.EVENT_RESPONSE_HEADERS, args2 -> {
                @SuppressWarnings("unchecked")
                Map<String, List<String>> headers = (Map<String, List<String>>) args2[0];
                List<String> values = headers.get("X-EngineIO");
                messages.offer(values.get(0));
                messages.offer(values.get(1));
            });
        });
        socket.open();

        assertThat(messages.take(), is("hi"));
        assertThat(messages.take(), is("foo"));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void websocketHandshakeHeaders_withExtraHeadersOption() throws InterruptedException {
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

        Socket.Options opts = createOptions();
        opts.transports = new String[] {WebSocket.NAME};
        opts.extraHeaders = singletonMap("X-EngineIO", singletonList("bar"));

        socket = new Socket(opts);
        socket.on(Socket.EVENT_TRANSPORT, args -> {
            Transport transport = (Transport)args[0];
            transport.on(Transport.EVENT_RESPONSE_HEADERS, args1 -> {
                @SuppressWarnings("unchecked")
                Map<String, List<String>> headers = (Map<String, List<String>>) args1[0];
                List<String> values = headers.get("X-EngineIO");
                messages.offer(values.get(0));
                messages.offer(values.get(1));
            });
        });
        socket.open();

        assertThat(messages.take(), is("hi"));
        assertThat(messages.take(), is("bar"));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void rememberWebsocket() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        EventThread.exec(() -> {
            final Socket socket = new Socket(createOptions());

            socket.on(Socket.EVENT_UPGRADE, args -> {
                Transport transport = (Transport) args[0];
                socket.close();
                if (WebSocket.NAME.equals(transport.name)) {
                    Socket.Options opts = new Socket.Options();
                    opts.port = PORT;
                    opts.rememberUpgrade = true;

                    Socket socket2 = new Socket(opts);
                    socket2.open();
                    values.offer(socket2.transport.name);
                    socket2.close();
                }
            });
            socket.open();
            values.offer(socket.transport.name);
        });

        assertThat((String)values.take(), is(Polling.NAME));
        assertThat((String)values.take(), is(WebSocket.NAME));
    }

    @Test(timeout = TIMEOUT)
    public void notRememberWebsocket() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        EventThread.exec(() -> {
            final Socket socket = new Socket(createOptions());

            socket.on(Socket.EVENT_UPGRADE, args -> {
                Transport transport = (Transport)args[0];
                socket.close();
                if (WebSocket.NAME.equals(transport.name)) {
                    Socket.Options opts = new Socket.Options();
                    opts.port = PORT;
                    opts.rememberUpgrade = false;

                    final Socket socket2 = new Socket(opts);
                    socket2.open();
                    values.offer(socket2.transport.name);
                    socket2.close();
                }
            });
            socket.open();
            values.offer(socket.transport.name);
        });

        assertThat((String) values.take(), is(Polling.NAME));
        assertThat((String)values.take(), is(not(WebSocket.NAME)));
    }
}
