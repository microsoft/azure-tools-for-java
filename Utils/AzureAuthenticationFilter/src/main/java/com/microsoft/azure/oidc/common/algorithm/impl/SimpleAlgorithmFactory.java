/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.common.algorithm.impl;

import com.microsoft.azure.oidc.common.algorithm.Algorithm;
import com.microsoft.azure.oidc.common.algorithm.AlgorithmFactory;
import com.microsoft.azure.oidc.exception.PreconditionException;

public final class SimpleAlgorithmFactory implements AlgorithmFactory {
    private static final AlgorithmFactory INSTANCE = new SimpleAlgorithmFactory();

    @Override
    public Algorithm createAlgorithm(final String name) {
        if (name == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return new SimpleAlgorithm(name);
    }

    public static AlgorithmFactory getInstanc() {
        return INSTANCE;
    }
}
