package io.socket.engineio.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ConnectionTest extends Connection {

    private Socket socket;

    @Test(timeout = TIMEOUT)
    public void connectToLocalhost() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        socket = new Socket(createOptions());
        socket.on(Socket.EVENT_OPEN, args -> socket.on(Socket.EVENT_MESSAGE, args1 -> {
            values.offer(args1[0]);
            socket.close();
        }));
        socket.open();

        assertThat((String)values.take(), is("hi"));
    }

    @Test(timeout = TIMEOUT)
    public void receiveMultibyteUTF8StringsWithPolling() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        socket = new Socket(createOptions());
        socket.on(Socket.EVENT_OPEN, args -> {
            socket.send("cash money €€€");
            socket.on(Socket.EVENT_MESSAGE, args1 -> {
                if ("hi".equals(args1[0])) return;
                values.offer(args1[0]);
                socket.close();
            });
        });
        socket.open();

        assertThat((String)values.take(), is("cash money €€€"));
    }

    @Test(timeout = TIMEOUT)
    public void receiveEmoji() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        socket = new Socket(createOptions());
        socket.on(Socket.EVENT_OPEN, args -> {
            socket.send("\uD800\uDC00-\uDB7F\uDFFF\uDB80\uDC00-\uDBFF\uDFFF\uE000-\uF8FF");
            socket.on(Socket.EVENT_MESSAGE, args1 -> {
                if ("hi".equals(args1[0])) return;
                values.offer(args1[0]);
                socket.close();
            });
        });
        socket.open();

        assertThat((String)values.take(), is("\uD800\uDC00-\uDB7F\uDFFF\uDB80\uDC00-\uDBFF\uDFFF\uE000-\uF8FF"));
    }

    @Test(timeout = TIMEOUT)
    public void notSendPacketsIfSocketCloses() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        socket = new Socket(createOptions());
        socket.on(Socket.EVENT_OPEN, args -> {
            final boolean[] noPacket = new boolean[] {true};
            socket.on(Socket.EVENT_PACKET_CREATE, args1 -> noPacket[0] = false);
            socket.close();
            socket.send("hi");
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    values.offer(noPacket[0]);
                }
            }, 1200);

        });
        socket.open();
        assertThat((Boolean)values.take(), is(true));
    }

    @Test(timeout = TIMEOUT)
    public void deferCloseWhenUpgrading() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        socket = new Socket(createOptions());
        socket.on(Socket.EVENT_OPEN, args -> {
            final boolean[] upgraded = new boolean[] {false};
            socket.on(Socket.EVENT_UPGRADE, args1 -> upgraded[0] = true).on(Socket.EVENT_UPGRADING, args2 -> {
                socket.on(Socket.EVENT_CLOSE, args3 -> values.offer(upgraded[0]));
                socket.close();
            });
        });
        socket.open();
        assertThat((Boolean)values.take(), is(true));
    }

    @Test(timeout = TIMEOUT)
    public void closeOnUpgradeErrorIfClosingIsDeferred() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        socket = new Socket(createOptions());
        socket.on(Socket.EVENT_OPEN, args -> {
            final boolean[] upgradError = new boolean[] {false};
            socket.on(Socket.EVENT_UPGRADE_ERROR, args3 -> upgradError[0] = true).on(Socket.EVENT_UPGRADING, args2 -> {
                socket.on(Socket.EVENT_CLOSE, args1 -> values.offer(upgradError[0]));
                socket.close();
                socket.transport.onError("upgrade error", new Exception());
            });
        });
        socket.open();
        assertThat((Boolean) values.take(), is(true));
    }

    public void notSendPacketsIfClosingIsDeferred() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        socket = new Socket(createOptions());
        socket.on(Socket.EVENT_OPEN, args -> {
            final boolean[] noPacket = new boolean[] {true};
            socket.on(Socket.EVENT_UPGRADING, args2 -> {
                socket.on(Socket.EVENT_PACKET_CREATE, args1 -> noPacket[0] = false);
                socket.close();
                socket.send("hi");
            });
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    values.offer(noPacket[0]);
                }
            }, 1200);
        });
        socket.open();
        assertThat((Boolean) values.take(), is(true));
    }

    @Test(timeout = TIMEOUT)
    public void sendAllBufferedPacketsIfClosingIsDeferred() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        socket = new Socket(createOptions());
        socket.on(Socket.EVENT_OPEN, args -> socket.on(Socket.EVENT_UPGRADING, args2 -> {
            socket.send("hi");
            socket.close();
        }).on(Socket.EVENT_CLOSE, args1 -> values.offer(socket.writeBuffer.size())));
        socket.open();
        assertThat((Integer) values.take(), is(0));
    }

    @Test(timeout = TIMEOUT)
    public void receivePing() throws InterruptedException {
        final BlockingQueue<String> values = new LinkedBlockingQueue<>();

        socket = new Socket(createOptions());
        socket.on(Socket.EVENT_PING, args -> {
            values.offer("end");
            socket.close();
        });
        socket.open();
        assertThat(values.take(), is("end"));
    }
}
