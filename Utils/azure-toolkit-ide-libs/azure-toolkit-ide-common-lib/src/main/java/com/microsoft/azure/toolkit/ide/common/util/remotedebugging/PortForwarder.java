package com.microsoft.azure.toolkit.ide.common.util.remotedebugging;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
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

    public static void startForward(String url, String token, int localPort) {
        try {
            final URL resourceBaseUrl = new URL(url);
            final ServerSocketChannel server = ServerSocketChannel.open().bind(new InetSocketAddress(localPort));
            forward(resourceBaseUrl, token, server);
        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException("Unable to start debugging.", e);
        }
    }

    private static void forward(URL resourceBaseUrl, String token, ServerSocketChannel server) throws IOException {
        final SocketChannel socketChannel = server.accept();
        final OkHttpClient okHttpClient = new OkHttpClient();
        final PortForwarderWebSocketListener listener = new PortForwarderWebSocketListener(socketChannel, socketChannel);
        final CompletableFuture<WebSocket> future = new WebSocketBuilder(okHttpClient).uri(URI.create(resourceBaseUrl.toString()))
                .header("Authorization", String.format("Bearer %s", token))
                .buildAsync(listener);
        future.whenComplete((socket, throwable) -> {
            if (throwable != null) {
                listener.onError(socket, throwable);
            }
        });
    }
}
