/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.configuration.impl;

import java.util.List;
import java.util.Map;

import com.microsoft.azure.oidc.common.algorithm.Algorithm;
import com.microsoft.azure.oidc.common.issuer.Issuer;
import com.microsoft.azure.oidc.common.name.Name;
import com.microsoft.azure.oidc.configuration.Configuration;
import com.microsoft.azure.oidc.configuration.ConfigurationFactory;
import com.microsoft.azure.oidc.configuration.endpoint.EndPoint;
import com.microsoft.azure.oidc.configuration.key.Key;
import com.microsoft.azure.oidc.exception.PreconditionException;

public final class SimpleConfigurationFactory implements ConfigurationFactory {
    private static final ConfigurationFactory INSTANCE = new SimpleConfigurationFactory();

    @Override
    public Configuration createConfiguration(final List<Algorithm> algorithms, final Map<Name, Key> keys,
            final Issuer issuer, final EndPoint authenticationEndPoint, final EndPoint logoutEndPoint) {
        if (algorithms == null || keys == null || issuer == null || authenticationEndPoint == null
                || logoutEndPoint == null) {
            throw new PreconditionException("Required parameter is null");
        }
        if (algorithms.isEmpty()) {
            throw new PreconditionException("Algorithm list is empty");
        }
        return new SimpleConfiguration(algorithms, keys, issuer, authenticationEndPoint, logoutEndPoint);
    }

    public static ConfigurationFactory getInstance() {
        return INSTANCE;
    }
}
