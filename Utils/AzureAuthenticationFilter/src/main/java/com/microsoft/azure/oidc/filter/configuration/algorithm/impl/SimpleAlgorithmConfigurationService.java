/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.configuration.algorithm.impl;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.oidc.filter.configuration.algorithm.AlgorithmConfiguration;
import com.microsoft.azure.oidc.filter.configuration.algorithm.AlgorithmConfigurationLoader;
import com.microsoft.azure.oidc.filter.configuration.algorithm.AlgorithmConfigurationParser;
import com.microsoft.azure.oidc.filter.configuration.algorithm.AlgorithmConfigurationService;

public final class SimpleAlgorithmConfigurationService implements AlgorithmConfigurationService {
    private static final AlgorithmConfigurationService INSTANCE = new SimpleAlgorithmConfigurationService();

    private final AlgorithmConfigurationLoader algorithmConfigurationLoader = SimpleAlgorithmConfigurationLoader
            .getInstance();

    private final AlgorithmConfigurationParser algorithmConfigurationParser = SimpleAlgorithmConfigurationParser
            .getInstance();

    private AlgorithmConfiguration algorithmConfiguration;

    @Override
    public void initialise(final FilterConfig filterConfig, final String parameterName) throws ServletException {
        final JsonNode node = algorithmConfigurationLoader.load(filterConfig, parameterName);
        algorithmConfiguration = algorithmConfigurationParser.parse(node);
    }

    @Override
    public AlgorithmConfiguration get() {
        return algorithmConfiguration;
    }

    public static AlgorithmConfigurationService getInstance() {
        return INSTANCE;
    }
}
