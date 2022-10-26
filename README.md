# vertx-reverse-proxy

Reverse proxy is a proxy tool based on Java Vertx that can be used for intranet penetration and supports TCP protocol.

Reverse proxy consists of a server and a client. The server runs on a server with a public IP address, and the client runs on a machine.

Since our network applications launched locally cannot provide external access, reverse proxy can be used to map the local network address to the external network in this scenario to provide external access.

Download: https://github.com/easykoala/vertx-reverse-proxy.git

##Start

###Server startup

Execute the Java command on the server with the public IP address:

```

java -jar target\reverse-proxy-server-fat. jar -p 1076 -t access-token

```

The output indicates successful startup:

```

Server listening on port 1076

```

The default server port number is 1076. Note that this port number is the port number that the reverse proxy client connects to the reverse proxy server, not the port number that provides access to the external network.


###Client startup

Execute Java commands on the local machine:

```

java -jar reverse-proxy-client-fat. jar -h remotehost-ip:1076 -t access-token -R 1075:localhost:3306

```

Parameter description:

-'p' Port of the server or client

-'t 'Access token of server or client

-'R' 1075 Proxy port number, localhost: 3306 is the address and port to be accessed. Here is the local port number, or you want to access ip: port

After successful startup, you can access the proxied application through the public IP. If the proxied application is TCP, such as Navicat application, you can access the local MySQL database through remotehost-ip: 1075.

##Typical usage scenarios

-When developing the WeChat official account service, because the local machine does not have a public IP, the WeChat server cannot access the local interface, which is very inconvenient for debugging. You can use reverse proxy to map the local port to the external network, which is convenient for debugging.
