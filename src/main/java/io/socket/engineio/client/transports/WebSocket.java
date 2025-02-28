package io.socket.engineio.client.transports;


import io.socket.engineio.client.Transport;
import io.socket.engineio.parser.Packet;
import io.socket.engineio.parser.Parser;
import io.socket.parseqs.ParseQS;
import io.socket.thread.EventThread;
import io.socket.yeast.Yeast;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocketListener;
import okio.ByteString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;


public class WebSocket extends Transport {

    public static final String NAME = "websocket";

    private static final Logger logger = Logger.getLogger(WebSocket.class.getName());

    private okhttp3.WebSocket ws;

    public WebSocket(Options opts) {
        super(opts);
        this.name = NAME;
    }

    protected void doOpen() {
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (this.extraHeaders != null) {
            headers.putAll(this.extraHeaders);
        }
        this.emit(EVENT_REQUEST_HEADERS, headers);

        final WebSocket self = this;
        Request.Builder builder = new Request.Builder().url(uri());
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String v : entry.getValue()) {
                builder.addHeader(entry.getKey(), v);
            }
        }
        final Request request = builder.build();
        ws = webSocketFactory.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(okhttp3.WebSocket webSocket, Response response) {
                final Map<String, List<String>> headers = response.headers().toMultimap();
                EventThread.exec(() -> {
                    self.emit(EVENT_RESPONSE_HEADERS, headers);
                    self.onOpen();
                });
            }

            @Override
            public void onMessage(okhttp3.WebSocket webSocket, final String text) {
                if (text == null) {
                    return;
                }
                EventThread.exec(() -> self.onData(text));
            }

            @Override
            public void onMessage(okhttp3.WebSocket webSocket, final ByteString bytes) {
                if (bytes == null) {
                    return;
                }
                EventThread.exec(() -> self.onData(bytes.toByteArray()));
            }

            @Override
            public void onClosed(okhttp3.WebSocket webSocket, int code, String reason) {
                EventThread.exec(() -> self.onClose());
            }

            @Override
            public void onFailure(okhttp3.WebSocket webSocket, final Throwable t, Response response) {
                if (!(t instanceof Exception)) {
                    return;
                }
                EventThread.exec(() -> self.onError("websocket error", (Exception) t));
            }
        });
    }

    protected void write(Packet[] packets) {
        final WebSocket self = this;
        this.writable = false;

        final Runnable done = () -> {
            // fake drain
            // defer to next tick to allow Socket to clear writeBuffer
            EventThread.nextTick(() -> {
                self.writable = true;
                self.emit(EVENT_DRAIN);
            });
        };

        final int[] total = new int[]{packets.length};
        for (Packet packet : packets) {
            if (this.readyState != ReadyState.OPENING && this.readyState != ReadyState.OPEN) {
                // Ensure we don't try to send anymore packets if the socket ends up being closed due to an exception
                break;
            }

            Parser.encodePacket(packet, packet1 -> {
                try {
                    if (packet1 instanceof String) {
                        self.ws.send((String) packet1);
                    } else if (packet1 instanceof byte[]) {
                        self.ws.send(ByteString.of((byte[]) packet1));
                    }
                } catch (IllegalStateException e) {
                    logger.fine("websocket closed before we could write");
                }

                if (0 == --total[0]) done.run();
            });
        }
    }

    protected void doClose() {
        if (ws != null) {
            ws.close(1000, "");
            ws = null;
        }
    }

    protected String uri() {
        Map<String, String> query = this.query;
        if (query == null) {
            query = new HashMap<>();
        }
        String schema = this.secure ? "wss" : "ws";
        String port = "";

        if (this.port > 0 && (("wss".equals(schema) && this.port != 443)
                || ("ws".equals(schema) && this.port != 80))) {
            port = ":" + this.port;
        }

        if (this.timestampRequests) {
            query.put(this.timestampParam, Yeast.yeast());
        }

        String derivedQuery = ParseQS.encode(query);
        if (!derivedQuery.isEmpty()) {
            derivedQuery = "?" + derivedQuery;
        }

        boolean ipv6 = this.hostname.contains(":");
        return schema + "://" + (ipv6 ? "[" + this.hostname + "]" : this.hostname) + port + this.path + derivedQuery;
    }
}