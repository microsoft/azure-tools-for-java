package com.microsoft.azure.toolkit.intellij.azuremcp;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.RequestBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GithubClientTest {
    @Test
    void retriesWhenHandlerThrowsUncheckedIOException() throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try (final GithubClient client = new GithubClient()) {
            final AtomicInteger attempts = new AtomicInteger();
            final String result = client.executeWithRetry(
                    RequestBuilder.get().setUri("http://127.0.0.1:" + server.getAddress().getPort() + "/").build(),
                    response -> {
                        if (attempts.incrementAndGet() == 1) {
                            throw new UncheckedIOException(new IOException("unexpected payload"));
                        }
                        return "ok";
                    },
                    "test-event");
            assertEquals("ok", result);
            assertEquals(2, attempts.get());
        } finally {
            server.stop(0);
        }
    }
}