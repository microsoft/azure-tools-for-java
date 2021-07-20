/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

import com.microsoft.azure.toolkit.lib.Azure;
import com.neovisionaries.ws.client.*;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class WebSocketSSLProxy {
    private static final Logger logger = Logger.getLogger(WebSocketSSLProxy.class.getName());
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    @Setter
    @Getter
    private int bufferSize = DEFAULT_BUFFER_SIZE;

    @Setter
    @Getter
    private int connectTimeout = 0;

    private String webSocketServerUri;
    private String id;
    private String password;
    private ServerSocket serverSocket;
    private Thread thread;
    private WebSocket webSocket;

    public WebSocketSSLProxy(String webSocketServerUri, String id, String password) {
        this.webSocketServerUri = webSocketServerUri;
        this.id = id;
        this.password = password;
    }

    public void start() throws IOException {
        close();
        // InetAddress.getByName(null) points to the loopback address (127.0.0.1)
        serverSocket = new ServerSocket(0, 1, InetAddress.getByName(null));
        thread = new Thread(() -> {
            try {
                for (;;) {
                    Socket clientSocket = serverSocket.accept();
                    createWebSocketToSocket(clientSocket);
                    pipeSocketDataToWebSocket(clientSocket);
                }
            } catch (IOException | WebSocketException e) {
                handleConnectionBroken(e);
            }

        });
        thread.setName("WebsocketSSLProxy-" + thread.getId());
        thread.start();
    }

    public void close() {
        if (this.webSocket != null) {
            this.webSocket.disconnect();
            this.webSocket = null;
        }
        if (this.serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // ignore
            }
            serverSocket = null;
        }

        if (thread != null) {
            this.thread.interrupt();
            this.thread = null;
        }
    }

    public int getLocalPort() {
        if (Objects.isNull(serverSocket)) {
            return 0;
        }
        return serverSocket.getLocalPort();
    }

    private void handleConnectionBroken(Exception e) {
        if (Objects.nonNull(serverSocket)) {
            logger.warning(message("common.webSocket.error.proxyingWebSocketFailed", e.getMessage()));
        }
        close();
    }

    private void pipeSocketDataToWebSocket(Socket socket) throws IOException {
        byte[] buffer = new byte[bufferSize];
        while (true) {
            int bytesRead = socket.getInputStream().read(buffer);
            if (bytesRead == -1) {
                break;
            }

            webSocket.sendBinary(Arrays.copyOfRange(buffer, 0, bytesRead));
        }
    }

    private void createWebSocketToSocket(Socket client) throws IOException, WebSocketException {
        final WebSocketFactory webSocketFactory = new WebSocketFactory();
        final ProxySettings proxySettings = webSocketFactory.getProxySettings();
        final InetSocketAddress httpProxy = Azure.az().config().getHttpProxy();
        if (httpProxy != null) {
            proxySettings.setServer(httpProxy.getHostString());
            proxySettings.setPort(httpProxy.getPort());
        }
        this.webSocket = webSocketFactory.setConnectionTimeout(connectTimeout).createSocket(webSocketServerUri)
                                               .setUserInfo(this.id, this.password)
                                               .addListener(new WebSocketAdapter() {
                                                   @Override
                                                   public void onBinaryMessage(WebSocket websocket, byte[] bytes) {
                                                       try {
                                                           client.getOutputStream().write(bytes);
                                                       } catch (IOException e) {
                                                           handleConnectionBroken(e);
                                                       }
                                                   }
                                               }).addExtension(WebSocketExtension.PERMESSAGE_DEFLATE).connect();

    }
}
