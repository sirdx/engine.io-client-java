package io.socket.engineio.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class BinaryWSTest extends Connection {

    private Socket socket;

    @Test(timeout = TIMEOUT)
    public void receiveBinaryData() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        final byte[] binaryData = new byte[5];
        for (int i = 0; i < binaryData.length; i++) {
            binaryData[i] = (byte)i;
        }
        Socket.Options opts = new Socket.Options();
        opts.port = PORT;
        socket = new Socket(opts);
        socket.on(Socket.EVENT_OPEN, args -> socket.on(Socket.EVENT_UPGRADE, args2 -> {
            socket.send(binaryData);
            socket.on(Socket.EVENT_MESSAGE, args1 -> {
                if (args1[0] instanceof String) return;
                values.offer(args1[0]);
            });
        }));
        socket.open();

        assertThat((byte[])values.take(), is(binaryData));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void receiveBinaryDataAndMultibyteUTF8String() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        final byte[] binaryData = new byte[5];
        for (int i = 0; i < binaryData.length; i++) {
            binaryData[i] = (byte)i;
        }

        final int[] msg = new int[] {0};
        Socket.Options opts = new Socket.Options();
        opts.port = PORT;
        socket = new Socket(opts);
        socket.on(Socket.EVENT_OPEN, args -> socket.on(Socket.EVENT_UPGRADE, args1 -> {
            socket.send(binaryData);
            socket.send("cash money €€€");
            socket.on(Socket.EVENT_MESSAGE, args2 -> {
                if ("hi".equals(args2[0])) return;

                values.offer(args2[0]);
                msg[0]++;
            });
        }));
        socket.open();

        assertThat((byte[])values.take(), is(binaryData));
        assertThat((String)values.take(), is("cash money €€€"));
        socket.close();
    }
}
