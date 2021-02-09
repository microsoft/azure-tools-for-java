/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.openid.wellknown;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.oidc.common.algorithm.Algorithm;
import com.microsoft.azure.oidc.common.issuer.Issuer;
import com.microsoft.azure.oidc.configuration.endpoint.EndPoint;

public interface WellKnownParser {

    Issuer getIssuer(JsonNode node);

    EndPoint getKeyStoreEndPoint(JsonNode node);

    List<Algorithm> getAlgorithms(JsonNode node);

    EndPoint getAuthenticationEndPoint(JsonNode node);

    EndPoint getLogoutEndPoint(JsonNode node);
}
