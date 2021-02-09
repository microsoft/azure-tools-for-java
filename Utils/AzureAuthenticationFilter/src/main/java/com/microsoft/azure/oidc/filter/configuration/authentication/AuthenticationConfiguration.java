/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.configuration.authentication;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public interface AuthenticationConfiguration {

    List<String> getExclusionUriPatternList();

    List<Pattern> getExclusionRegexPatternList();

    List<String> getAuthorisationUriPatternList();

    List<Pattern> getAuthorisationRegexPatternList();

    Map<String, List<String>> getAuthorisationRoleMap();

}
