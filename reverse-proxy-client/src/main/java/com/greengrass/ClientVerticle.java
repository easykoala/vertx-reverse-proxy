package com.greengrass;

import com.greengrass.coder.MessageDecoder;
import com.greengrass.coder.MessageEncoder;
import com.greengrass.exception.ProxyException;
import com.greengrass.protocol.Message;
import com.greengrass.protocol.MessageType;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(ClientVerticle.class);
    public static final String CHANNELID = "channelId";
    private MessageEncoder encoder = new MessageEncoder();
    private MessageDecoder decoder = new MessageDecoder();

    private int port;
    private String password;
    private String localProxyHost;
    private int localPort;
    private int remotePort;
    private String remoteHost;
    private NetSocket clientSocket = null;
    private NetClient localClient;
    private ConcurrentMap<Object, Object> sockets = new ConcurrentHashMap<>();

    public ClientVerticle(String remoteHost, int remotePort, String password, int remoteProxyPort, String localProxyHost, int localPort) {
        this.port = remoteProxyPort;
        this.password = password;
        this.localProxyHost = localProxyHost;
        this.localPort = localPort;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    @Override
    public void start() throws Exception {
        super.start();
        NetClientOptions options = new NetClientOptions();
        options.setTcpKeepAlive(true);
        options.setIdleTimeout(0);
        options.setTcpQuickAck(true);
        localClient = vertx.createNetClient();
        NetClient client = vertx.createNetClient(options);
        client.connect(remotePort, remoteHost, conn -> {
            if (conn.succeeded()) {
                logger.info(String.format("Connected to %s:%d", remoteHost, remotePort));
                clientSocket = conn.result();

                ProxySession proxySession = new ProxySession(vertx, clientSocket, clientSocket.writeHandlerID());
                proxySession.startKeepAliveTimer(30);

                clientSocket.handler(buffer -> {
                    proxySession.resetKeepAliveTimer();
                    try {
                        ArrayList<Message> messages = decoder.decode(buffer);
                        if (messages.isEmpty()) {
                            logger.debug("Waiting for data...");
                        } else {
                            for (Message message : messages) {
                                if (message.getType() == MessageType.REGISTER_RESULT) {
                                    processRegisterResult(clientSocket, message);
                                } else if (message.getType() == MessageType.CONNECTED) {
                                    processConnected(message);
                                } else if (message.getType() == MessageType.DISCONNECTED) {
                                    processDisconnected(message);
                                } else if (message.getType() == MessageType.DATA) {
                                    processData(message);
                                } else if (message.getType() == MessageType.KEEPALIVE) {
                                    // heart beat
                                } else {
                                    throw new ProxyException("Unknown type: " + message.getType());
                                }
                            }
                            messages.clear();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                clientSocket.closeHandler(event -> {
                    logger.info(String.format("Client disnnected %s:%s", clientSocket.remoteAddress().host(), clientSocket.remoteAddress().port()));
                    destroy();
                });
                clientSocket.exceptionHandler(e -> {
                    logger.error(e.getMessage());
                });

                registerClient();
            } else {
                logger.error(conn.cause().getMessage());
                destroy();
            }
        });
    }

    private void registerClient() {
        // register client information
        Message message = new Message();
        message.setType(MessageType.REGISTER);
        HashMap<String, Object> metaData = new HashMap<>();
        metaData.put("port", port);
        metaData.put("password", password);
        message.setMetaData(metaData);
        SocketUtil.sendTo(encoder.encode(message), clientSocket, clientSocket);
    }

    private void processRegisterResult(NetSocket netSocket, Message message) {
        if ((Boolean) message.getMetaData().get("success")) {
            logger.info("Register to Proxy server");
        } else {
            logger.error("Register fail: " + message.getMetaData().get("reason"));
            netSocket.close();
        }
    }

    private void processConnected(Message message) {
        Object channelId = message.getMetaData().get(CHANNELID);
        if (sockets.get(channelId) != null) return;

        localClient.connect(localPort, localProxyHost, result -> {
            if (result.succeeded()) {
                NetSocket proxySocket = result.result();
                sockets.put(channelId, proxySocket);
                sockets.put(proxySocket, channelId);
                logger.info("Connected to proxy from " + channelId);
                if (message.getType() == MessageType.DATA) {// Call from processData
                    SocketUtil.sendTo(Buffer.buffer(message.getData()), proxySocket, proxySocket);
                }
                proxySocket.handler(buffer -> {
                    String s = buffer.toString();
                    logger.debug("Proxy message: " + buffer.length() + " " + s.substring(0, s.length() > 120 ? 120 : s.length()));

                    Message msg = new Message();
                    msg.setType(MessageType.DATA);
                    msg.setData(buffer.getBytes());
                    HashMap<String, Object> metaData = new HashMap<>();
                    metaData.put(CHANNELID, channelId);
                    msg.setMetaData(metaData);
                    SocketUtil.sendTo(encoder.encode(msg), clientSocket, clientSocket);
                });

                proxySocket.exceptionHandler(event -> {
                    event.printStackTrace();
                });
                proxySocket.closeHandler(event -> {
                    logger.info("Proxy closed " + channelId);
                    sockets.remove(sockets.get(proxySocket));
                    sockets.remove(proxySocket);
                });
            } else {
                logger.error(result.cause().getMessage());
            }
        });
    }

    private void processDisconnected(Message message) {
    }

    private void processData(Message message) {
        NetSocket proxySocket = (NetSocket) sockets.get(message.getMetaData().get(CHANNELID));
        if (proxySocket != null)
            SocketUtil.sendTo(Buffer.buffer(message.getData()), proxySocket, proxySocket);
        else {
            // unconnected or disconnected proxy, http's case: message's type is MessageType.DATA
            // logger.info("Connecting to proxy: " + message.getMetaData().get(CHANNELID));
            processConnected(message);
        }
    }

    private void destroy() {
        vertx.undeploy(super.deploymentID(), r -> {
            if (r.succeeded()) {
                logger.info("Undeployed successful");
                System.exit(-1);
            }
        });
    }
}
