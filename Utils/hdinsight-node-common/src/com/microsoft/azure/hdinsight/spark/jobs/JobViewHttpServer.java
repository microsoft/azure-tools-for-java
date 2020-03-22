/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
