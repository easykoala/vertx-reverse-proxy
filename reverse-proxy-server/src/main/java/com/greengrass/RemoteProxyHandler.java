package com.greengrass;

import com.greengrass.coder.MessageEncoder;
import com.greengrass.protocol.Message;
import com.greengrass.protocol.MessageType;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class RemoteProxyHandler implements Handler<Buffer> {
    private static Logger logger = LoggerFactory.getLogger(RemoteProxyHandler.class);
    private MessageEncoder encoder = new MessageEncoder();

    private NetSocket netSocket;
    private Object channelId;

    public RemoteProxyHandler(NetSocket netSocket, Object channelId) {
        this.netSocket = netSocket;
        this.channelId = channelId;
    }

    @Override
    public void handle(Buffer buffer) {
        send(buffer, MessageType.DATA);
    }

    public void send(Buffer buffer, MessageType type) {
        Message message = new Message();
        message.setType(type);
        message.setData(buffer.getBytes());
        HashMap<String, Object> metaData = new HashMap<>();
        metaData.put(ProxyHandler.CHANNELID, this.channelId);
        message.setMetaData(metaData);
        SocketUtil.sendTo(encoder.encode(message), netSocket, netSocket);
    }

}
