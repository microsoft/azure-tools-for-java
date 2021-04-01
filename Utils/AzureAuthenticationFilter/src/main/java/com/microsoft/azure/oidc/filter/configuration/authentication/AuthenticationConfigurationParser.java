/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.configuration.authentication;

import com.fasterxml.jackson.databind.JsonNode;

public interface AuthenticationConfigurationParser {

    AuthenticationConfiguration parse(JsonNode node);

}
