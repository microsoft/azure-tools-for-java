/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.configuration.authentication.impl;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.oidc.filter.configuration.authentication.AuthenticationConfiguration;
import com.microsoft.azure.oidc.filter.configuration.authentication.AuthenticationConfigurationLoader;
import com.microsoft.azure.oidc.filter.configuration.authentication.AuthenticationConfigurationParser;
import com.microsoft.azure.oidc.filter.configuration.authentication.AuthenticationConfigurationService;

public final class SimpleAuthenticationConfigurationService implements AuthenticationConfigurationService {
    private static final AuthenticationConfigurationService INSTANCE = new SimpleAuthenticationConfigurationService();

    private final AuthenticationConfigurationLoader authenticationConfigurationLoader = SimpleAuthenticationConfigurationLoader
            .getInstance();

    private final AuthenticationConfigurationParser authenticationConfigurationParser = SimpleAuthenticationConfigurationParser
            .getInstance();

    private AuthenticationConfiguration authenticationConfiguration;

    @Override
    public void initialise(final FilterConfig filterConfig, final String parameterName) throws ServletException {
        final JsonNode node = authenticationConfigurationLoader.load(filterConfig, parameterName);
        authenticationConfiguration = authenticationConfigurationParser.parse(node);
    }

    @Override
    public AuthenticationConfiguration get() {
        return authenticationConfiguration;
    }

    public static AuthenticationConfigurationService getInstance() {
        return INSTANCE;
    }
}
