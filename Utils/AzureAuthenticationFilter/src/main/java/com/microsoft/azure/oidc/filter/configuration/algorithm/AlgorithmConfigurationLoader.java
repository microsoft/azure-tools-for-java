/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.configuration.algorithm;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.JsonNode;

public interface AlgorithmConfigurationLoader {

    JsonNode load(FilterConfig filterConfig, String parameterName) throws ServletException;

}
