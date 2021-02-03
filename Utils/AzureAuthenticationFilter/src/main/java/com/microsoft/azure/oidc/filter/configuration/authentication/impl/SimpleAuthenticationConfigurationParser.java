/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.configuration.authentication.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.oidc.filter.configuration.authentication.AuthenticationConfiguration;
import com.microsoft.azure.oidc.filter.configuration.authentication.AuthenticationConfigurationFactory;
import com.microsoft.azure.oidc.filter.configuration.authentication.AuthenticationConfigurationParser;

public final class SimpleAuthenticationConfigurationParser implements AuthenticationConfigurationParser {
    private static final AuthenticationConfigurationParser INSTANCE = new SimpleAuthenticationConfigurationParser();

    private final AuthenticationConfigurationFactory authenticationConfigurationFactory = SimpleAuthenticationConfigurationFactory
            .getInstance();

    @Override
    public AuthenticationConfiguration parse(final JsonNode node) {
        final List<String> exclusionUriPatterns = new ArrayList<String>();
        final List<String> authorisationUriPatterns = new ArrayList<String>();
        final Map<String, List<String>> authorisationRoleMap = new HashMap<String, List<String>>();
        for (final JsonNode exclusion : node.get("exclusionUriPatterns")) {
            exclusionUriPatterns.add(exclusion.asText());
        }
        for (final JsonNode exclusion : node.get("authorisationUriPatterns")) {
            final String patternString = exclusion.get("uriPattern").asText();
            authorisationUriPatterns.add(patternString);
            authorisationRoleMap.put(patternString, new ArrayList<String>());
            for (final JsonNode role : exclusion.get("roles")) {
                authorisationRoleMap.get(patternString).add(role.asText());
            }
        }
        return authenticationConfigurationFactory.createAuthenticationConfiguration(exclusionUriPatterns,
                authorisationUriPatterns, authorisationRoleMap);
    }

    public static AuthenticationConfigurationParser getInstance() {
        return INSTANCE;
    }
}
