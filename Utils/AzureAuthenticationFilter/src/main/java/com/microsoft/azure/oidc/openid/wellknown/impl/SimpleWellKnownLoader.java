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

package com.microsoft.azure.oidc.openid.wellknown.impl;

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
import com.microsoft.azure.oidc.application.settings.ApplicationSettings;
import com.microsoft.azure.oidc.application.settings.ApplicationSettingsLoader;
import com.microsoft.azure.oidc.application.settings.impl.SimpleApplicationSettingsLoader;
import com.microsoft.azure.oidc.openid.wellknown.WellKnownLoader;

public final class SimpleWellKnownLoader implements WellKnownLoader {
    private static final WellKnownLoader INSTANCE = new SimpleWellKnownLoader();
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleWellKnownLoader.class);

    private final ApplicationSettingsLoader applicationSettingsLoader = SimpleApplicationSettingsLoader.getInstance();

    @Override
    public Future<JsonNode> loadAsync() {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Future<JsonNode> future = executorService.submit(new Callable<JsonNode>() {
            public JsonNode call() throws Exception {
                return load();
            }
        });
        executorService.shutdown();
        return future;
    }

    public JsonNode load() {
        try {
            final ApplicationSettings applicationSettings = applicationSettingsLoader.load();
            final StringBuilder builder = new StringBuilder();
            final URL url = new URL(
                    String.format("https://login.microsoftonline.com/%s/v2.0/.well-known/openid-configuration?p=%s",
                            applicationSettings.getTenant(), applicationSettings.getOIDCPolicy()));
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

    public static WellKnownLoader getInstance() {
        return INSTANCE;
    }
}
