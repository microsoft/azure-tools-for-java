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

import com.microsoft.azure.oidc.common.algorithm.Algorithm;
import com.microsoft.azure.oidc.common.issuer.Issuer;
import com.microsoft.azure.oidc.common.name.Name;
import com.microsoft.azure.oidc.configuration.Configuration;
import com.microsoft.azure.oidc.configuration.endpoint.EndPoint;
import com.microsoft.azure.oidc.configuration.key.Key;
import com.microsoft.azure.oidc.exception.PreconditionException;

final class SimpleConfiguration implements Configuration {
    private final Map<Name, Key> keys;
    private final List<Algorithm> algorithms;
    private final Issuer issuer;
    private final EndPoint authenticationEndPoint;
    private final EndPoint logoutEndPoint;

    SimpleConfiguration(final List<Algorithm> algorithms, final Map<Name, Key> keys, final Issuer issuer,
            final EndPoint authenticationEndPoint, final EndPoint logoutEndPoint) {
        if (algorithms == null || keys == null || issuer == null || authenticationEndPoint == null
                || logoutEndPoint == null) {
            throw new PreconditionException("Required parameter is null");
        }
        if (algorithms.isEmpty()) {
            throw new PreconditionException("Algorithm list is empty");
        }
        this.algorithms = algorithms;
        this.keys = keys;
        this.issuer = issuer;
        this.authenticationEndPoint = authenticationEndPoint;
        this.logoutEndPoint = logoutEndPoint;
    }

    @Override
    public EndPoint getLogoutEndPoint() {
        return logoutEndPoint;
    }

    @Override
    public List<Algorithm> getAlgorithms() {
        return algorithms;
    }

    @Override
    public Map<Name, Key> getKeys() {
        return keys;
    }

    @Override
    public Key getKey(final Name name) {
        return getKeys().get(name);
    }

    @Override
    public Issuer getIssuer() {
        return issuer;
    }

    @Override
    public EndPoint getAuthenticationEndPoint() {
        return authenticationEndPoint;
    }
}
