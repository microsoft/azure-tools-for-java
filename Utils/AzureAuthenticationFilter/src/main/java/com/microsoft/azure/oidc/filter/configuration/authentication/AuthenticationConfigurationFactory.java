/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.configuration.authentication;

import java.util.List;
import java.util.Map;

public interface AuthenticationConfigurationFactory {

    AuthenticationConfiguration createAuthenticationConfiguration(List<String> exclusionUriPatternList,
            List<String> authorisationUriPatternList, Map<String, List<String>> authorisationRoleMap);

}
