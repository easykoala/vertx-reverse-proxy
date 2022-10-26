package com.greengrass;

import io.vertx.core.Vertx;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CLIException;

import java.util.Arrays;
import java.util.List;

public class StartClient {
    public static void main(String[] args) throws Exception {
//        System.setProperty("vertx.logger-delegate-factory-class-name","io.vertx.core.logging.Log4j2LogDelegateFactory");

        io.vertx.core.cli.CommandLine commandLine = cli(args);
        if (commandLine == null)
            System.exit(-1);

        String password = commandLine.getOptionValue("t");

        String host = commandLine.getOptionValue("h");
        String[] hosts = host.split(":");
        String remoteHost = hosts[0];
        String remotePort = hosts[1];

        String reverse = commandLine.getOptionValue("R");
        String[] proxies = reverse.split(":");
        String remoteProxyPort = proxies[0];
        String localProxyHost = proxies[1];
        String localPort = proxies[2];

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new ClientVerticle(remoteHost, Integer.parseInt(remotePort), password, Integer.parseInt(remoteProxyPort), localProxyHost, Integer.parseInt(localPort)));
    }

    static io.vertx.core.cli.CommandLine cli(String[] args) {
        CLI cli = CLI.create("java -jar <reverse-proxy>-fat.jar")
                .setSummary("A vert.x Proxy")
                .addOption(new io.vertx.core.cli.Option()
                        .setLongName("host")
                        .setShortName("h")
                        .setDescription("host and port")
                        .setRequired(true)
                ).addOption(new io.vertx.core.cli.Option()
                        .setLongName("token")
                        .setShortName("t")
                        .setDescription("remote access token")
                        .setRequired(true)
                ).addOption(new io.vertx.core.cli.Option()
                        .setLongName("port")
                        .setShortName("R")
                        .setDescription("proxy port")
                        .setRequired(true)
                );

        // parsing
        io.vertx.core.cli.CommandLine commandLine = null;
        try {
            List<String> userCommandLineArguments = Arrays.asList(args);
            commandLine = cli.parse(userCommandLineArguments);
        } catch (CLIException e) {
            // usage
            StringBuilder builder = new StringBuilder();
            cli.usage(builder);
            System.out.println(builder.toString());
//            throw e;
        }
        return commandLine;
    }

}
