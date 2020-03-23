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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.oidc.filter.configuration.algorithm.AlgorithmConfiguration;
import com.microsoft.azure.oidc.filter.configuration.algorithm.AlgorithmConfigurationFactory;
import com.microsoft.azure.oidc.filter.configuration.algorithm.AlgorithmConfigurationParser;

public final class SimpleAlgorithmConfigurationParser implements AlgorithmConfigurationParser {
    private static final AlgorithmConfigurationParser INSTANCE = new SimpleAlgorithmConfigurationParser();

    private final AlgorithmConfigurationFactory algorithmConfigurationFactory = SimpleAlgorithmConfigurationFactory.getInstance();

    @Override
    public AlgorithmConfiguration parse(final JsonNode node) {
        final Map<String, String> algorithmMap = new HashMap<String, String>();
        final Map<String, String> algorithmClassMap = new HashMap<String, String>();
        for(final JsonNode n : node.get("algorithms")) {
            algorithmMap.put(n.get("name").asText(), n.get("javaName").asText());

        }
        for(final JsonNode n : node.get("algorithmClasses")) {
            algorithmClassMap.put(n.get("name").asText(), n.get("className").asText());

        }
        return algorithmConfigurationFactory.createAlgorithmConfiguration(algorithmMap, algorithmClassMap);
    }

    public static AlgorithmConfigurationParser getInstance() {
        return INSTANCE;
    }
}
