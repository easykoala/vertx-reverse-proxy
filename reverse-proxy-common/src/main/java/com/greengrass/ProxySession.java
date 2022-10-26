package com.greengrass;

import com.greengrass.coder.MessageDecoder;
import com.greengrass.coder.MessageEncoder;
import com.greengrass.protocol.Message;
import com.greengrass.protocol.MessageType;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class ProxySession {

    private static Logger logger = LoggerFactory.getLogger(ProxySession.class);

    private Vertx vertx;
    private MessageDecoder decoder;
    private MessageEncoder encoder;
    private NetSocket netSocket;
    private long keepAliveTimerID = -1;
    private boolean keepAliveTimeEnded;
    private Object channelId;

    public ProxySession(Vertx vertx, NetSocket netSocket, Object channelId) {
        this.vertx = vertx;
        this.netSocket = netSocket;
        this.channelId = channelId;
        this.decoder = new MessageDecoder();
        this.encoder = new MessageEncoder();
    }

    public void startKeepAliveTimer(int keepAliveSeconds) {
        logger.info("startKeepAliveTimer...");
        if (keepAliveSeconds > 0) {
            keepAliveTimeEnded = true;
            /*
             * If the Keep Alive value is non-zero and the Server does not receive a Control Packet from the Client
             * within one and a half times the Keep Alive time period, it MUST disconnect
             */
            long keepAliveMillis = keepAliveSeconds * 1500;
            keepAliveTimerID = vertx.setPeriodic(keepAliveMillis, tid -> {
                if (keepAliveTimeEnded) {
                    keepAlive();
                    // stopKeepAliveTimer();
                }
                // next time, will close connection
                keepAliveTimeEnded = true;
            });
        }
    }

    private void stopKeepAliveTimer() {
        boolean removed = vertx.cancelTimer(keepAliveTimerID);
        if (!removed) {
            logger.warn("cancel old timer failed ID: " + keepAliveTimerID);
        }
    }

    public void resetKeepAliveTimer() {
        keepAliveTimeEnded = false;
    }

    private void keepAlive() {
        Message message = new Message();
        message.setType(MessageType.KEEPALIVE);
        HashMap<String, Object> metaData = new HashMap<>();
        metaData.put(SocketUtil.CHANNELID, channelId);
        message.setMetaData(metaData);
        message.setData(new byte[0]);
        SocketUtil.sendTo(encoder.encode(message), netSocket, netSocket);
    }

}
