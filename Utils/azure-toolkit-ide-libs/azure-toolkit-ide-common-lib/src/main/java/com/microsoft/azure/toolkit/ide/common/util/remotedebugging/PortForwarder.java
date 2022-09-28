package com.microsoft.azure.toolkit.ide.common.util.remotedebugging;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;


public class PortForwarder {
    private final OkHttpClient okHttpClient;

    public PortForwarder() {
        okHttpClient = new OkHttpClient();
    }

    public void startForward(String url, String token, int localPort) {
        try {
            final URL resourceBaseUrl = new URL(url);
            final ServerSocketChannel server = ServerSocketChannel.open().bind(new InetSocketAddress(localPort));
            AzureTaskManager.getInstance().runOnPooledThread(() -> {
                try {
                    this.forward(resourceBaseUrl, token, server);
                } catch (final IOException e) {
                    throw new AzureToolkitRuntimeException("Error while deugging.", e);
                }
            });
        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException("Unable to start debugging.", e);
        }
    }

    private void forward(URL resourceBaseUrl, String token, ServerSocketChannel server) throws IOException {
        final SocketChannel socketChannel = server.accept();
        final PortForwarderWebSocketListener listener = new PortForwarderWebSocketListener(socketChannel, socketChannel);
        final CompletableFuture<WebSocket> future = new WebSocketBuilder(this.okHttpClient).uri(URI.create(resourceBaseUrl.toString()))
                .header("Authorization", String.format("Bearer %s", token))
                .buildAsync(listener);
        future.whenComplete((socket, throwable) -> {
            if (server.isOpen()) {
                try {
                    server.close();
                } catch (final IOException e) {
                    throw new AzureToolkitRuntimeException("Unable to close server.", e);
                }
            }
            if (throwable != null) {
                listener.onError(socket, throwable);
            }
        });
    }
}
