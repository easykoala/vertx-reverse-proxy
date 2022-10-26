package com.greengrass;

import com.greengrass.coder.MessageDecoder;
import com.greengrass.coder.MessageEncoder;
import com.greengrass.exception.ProxyException;
import com.greengrass.protocol.Message;
import com.greengrass.protocol.MessageType;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class ProxyHandler implements Handler<Buffer> {
    private static Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    public static final String CHANNELID = "channelId";
    private MessageEncoder encoder = new MessageEncoder();
    private MessageDecoder decoder = new MessageDecoder();

    public NetSocket netSocket;
    private Vertx vertx;
    private String password;
    private int port;

    private boolean register = false;
    private ConcurrentMap<Object, ProxyHandler> proxyHandlers;
    public Map<Object, NetSocket> sockets = new HashMap<>();
    private NetServer proxyServer = null;

    public ProxyHandler(Vertx vertx, final NetSocket netSocket, String password, final ConcurrentMap<Object, ProxyHandler> proxyHandlers) {
        this.vertx = vertx;
        this.netSocket = netSocket;
        this.password = password;
        this.proxyHandlers = proxyHandlers;

        this.netSocket.closeHandler(event -> {
            logger.info(String.format("Client disconnected %s:%s", this.netSocket.remoteAddress().host(), this.netSocket.remoteAddress().port()));
            if (proxyServer != null)
                proxyServer.close(h -> proxyHandlers.remove(port));
            this.netSocket.handler(null);
        });
    }

    @Override
    public void handle(Buffer buffer) {
        try {
            ArrayList<Message> messages = decoder.decode(buffer);
            if (messages.isEmpty()) {
                logger.debug("Waiting for data...");
            } else {
                for (Message message : messages) {
                    if (message.getType() == MessageType.REGISTER) {
                        processRegister(message);
                    } else if (register) {
                        if (message.getType() == MessageType.DISCONNECTED) {
                            processDisconnected(message);
                        } else if (message.getType() == MessageType.DATA) {
                            processData(message);
                        } else if (message.getType() == MessageType.KEEPALIVE) {
                            // heart beat
                        } else {
                            throw new ProxyException("Unknown type: " + message.getType());
                        }
                    } else {
                        netSocket.close();
                    }
                }
                messages.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processRegister(Message message) {
        HashMap<String, Object> metaData = new HashMap<>();

        String password = message.getMetaData().get("password").toString();
        port = (int) message.getMetaData().get("port");
        if (this.password != null && !this.password.equals(password) || proxyHandlers.get(port) != null) {
            metaData.put("success", false);
            metaData.put("reason", "Token is wrong or already registered on port " + port);
        } else {
            try {
                proxyServer = vertx.createNetServer();
                proxyServer.connectHandler(proxySocket -> {
                    logger.info(String.format("Client connected %s:%s %s", proxySocket.remoteAddress().host(), proxySocket.remoteAddress().port(), proxySocket.writeHandlerID()));
                    sockets.put(proxySocket.writeHandlerID(), proxySocket);
                    RemoteProxyHandler handler = new RemoteProxyHandler(netSocket, proxySocket.writeHandlerID());
                    handler.send(Buffer.buffer(""), MessageType.CONNECTED);
                    proxySocket.handler(handler);

                    proxySocket.exceptionHandler(evt -> {
                        logger.error("Proxy error " + proxySocket.writeHandlerID() + " error: " + evt.getMessage(), evt.getCause());
                    });
                    proxySocket.closeHandler(c -> {
                        logger.info("Proxy closed " + proxySocket.writeHandlerID());
                    });
                });

                metaData.put("success", true);
                register = true;
                proxyServer.listen(port, r -> {
                    if (r.succeeded()) {
                        logger.info("Register success, start proxy server on port: " + port);
                        proxyHandlers.put(port, this);
                    } else
                        r.cause().printStackTrace();
                });
            } catch (Exception e) {
                metaData.put("success", false);
                metaData.put("reason", e.getMessage());
                e.printStackTrace();
            }
        }

        Message sendBackMessage = new Message();
        sendBackMessage.setType(MessageType.REGISTER_RESULT);
        sendBackMessage.setMetaData(metaData);
        SocketUtil.sendTo(encoder.encode(sendBackMessage), netSocket, netSocket);

        if (!register) {
            System.out.println("Client register error: " + metaData.get("reason"));
            netSocket.close();
        }
    }

    private void processData(Message message) {
        NetSocket proxySocket = sockets.get(message.getMetaData().get(CHANNELID));
        if (proxySocket == null) {
            logger.info("processData---proxySocket is null, " + message.getMetaData().get(CHANNELID));
            // throw new ProxyException("processData---proxySocket is null, " + Message.getMetaData().get(CHANNELID));
        }
        SocketUtil.sendTo(Buffer.buffer(message.getData()), proxySocket, proxySocket);
    }

    private void processDisconnected(Message message) {
        netSocket.close();
    }

    @Override
    public void finalize() {
        logger.info("finalized");
    }
}
