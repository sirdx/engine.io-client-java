package io.socket.engineio.parser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static io.socket.engineio.parser.Parser.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ParserTest {

    static final String ERROR_DATA = "parser error";

    @Test
    public void encodeAsString() {
        encodePacket(new Packet<>(Packet.MESSAGE, "test"), (EncodeCallback<String>) data -> assertThat(data, isA(String.class)));
    }

    @Test
    public void decodeAsPacket()  {
        encodePacket(new Packet<>(Packet.MESSAGE, "test"), (EncodeCallback<String>) data -> assertThat(decodePacket(data), isA(Packet.class)));
    }

    @Test
    public void noData()  {
        encodePacket(new Packet(Packet.MESSAGE), (EncodeCallback<String>) data -> {
            Packet p = decodePacket(data);
            assertThat(p.type, is(Packet.MESSAGE));
            assertThat(p.data, is(nullValue()));
        });
    }

    @Test
    public void encodeOpenPacket()  {
        encodePacket(new Packet<>(Packet.OPEN, "{\"some\":\"json\"}"), (EncodeCallback<String>) data -> {
            Packet<String> p = decodePacket(data);
            assertThat(p.type, is(Packet.OPEN));
            assertThat(p.data, is("{\"some\":\"json\"}"));
        });
    }

    @Test
    public void encodeClosePacket()  {
        encodePacket(new Packet<String>(Packet.CLOSE), (EncodeCallback<String>) data -> {
            Packet p = decodePacket(data);
            assertThat(p.type, is(Packet.CLOSE));
        });
    }

    @Test
    public void encodePingPacket()  {
        encodePacket(new Packet<>(Packet.PING, "1"), (EncodeCallback<String>) data -> {
            Packet<String> p = decodePacket(data);
            assertThat(p.type, is(Packet.PING));
            assertThat(p.data, is("1"));
        });
    }

    @Test
    public void encodePongPacket()  {
        encodePacket(new Packet<>(Packet.PONG, "1"), (EncodeCallback<String>) data -> {
            Packet<String> p = decodePacket(data);
            assertThat(p.type, is(Packet.PONG));
            assertThat(p.data, is("1"));
        });
    }

    @Test
    public void encodeMessagePacket()  {
        encodePacket(new Packet<>(Packet.MESSAGE, "aaa"), (EncodeCallback<String>) data -> {
            Packet<String> p = decodePacket(data);
            assertThat(p.type, is(Packet.MESSAGE));
            assertThat(p.data, is("aaa"));
        });
    }

    @Test
    public void encodeUTF8SpecialCharsMessagePacket()  {
        encodePacket(new Packet<>(Packet.MESSAGE, "utf8 — string"), (EncodeCallback<String>) data -> {
            Packet<String> p = decodePacket(data);
            assertThat(p.type, is(Packet.MESSAGE));
            assertThat(p.data, is("utf8 — string"));
        });
    }

    @Test
    public void encodeMessagePacketCoercingToString()  {
        encodePacket(new Packet<>(Packet.MESSAGE, 1), (EncodeCallback<String>) data -> {
            Packet<String> p = decodePacket(data);
            assertThat(p.type, is(Packet.MESSAGE));
            assertThat(p.data, is("1"));
        });
    }

    @Test
    public void encodeUpgradePacket()  {
        encodePacket(new Packet<String>(Packet.UPGRADE), (EncodeCallback<String>) data -> {
            Packet p = decodePacket(data);
            assertThat(p.type, is(Packet.UPGRADE));
        });
    }

    @Test
    public void encodingFormat()  {
        encodePacket(new Packet<>(Packet.MESSAGE, "test"), (EncodeCallback<String>) data -> assertThat(data.matches("[0-9].*"), is(true)));
        encodePacket(new Packet<String>(Packet.MESSAGE), (EncodeCallback<String>) data -> assertThat(data.matches("[0-9]"), is(true)));
    }

    @Test
    public void decodeEmptyPayload() {
        Packet<String> p = decodePacket((String)null);
        assertThat(p.type, is(Packet.ERROR));
        assertThat(p.data, is(ERROR_DATA));
    }

    @Test
    public void decodeBadFormat() {
        Packet<String> p = decodePacket(":::");
        assertThat(p.type, is(Packet.ERROR));
        assertThat(p.data, is(ERROR_DATA));
    }

    @Test
    public void decodeInexistentTypes() {
        Packet<String> p = decodePacket("94103");
        assertThat(p.type, is(Packet.ERROR));
        assertThat(p.data, is(ERROR_DATA));
    }

    @Test
    public void encodePayloads()  {
        encodePayload(new Packet[]{new Packet(Packet.PING), new Packet(Packet.PONG)}, data -> assertThat(data, isA(String.class)));
    }

    @Test
    public void encodeAndDecodePayloads()  {
        encodePayload(new Packet[] {new Packet<>(Packet.MESSAGE, "a")}, data -> decodePayload(data, (packet, index, total) -> {
            boolean isLast = index + 1 == total;
            assertThat(isLast, is(true));
            return true;
        }));
        encodePayload(new Packet[]{new Packet<>(Packet.MESSAGE, "a"), new Packet(Packet.PING)}, data -> decodePayload(data, (packet, index, total) -> {
            boolean isLast = index + 1 == total;
            if (!isLast) {
                assertThat(packet.type, is(Packet.MESSAGE));
            } else {
                assertThat(packet.type, is(Packet.PING));
            }
            return true;
        }));
    }

    @Test
    public void encodeAndDecodeEmptyPayloads()  {
        encodePayload(new Packet[] {}, data -> decodePayload(data, (packet, index, total) -> {
            assertThat(packet.type, is(Packet.OPEN));
            boolean isLast = index + 1 == total;
            assertThat(isLast, is(true));
            return true;
        }));
    }

    @Test
    public void notUTF8EncodeWhenDealingWithStringsOnly()  {
        encodePayload(new Packet[] {
                new Packet(Packet.MESSAGE, "€€€"),
                new Packet(Packet.MESSAGE, "α")
        }, data -> assertThat(data, is("4€€€\u001e4α")));
    }

    @Test
    public void decodePayloadBadFormat() {
        decodePayload("", (packet, index, total) -> {
            boolean isLast = index + 1 == total;
            assertThat(packet.type, is(Packet.ERROR));
            assertThat(packet.data, is(ERROR_DATA));
            assertThat(isLast, is(true));
            return true;
        });
        decodePayload("))", (packet, index, total) -> {
            boolean isLast = index + 1 == total;
            assertThat(packet.type, is(Packet.ERROR));
            assertThat(packet.data, is(ERROR_DATA));
            assertThat(isLast, is(true));
            return true;
        });
    }

    @Test
    public void decodePayloadBadPacketFormat() {
        decodePayload("99:", (packet, index, total) -> {
            boolean isLast = index + 1 == total;
            assertThat(packet.type, is(Packet.ERROR));
            assertThat(packet.data, is(ERROR_DATA));
            assertThat(isLast, is(true));
            return true;
        });
        decodePayload("aa", (packet, index, total) -> {
            boolean isLast = index + 1 == total;
            assertThat(packet.type, is(Packet.ERROR));
            assertThat(packet.data, is(ERROR_DATA));
            assertThat(isLast, is(true));
            return true;
        });
    }

    @Test
    public void encodeBinaryMessage()  {
        final byte[] data = new byte[5];
        for (int i = 0; i < data.length; i++) {
            data[0] = (byte)i;
        }
        encodePacket(new Packet<>(Packet.MESSAGE, data), (EncodeCallback<byte[]>) encoded -> {
            Packet<byte[]> p = decodePacket(encoded);
            assertThat(p.type, is(Packet.MESSAGE));
            assertThat(p.data, is(data));
        });
    }

    @Test
    public void encodeBinaryContents()  {
        final byte[] firstBuffer = new byte[5];
        for (int i = 0 ; i < firstBuffer.length; i++) {
            firstBuffer[0] = (byte)i;
        }
        final byte[] secondBuffer = new byte[4];
        for (int i = 0 ; i < secondBuffer.length; i++) {
            secondBuffer[0] = (byte)(firstBuffer.length + i);
        }

        encodePayload(new Packet[]{
                new Packet<>(Packet.MESSAGE, firstBuffer),
                new Packet<>(Packet.MESSAGE, secondBuffer),
        }, data -> decodePayload(data, (DecodePayloadCallback) (packet, index, total) -> {
            boolean isLast = index + 1 == total;
            assertThat(packet.type, is(Packet.MESSAGE));
            if (!isLast) {
                assertThat((byte[])packet.data, is(firstBuffer));
            } else {
                assertThat((byte[])packet.data, is(secondBuffer));
            }
            return true;
        }));
    }

    @Test
    public void encodeMixedBinaryAndStringContents()  {
        final byte[] firstBuffer = new byte[123];
        for (int i = 0 ; i < firstBuffer.length; i++) {
            firstBuffer[0] = (byte)i;
        }
        encodePayload(new Packet[]{
                new Packet<>(Packet.MESSAGE, firstBuffer),
                new Packet<>(Packet.MESSAGE, "hello"),
            new Packet<String>(Packet.CLOSE),
        }, encoded -> decodePayload(encoded, (DecodePayloadCallback) (packet, index, total) -> {
            if (index == 0) {
                assertThat(packet.type, is(Packet.MESSAGE));
                assertThat((byte[])packet.data, is(firstBuffer));
            } else if (index == 1) {
                assertThat(packet.type, is(Packet.MESSAGE));
                assertThat((String)packet.data, is("hello"));
            } else {
                assertThat(packet.type, is(Packet.CLOSE));
            }
            return true;
        }));
    }
}
