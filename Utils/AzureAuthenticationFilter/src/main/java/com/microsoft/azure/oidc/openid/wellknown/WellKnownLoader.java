/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.openid.wellknown;

import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.JsonNode;

public interface WellKnownLoader {

    Future<JsonNode> loadAsync();

}
