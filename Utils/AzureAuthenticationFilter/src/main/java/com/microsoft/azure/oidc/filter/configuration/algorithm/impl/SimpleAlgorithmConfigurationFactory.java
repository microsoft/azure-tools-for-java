/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.configuration.algorithm.impl;

import java.util.Map;

import com.microsoft.azure.oidc.filter.configuration.algorithm.AlgorithmConfiguration;
import com.microsoft.azure.oidc.filter.configuration.algorithm.AlgorithmConfigurationFactory;

public final class SimpleAlgorithmConfigurationFactory implements AlgorithmConfigurationFactory {
    private static final AlgorithmConfigurationFactory INSTANCE = new SimpleAlgorithmConfigurationFactory();

    @Override
    public AlgorithmConfiguration createAlgorithmConfiguration(final Map<String, String> algorithmMap,
            final Map<String, String> algorithmClassMap) {
        return new SimpleAlgorithmConfiguration(algorithmMap, algorithmClassMap);
    }

    public static AlgorithmConfigurationFactory getInstance() {
        return INSTANCE;
    }
}
