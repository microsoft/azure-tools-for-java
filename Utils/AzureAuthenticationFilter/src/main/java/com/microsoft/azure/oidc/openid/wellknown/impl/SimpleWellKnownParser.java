/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
