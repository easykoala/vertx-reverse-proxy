package com.greengrass;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CLIException;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public class StartProxy {
    public static void main(String[] args) throws Exception {
//        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");

        io.vertx.core.cli.CommandLine commandLine = cli(args);
        if (commandLine == null)
            System.exit(-1);

        String port = commandLine.getOptionValue("p");
        String token = commandLine.getOptionValue("t");
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        JsonObject config = new JsonObject();
        config.put("port", Integer.parseInt(port));
        config.put("password", token);
        deploymentOptions.setConfig(config);
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new RemoteVerticle(), deploymentOptions, r ->
        {
            if (r.failed())
                r.cause().printStackTrace();
        });

    }

    static io.vertx.core.cli.CommandLine cli(String[] args) {
        CLI cli = CLI.create("java -jar <reverse-proxy>-fat.jar")
                .setSummary("A vert.x Proxy")
                .addOption(new io.vertx.core.cli.Option()
                        .setLongName("port")
                        .setShortName("p")
                        .setDescription("connect port")
                        .setRequired(true)
                )
                .addOption(new io.vertx.core.cli.Option()
                        .setLongName("token")
                        .setShortName("t")
                        .setDescription("access token")
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
