package com.greengrass.coder;

import com.greengrass.protocol.Message;
import com.greengrass.protocol.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Wangzb
 */
public class MessageDecoder {
    public static final long WAIT_TIMEOUT = 10000;
    private static Logger logger = LoggerFactory.getLogger(MessageDecoder.class);
    private ByteBuf tokenCache = ByteBufAllocator.DEFAULT.buffer(4096);

    protected void decode(Buffer buffer, List<Message> out) throws Exception {
        int receivedLength = buffer.length();
        byte[] decoded = new byte[receivedLength];
        buffer.getBytes(decoded);

        long startTime = System.currentTimeMillis();
        ByteBuf in = tokenCache;
        in.writeBytes(decoded);

        while (System.currentTimeMillis() - startTime < WAIT_TIMEOUT) {
            in.markReaderIndex();
            if (in.readableBytes() < 4) {
                return;
            }
            int readableBytes = in.readableBytes();
            int dataLength = in.readInt();
            logger.debug(String.format("readable bytes: %d, data length: %d, ", readableBytes, dataLength));
            if (in.readableBytes() < dataLength) {
                in.resetReaderIndex();
                return;
            }

            // Message message = Json.decodeValue(Buffer.buffer(decoded), Message.class);
            Message message = new Message();
            message.setType(MessageType.valueOf(in.readInt()));
            int metaLen = in.readInt();
            CharSequence cs = in.readCharSequence(metaLen, Charset.defaultCharset());
            message.setMetaData(Json.decodeValue(cs.toString(), Map.class));
            if (in.readableBytes() > 0) {
                decoded = new byte[dataLength - metaLen - 8];
                in.readBytes(decoded);
                message.setData(decoded);
            }
            out.add(message);
            // logger.info("after readableBytes: " + in.readableBytes() + " " + (dataLength - metaLen - 8) + " " + in.readerIndex());
            if (logger.isDebugEnabled()) {
                String s = message.getData() == null ? "" : new String(message.getData());
                int len = s.length() > 120 ? 120 : s.length();
                logger.debug(String.format("Received---total、length、remain and content：%d, %d, %d, %s, %s ", receivedLength, message.getData() == null ? 0 : message.getData().length, in.readableBytes(), message.getMetaData().get("channelId"), s.substring(0, len)));
            }
            if (in.readableBytes() == 0) {
                in.clear();
            }
        }
        logger.info("timeout readableBytes: " + in.readableBytes());
    }

    public ArrayList<Message> decode(Buffer in) throws Exception {
        ArrayList<Message> out = new ArrayList<>();
        decode(in, out);
        if (out.size() > 1) {
            logger.debug("Parsed too many messages: " + out.size());
        }
        return out;
    }
}