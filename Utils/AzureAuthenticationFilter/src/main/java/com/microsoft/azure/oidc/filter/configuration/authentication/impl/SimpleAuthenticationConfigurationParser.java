/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
