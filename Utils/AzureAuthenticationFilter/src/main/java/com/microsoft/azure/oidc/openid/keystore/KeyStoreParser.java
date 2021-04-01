/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.openid.keystore;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.oidc.common.name.Name;
import com.microsoft.azure.oidc.configuration.key.Key;

public interface KeyStoreParser {

    Map<Name,Key> getKeys(JsonNode node);

}
