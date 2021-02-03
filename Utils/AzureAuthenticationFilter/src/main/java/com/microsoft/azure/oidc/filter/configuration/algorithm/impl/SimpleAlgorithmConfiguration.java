/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.configuration.algorithm.impl;

import java.util.Map;

import com.microsoft.azure.oidc.filter.configuration.algorithm.AlgorithmConfiguration;

final class SimpleAlgorithmConfiguration implements AlgorithmConfiguration {
    private Map<String, String> algorithmMap;
    private Map<String, String> algorithmClassMap;

    public SimpleAlgorithmConfiguration(final Map<String, String> algorithmMap, final Map<String, String> algorithmClassMap) {
        this.algorithmMap = algorithmMap;
        this.algorithmClassMap = algorithmClassMap;
    }

    @Override
    public Map<String, String> getAlgorithmMap() {
        return algorithmMap;
    }

    @Override
    public Map<String, String> getAlgorithmClassMap() {
        return algorithmClassMap;
    }
}
