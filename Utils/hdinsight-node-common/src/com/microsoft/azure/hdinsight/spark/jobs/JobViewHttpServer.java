/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.jobs;

import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JobViewHttpServer {
    private static HttpServer server;
    private static final int NUMBER_OF_THREADS = 50;
    private static ExecutorService executorService;
    private static boolean isEnabled = false;
    private static int port = -1;

    public synchronized static boolean isEnabled() {
        return isEnabled;
    }

    public synchronized static void close() {
        if (server != null) {
            server.stop(0);
        }
        if (executorService != null) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
        }
        isEnabled = false;
    }

    public synchronized static int getPort() {
        return port;
    }

    public synchronized static void initialize() {
        if (isEnabled) {
            return;
        }

        try {
            // try to get a random socket port
            ServerSocket s = new ServerSocket(0);
            s.close();

            InetSocketAddress socketAddress = new InetSocketAddress(s.getLocalPort());
            port = socketAddress.getPort();

            server = HttpServer.create(socketAddress, NUMBER_OF_THREADS);

            server.createContext("/try", (httpExchange) -> {
                    httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    JobUtils.setResponse(httpExchange, "Connect Successfully");
            });
            server.createContext("/applications", new SparkJobHttpHandler());
            server.createContext("/apps", new YarnJobHttpHandler());
            server.createContext("/actions", new ActionHttpHandler());

            executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
            server.setExecutor(executorService);
            server.start();
            isEnabled = true;
        } catch (IOException e) {
        }
    }
}
