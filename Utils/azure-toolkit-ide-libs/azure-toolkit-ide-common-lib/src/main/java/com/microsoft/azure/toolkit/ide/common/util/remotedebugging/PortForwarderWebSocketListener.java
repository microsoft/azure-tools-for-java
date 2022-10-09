/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.util.remotedebugging;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

public class PortForwarderWebSocketListener extends WebSocketListener {
    private volatile boolean opened;
    private boolean more = true;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition moreRequested;
    private final CompletableFuture<WebSocket> future;
    private final ExecutorService pumperService = Executors.newSingleThreadExecutor();
    private final AtomicBoolean alive = new AtomicBoolean(true);
    private final ReadableByteChannel in;
    private final WritableByteChannel out;
    private int messagesRead = 0;

    public PortForwarderWebSocketListener(ReadableByteChannel in, WritableByteChannel out) {
        this.in = in;
        this.out = out;
        this.future = new CompletableFuture<>();
        this.moreRequested = this.lock.newCondition();
    }

    public CompletableFuture<WebSocket> getFuture() {
        return future;
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        if (response != null) {
            response.close();
        }
        if (!this.opened) {
            if (response != null) {
                future.completeExceptionally(t);
            } else {
                future.completeExceptionally(t);
            }
        } else {
            this.onError(webSocket, t);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        this.opened = true;
        if (response != null) {
            response.close();
        }
        if (this.in != null) {
            this.pumperService.execute(() -> {
                try {
                    pipe(this.in, webSocket, this.alive::get);
                } catch (final InterruptedException | IOException e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    if (this.alive.get()) {
                        this.closeWebSocket(webSocket, 1001, "Client error");
                        throw new AzureToolkitRuntimeException("Error while writing client data.", e);
                    }
                }
            });
        }
        future.complete(webSocket);
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        this.writeMessage(webSocket, ByteString.of(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        this.writeMessage(webSocket, bytes);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        this.awaitMoreRequest();
        if (this.alive.get()) {
            this.closeForwarder();
            this.future.cancel(true);
        }
    }

    public void onError(WebSocket webSocket, Throwable t) {
        if (this.alive.get()) {
            this.closeForwarder();
            throw new AzureToolkitRuntimeException("Throwable received from websocket.", t);
        }
    }

    private void request() {
        this.lock.lock();
        try {
            this.more = true;
            this.moreRequested.signalAll();
        } finally {
            this.lock.unlock();
        }
    }

    private void closeWebSocket(WebSocket webSocket, int code, String message) {
        this.alive.set(false);
        try {
            webSocket.close(code, message);
        } catch (final Exception e) {
            throw new AzureToolkitRuntimeException("Error while closing the websocket.", e);
        }
        this.closeForwarder();
    }

    private void closeForwarder() {
        this.alive.set(false);
        if (this.in != null) {
            try {
                this.in.close();
            } catch (final IOException e) {
                throw new AzureToolkitRuntimeException("Error while stop debugger.", e);
            }
        }
        if (this.out != null && this.out != this.in) {
            try {
                this.out.close();
            } catch (final IOException e) {
                throw new AzureToolkitRuntimeException("Error while stop debugger.", e);
            }
        }
        this.pumperService.shutdownNow();
    }

    private void pipe(ReadableByteChannel in, WebSocket webSocket, BooleanSupplier isAlive) throws IOException, InterruptedException {
        final ByteBuffer buffer = ByteBuffer.allocate(4096);
        int read;
        do {
            buffer.clear();
            buffer.put((byte) 0);
            read = in.read(buffer);
            if (read > 0) {
                buffer.flip();
                webSocket.send(ByteString.of(buffer));
            } else if (read == 0) {
                Thread.sleep(50L);
            }
        } while (isAlive.getAsBoolean() && read >= 0);
    }

    private void awaitMoreRequest() {
        this.lock.lock();
        try {
            while (!this.more) {
                this.moreRequested.await();
            }
            this.more = false;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            this.lock.unlock();
        }
    }

    private void writeMessage(WebSocket webSocket, ByteString bytes) {
        this.awaitMoreRequest();
        ++this.messagesRead;
        final ByteBuffer buffer = bytes.asByteBuffer();
        if (this.messagesRead <= 2) {
            this.request();
            return;
        }
        if (!buffer.hasRemaining()) {
            this.closeWebSocket(webSocket, 1002, "Protocol error");
            throw new AzureToolkitRuntimeException("Received an empty message.");
        }
        final byte channel = buffer.get();
        if (channel < 0 || channel > 1) {
            this.closeWebSocket(webSocket, 1002, "Protocol error");
            throw new AzureToolkitRuntimeException("Received a wrong channel from the remote socket.");
        }
        if (channel == 1) {
            this.closeForwarder();
            throw new AzureToolkitRuntimeException("Received an error from the remote socket.");
        }
        if (this.out != null) {
            while (true) {
                try {
                    if (buffer.hasRemaining()) {
                        final int written = this.out.write(buffer);
                        if (written == 0) {
                            Thread.sleep(50L);
                        }
                        continue;
                    }
                    this.request();
                } catch (final InterruptedException | IOException e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    if (this.alive.get()) {
                        this.closeWebSocket(webSocket, 1002, "Protocol error");
                        throw new AzureToolkitRuntimeException("Error while forwarding data from remote to the client.", e);
                    }
                }
                return;
            }
        }
    }
}
