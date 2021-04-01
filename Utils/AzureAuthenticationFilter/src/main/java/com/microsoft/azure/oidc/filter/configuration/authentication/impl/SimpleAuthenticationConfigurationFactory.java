/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.configuration.authentication.impl;

import java.util.List;
import java.util.Map;

import com.microsoft.azure.oidc.filter.configuration.authentication.AuthenticationConfiguration;
import com.microsoft.azure.oidc.filter.configuration.authentication.AuthenticationConfigurationFactory;

public final class SimpleAuthenticationConfigurationFactory implements AuthenticationConfigurationFactory {
    private static final AuthenticationConfigurationFactory INSTANCE = new SimpleAuthenticationConfigurationFactory();

    @Override
    public AuthenticationConfiguration createAuthenticationConfiguration(final List<String> exclusionUriPatternList,
            final List<String> authorisationUriPatternList, final Map<String, List<String>> authorisationRoleMap) {
        return new SimpleAuthenticationConfiguration(exclusionUriPatternList, authorisationUriPatternList, authorisationRoleMap);
    }

    public static AuthenticationConfigurationFactory getInstance() {
        return INSTANCE;
    }
}
