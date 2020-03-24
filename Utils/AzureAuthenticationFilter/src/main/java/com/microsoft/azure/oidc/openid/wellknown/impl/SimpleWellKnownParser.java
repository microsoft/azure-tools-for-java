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

package com.microsoft.azure.oidc.openid.wellknown.impl;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.oidc.common.algorithm.Algorithm;
import com.microsoft.azure.oidc.common.algorithm.AlgorithmFactory;
import com.microsoft.azure.oidc.common.algorithm.impl.SimpleAlgorithmFactory;
import com.microsoft.azure.oidc.common.issuer.Issuer;
import com.microsoft.azure.oidc.common.issuer.IssuerFactory;
import com.microsoft.azure.oidc.common.issuer.impl.SimpleIssuerFactory;
import com.microsoft.azure.oidc.configuration.endpoint.EndPoint;
import com.microsoft.azure.oidc.configuration.endpoint.EndPointFactory;
import com.microsoft.azure.oidc.configuration.endpoint.impl.SimpleEndPointFactory;
import com.microsoft.azure.oidc.exception.PreconditionException;
import com.microsoft.azure.oidc.openid.wellknown.WellKnownParser;

public final class SimpleWellKnownParser implements WellKnownParser {
    private static final WellKnownParser INSTANCE = new SimpleWellKnownParser();

    private final IssuerFactory issuerFactory = SimpleIssuerFactory.getInstance();

    private final AlgorithmFactory algorithmFactory = SimpleAlgorithmFactory.getInstanc();

    private final EndPointFactory endPointFactory = SimpleEndPointFactory.getInstance();

    @Override
    public Issuer getIssuer(JsonNode node) {
        if (node == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return issuerFactory.createIssuer(node.get("issuer").asText());
    }

    @Override
    public EndPoint getKeyStoreEndPoint(JsonNode node) {
        if (node == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return endPointFactory.createEndPoint(node.get("jwks_uri").asText());
    }

    @Override
    public List<Algorithm> getAlgorithms(JsonNode node) {
        if (node == null) {
            throw new PreconditionException("Required parameter is null");
        }
        final List<Algorithm> algorithms = new ArrayList<Algorithm>();
        for (final JsonNode n : node.get("id_token_signing_alg_values_supported")) {
            final Algorithm algorithm = algorithmFactory.createAlgorithm(n.asText());
            algorithms.add(algorithm);
        }
        return algorithms;
    }

    @Override
    public EndPoint getAuthenticationEndPoint(JsonNode node) {
        if (node == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return endPointFactory.createEndPoint(node.get("authorization_endpoint").asText());
    }

    @Override
    public EndPoint getLogoutEndPoint(JsonNode node) {
        if (node == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return endPointFactory.createEndPoint(node.get("end_session_endpoint").asText());
    }

    public static WellKnownParser getInstance() {
        return INSTANCE;
    }
}
