package com.greengrass.coder;

import com.greengrass.protocol.Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * @author Wangzb
 */
public class MessageEncoder {
    private static Logger logger = LoggerFactory.getLogger(MessageEncoder.class);

    protected void encode(Message msg, ByteBuf out) {
        Buffer buffer = Json.encodeToBuffer(msg);
        out.writeInt(buffer.length());
        out.writeBytes(buffer.getBytes());
    }

    public Buffer encode(Message msg) {
        // Buffer newBuff = Buffer.buffer(4096);
        ByteBuf out = ByteBufAllocator.DEFAULT.buffer(4096);
        out.writeInt(0);
        out.writeInt(msg.getType().getCode());
        int metaPos = out.writerIndex();
        out.writeInt(0);
        String metaData = Json.encode(msg.getMetaData());
        out.writeCharSequence(metaData, Charset.defaultCharset());
        out.setInt(metaPos, out.writerIndex() - metaPos - 4);
        if (msg.getData() != null)
            out.writeBytes(msg.getData());
        out.setInt(0, out.writerIndex() - 4);

        if (logger.isDebugEnabled()) {
            String s = msg.getData() == null ? "" : new String(msg.getData());
            int len = s.length() > 120 ? 120 : s.length();
            logger.debug(String.format("Sent---total、length and content：%d, %d, %s, %s ", out.writerIndex(), msg.getData() == null ? 0 : msg.getData().length, msg.getMetaData().get("channelId"), s.substring(0, len)));
        }
        return Buffer.buffer(out);
    }
}