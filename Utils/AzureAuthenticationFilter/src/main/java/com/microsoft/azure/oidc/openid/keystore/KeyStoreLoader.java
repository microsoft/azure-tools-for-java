/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.openid.keystore;

import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.oidc.configuration.endpoint.EndPoint;

public interface KeyStoreLoader {

    Future<JsonNode> loadAsync(EndPoint endPoint);

}
