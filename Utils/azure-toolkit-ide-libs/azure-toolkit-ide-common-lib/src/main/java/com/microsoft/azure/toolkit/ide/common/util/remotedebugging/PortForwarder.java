package com.microsoft.azure.toolkit.ide.common.util.remotedebugging;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.CompletableFuture;


public class PortForwarder {
    private final OkHttpClient okHttpClient;
    private Closeable closeable;

    public PortForwarder() {
        okHttpClient = new OkHttpClient();
    }

    public void startForward(URL resourceBaseUrl, String token, int localPort) {
        try {
            final ServerSocketChannel server = ServerSocketChannel.open().bind(new InetSocketAddress(localPort));
            AzureTaskManager.getInstance().runOnPooledThread(() -> {
                try {
                    final SocketChannel socketChannel = server.accept();
                    this.closeable = this.forward(resourceBaseUrl, token, socketChannel, socketChannel);
                } catch (final IOException e) {
                    throw new AzureToolkitRuntimeException("Error while deugging", e);
                }
            });
        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException("Unable to start debugging", e);
        }
    }

    public void stopForward() {
        try {
            closeable.close();
        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException("Unable to stop debugging", e);
        }
    }

    private Closeable forward(URL resourceBaseUrl, String token, ReadableByteChannel in, WritableByteChannel out) {
        final PortForwarderWebSocketListener listener = new PortForwarderWebSocketListener(in, out);
        final CompletableFuture<WebSocket> future = new WebSocketBuilder(this.okHttpClient).uri(URI.create(resourceBaseUrl.toString()))
                .header("Authorization", String.format("Bearer %s", token))
                .buildAsync(listener);
        future.whenComplete((socket, throwable) -> {
            if (throwable != null) {
                listener.onError(socket, throwable);
            }
        });
        return () -> {
            future.cancel(true);
            future.whenComplete((socket, throwable) -> {
                if (socket != null) {
                    socket.close(1001, "User closing");
                }
            });
        };
    }
}
