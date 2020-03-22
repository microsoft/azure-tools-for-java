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
package com.microsoft.azure.oidc.configuration.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.oidc.common.algorithm.Algorithm;
import com.microsoft.azure.oidc.common.issuer.Issuer;
import com.microsoft.azure.oidc.common.name.Name;
import com.microsoft.azure.oidc.configuration.Configuration;
import com.microsoft.azure.oidc.configuration.ConfigurationFactory;
import com.microsoft.azure.oidc.configuration.ConfigurationLoader;
import com.microsoft.azure.oidc.configuration.endpoint.EndPoint;
import com.microsoft.azure.oidc.configuration.key.Key;
import com.microsoft.azure.oidc.future.FutureHelper;
import com.microsoft.azure.oidc.future.impl.SimpleFutureHelper;
import com.microsoft.azure.oidc.openid.keystore.KeyStoreLoader;
import com.microsoft.azure.oidc.openid.keystore.KeyStoreParser;
import com.microsoft.azure.oidc.openid.keystore.impl.SimpleKeyStoreLoader;
import com.microsoft.azure.oidc.openid.keystore.impl.SimpleKeyStoreParser;
import com.microsoft.azure.oidc.openid.wellknown.WellKnownLoader;
import com.microsoft.azure.oidc.openid.wellknown.WellKnownParser;
import com.microsoft.azure.oidc.openid.wellknown.impl.SimpleWellKnownParser;
import com.microsoft.azure.oidc.openid.wellknown.impl.SimpleWellKnownLoader;

public final class SimpleConfigurationLoader implements ConfigurationLoader {
    private static final ConfigurationLoader INSTANCE = new SimpleConfigurationLoader();
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleConfigurationLoader.class);

    private final WellKnownLoader wellKnownLoader = SimpleWellKnownLoader.getInstance();

    private final WellKnownParser wellKnownParser = SimpleWellKnownParser.getInstance();

    private final KeyStoreLoader keyStoreLoader = SimpleKeyStoreLoader.getInstance();

    private final KeyStoreParser keyStoreParser = SimpleKeyStoreParser.getInstance();

    private final ConfigurationFactory configurationFactory = SimpleConfigurationFactory.getInstance();

    private final FutureHelper futureHelper = SimpleFutureHelper.getInstance();

    @Override
    public Future<Configuration> loadAsync() {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Future<Configuration> future = executorService.submit(new Callable<Configuration>() {
            public Configuration call() throws Exception {
                return load();
            }
        });
        executorService.shutdown();
        return future;
    }

    public Configuration load() {
        try {
            final JsonNode wellKnownNode = futureHelper.getResult(wellKnownLoader.loadAsync());
            if (wellKnownNode == null) {
                LOGGER.error("Error loading metadata");
                return null;
            }
            final List<Algorithm> algorithms = wellKnownParser.getAlgorithms(wellKnownNode);
            final EndPoint authenticationEndPoint = wellKnownParser.getAuthenticationEndPoint(wellKnownNode);
            final EndPoint keyStoreEndPoint = wellKnownParser.getKeyStoreEndPoint(wellKnownNode);
            final EndPoint logoutEndPoint = wellKnownParser.getLogoutEndPoint(wellKnownNode);
            final Issuer issuer = wellKnownParser.getIssuer(wellKnownNode);
            final JsonNode keyStoreNode = futureHelper.getResult(keyStoreLoader.loadAsync(keyStoreEndPoint));
            if (keyStoreNode == null) {
                LOGGER.error("Error loading keystore");
                return null;
            }
            final Map<Name, Key> keys = keyStoreParser.getKeys(keyStoreNode);
            return configurationFactory.createConfiguration(algorithms, keys, issuer, authenticationEndPoint,
                    logoutEndPoint);
        } catch (RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    public static ConfigurationLoader getInstance() {
        return INSTANCE;
    }
}
