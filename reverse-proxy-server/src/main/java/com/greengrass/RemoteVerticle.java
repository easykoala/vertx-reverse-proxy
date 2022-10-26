package com.greengrass;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(RemoteVerticle.class);
    private ConcurrentMap<Object, ProxyHandler> proxyHandlers = new ConcurrentHashMap<>();

    @Override
    public void start() throws Exception {
        super.start();
        logger.debug("config: " + config().toString());

        NetServerOptions options = new NetServerOptions();
        options.setTcpKeepAlive(true);
        options.setIdleTimeout(0);
        options.setTcpQuickAck(true);
        NetServer netServer = vertx.createNetServer(options);
        netServer.connectHandler(clientSocket -> {
            logger.info(String.format("Client registered %s:%s %s", clientSocket.remoteAddress().host(), clientSocket.remoteAddress().port(), clientSocket.writeHandlerID()));
            clientSocket.handler(new ProxyHandler(vertx, clientSocket, config().getString("password"), proxyHandlers));
            clientSocket.exceptionHandler(r -> {
                logger.error(r.getMessage());
                clientSocket.close();
            });
        });
        netServer.listen(config().getInteger("port"), r -> {
            if (r.succeeded())
                logger.info("Server listening on port " + config().getInteger("port"));
            else
                r.cause().printStackTrace();
        });

    }

}
