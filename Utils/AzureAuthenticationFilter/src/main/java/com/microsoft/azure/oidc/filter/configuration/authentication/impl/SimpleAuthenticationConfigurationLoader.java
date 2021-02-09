/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.configuration.authentication.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.oidc.filter.configuration.authentication.AuthenticationConfigurationLoader;

public final class SimpleAuthenticationConfigurationLoader implements AuthenticationConfigurationLoader {
    private static final AuthenticationConfigurationLoader INSTANCE = new SimpleAuthenticationConfigurationLoader();

    @Override
    public JsonNode load(final FilterConfig filterConfig, final String parameterName) throws ServletException {
        final String authenticationConfigurationFileName = filterConfig.getInitParameter(parameterName);
        final InputStream is = filterConfig.getServletContext()
                .getResourceAsStream(authenticationConfigurationFileName);
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            final StringBuilder builder = new StringBuilder();
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                builder.append(line);
            }
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(builder.toString(), JsonNode.class);
        } catch (final IOException e) {
            throw new ServletException(e.getMessage(), e);
        }
    }

    public static AuthenticationConfigurationLoader getInstance() {
        return INSTANCE;
    }
}
