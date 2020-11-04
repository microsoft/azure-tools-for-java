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

package com.microsoft.azure.toolkit.lib.common.utils;

import com.neovisionaries.ws.client.*;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;

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
            logger.warning(String.format("Encounter error while proxying websocket: %s", e.getMessage()));
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
        this.webSocket = new WebSocketFactory().setConnectionTimeout(connectTimeout).createSocket(webSocketServerUri)
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
