/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.configuration.authentication.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.microsoft.azure.oidc.filter.configuration.authentication.AuthenticationConfiguration;

final class SimpleAuthenticationConfiguration implements AuthenticationConfiguration {
    private List<String> exclusionUriPatternList;
    private List<String> authorisationUriPatternList;
    private Map<String, List<String>> authorisationRoleMap;

    private List<Pattern> exclusionRegexPatternList;
    private List<Pattern> authorisationRegexPatternList;

    public SimpleAuthenticationConfiguration(final List<String> exclusionUriPatternList,
            final List<String> authorisationUriPatternList, final Map<String, List<String>> authorisationRoleMap) {
        setExclusionUriPatternList(exclusionUriPatternList);
        setAuthorisationUriPatternList(authorisationUriPatternList);
        setAuthorisationRoleMap(authorisationRoleMap);
    }

    private void setExclusionUriPatternList(List<String> exclusionUriPatternList) {
        this.exclusionUriPatternList = exclusionUriPatternList;
        exclusionRegexPatternList = new ArrayList<Pattern>();
        exclusionRegexPatternList.add(Pattern.compile(
                "/javax.faces.resource/*".replaceAll("([^a-zA-Z0-9\\*])", "\\\\$1").replaceAll("\\*", "(\\.\\*)")));
        if (exclusionRegexPatternList == null) {
            return;
        }
        for (final String pattern : exclusionUriPatternList) {
            final String localPattern = pattern.trim();
            if (localPattern.endsWith("*")) {
                exclusionRegexPatternList.add(Pattern
                        .compile(localPattern.replaceAll("([^a-zA-Z0-9\\*])", "\\\\$1").replaceAll("\\*", "(\\.\\*)")));
            } else if (localPattern.startsWith("/")) {
                exclusionRegexPatternList.add(Pattern.compile(
                        localPattern.replaceAll("([^a-zA-Z0-9\\*])", "\\\\$1").replaceAll("\\*", "(\\\\w\\*)")));
            } else {
                exclusionRegexPatternList.add(Pattern
                        .compile(localPattern.replaceAll("([^a-zA-Z0-9\\*])", "\\\\$1").replaceAll("\\*", "(\\.\\*)")));
            }
        }
    }

    private void setAuthorisationUriPatternList(List<String> authorisationUriPatternList) {
        this.authorisationUriPatternList = authorisationUriPatternList;
        authorisationRegexPatternList = new ArrayList<Pattern>();
        if (authorisationUriPatternList == null) {
            return;
        }
        for (final String pattern : authorisationUriPatternList) {
            final String localPattern = pattern.trim();
            if (localPattern.endsWith("*")) {
                authorisationRegexPatternList.add(Pattern
                        .compile(localPattern.replaceAll("([^a-zA-Z0-9\\*])", "\\\\$1").replaceAll("\\*", "(\\.\\*)")));
            } else if (localPattern.startsWith("/")) {
                authorisationRegexPatternList.add(Pattern.compile(
                        localPattern.replaceAll("([^a-zA-Z0-9\\*])", "\\\\$1").replaceAll("\\*", "(\\\\w\\*)")));
            } else {
                authorisationRegexPatternList.add(Pattern
                        .compile(localPattern.replaceAll("([^a-zA-Z0-9\\*])", "\\\\$1").replaceAll("\\*", "(\\.\\*)")));
            }
        }
    }

    private void setAuthorisationRoleMap(Map<String, List<String>> authorisationRoleMap) {
        this.authorisationRoleMap = authorisationRoleMap;
    }

    @Override
    public List<String> getExclusionUriPatternList() {
        return exclusionUriPatternList;
    }

    @Override
    public List<Pattern> getExclusionRegexPatternList() {
        return exclusionRegexPatternList;
    }

    @Override
    public List<String> getAuthorisationUriPatternList() {
        return authorisationUriPatternList;
    }

    @Override
    public List<Pattern> getAuthorisationRegexPatternList() {
        return authorisationRegexPatternList;
    }

    @Override
    public Map<String, List<String>> getAuthorisationRoleMap() {
        return authorisationRoleMap;
    }
}
