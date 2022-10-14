/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.portforwarder;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractPortForwarder {
    protected ServerSocketChannel server;

    public void startForward(final int localPort) {
        try {
            this.server = ServerSocketChannel.open().bind(new InetSocketAddress(localPort));
            final OkHttpClient okHttpClient = new OkHttpClient();
            final PortForwarderWebSocketListener listener = createWebSocketListener(server.accept());
            final CompletableFuture<WebSocket> future = createSocketBuilder(okHttpClient).buildAsync(listener);
            future.whenComplete((socket, throwable) -> Optional.ofNullable(throwable).ifPresent(t -> listener.onError(socket, t)));
        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException("Unable to start debugging.", e);
        }
    }

    public void stopForward() {
        if (Objects.nonNull(server) && server.isOpen()) {
            try {
                server.close();
            } catch (final IOException e) {
                throw new AzureToolkitRuntimeException(e);
            }
        }
    }

    protected abstract WebSocketBuilder createSocketBuilder(OkHttpClient httpClient);

    protected abstract PortForwarderWebSocketListener createWebSocketListener(final SocketChannel portForwarder);

}
