package com.microsoft.azure.toolkit.ide.common.util.remotedebugging;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class WebSocketBuilder {
    private Request.Builder builder = new Request.Builder();
    private final OkHttpClient httpClient;

    public WebSocketBuilder(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public WebSocketBuilder uri(URI uri) {
        this.builder.url(uri.toString());
        return this;
    }

    public WebSocketBuilder header(String name, String value) {
        this.builder = this.builder.addHeader(name, value);
        return this;
    }

    public CompletableFuture<WebSocket> buildAsync(final PortForwarderWebSocketListener listener) {
        final Request request = this.builder.build();
        this.httpClient.newWebSocket(request, listener);
        return listener.getFuture();
    }
}
