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
