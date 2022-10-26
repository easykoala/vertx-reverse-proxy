package com.greengrass;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketUtil {
    private static Logger logger = LoggerFactory.getLogger(SocketUtil.class);
    public static final String CHANNELID = "channelId";

    public static void sendTo(Buffer bytes, WriteStream<Buffer> writer, ReadStream<Buffer> reader) {
        try {
            writer.write(bytes);
            if (writer.writeQueueFull()) {
                reader.pause();
                writer.drainHandler(done -> reader.resume());
            }
        } catch (Throwable e) {
            logger.error(e.getMessage());
        }
    }
}
