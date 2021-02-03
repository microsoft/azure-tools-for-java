/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
