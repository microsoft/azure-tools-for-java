/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.openid.keystore.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.oidc.configuration.endpoint.EndPoint;
import com.microsoft.azure.oidc.exception.PreconditionException;
import com.microsoft.azure.oidc.openid.keystore.KeyStoreLoader;

public final class SimpleKeyStoreLoader implements KeyStoreLoader {
    private static final KeyStoreLoader INSTANCE = new SimpleKeyStoreLoader();
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleKeyStoreLoader.class);

    @Override
    public Future<JsonNode> loadAsync(final EndPoint endPoint) {
        if (endPoint == null) {
            throw new PreconditionException("Required parameter is null");
        }
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Future<JsonNode> future = executorService.submit(new Callable<JsonNode>() {
            public JsonNode call() throws Exception {
                return load(endPoint);
            }
        });
        executorService.shutdown();
        return future;
    }

    public JsonNode load(final EndPoint endPoint) {
        if (endPoint == null) {
            throw new PreconditionException("Required parameter is null");
        }
        try {
            final StringBuilder builder = new StringBuilder();
            final URL url = new URL(endPoint.getName());
            final URLConnection connection = url.openConnection();
            try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    builder.append(line);
                }
            }
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(builder.toString(), JsonNode.class);
        } catch (IOException e) {
            LOGGER.error("IO Exception", e);
        } catch (RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public static KeyStoreLoader getInstance() {
        return INSTANCE;
    }
}
