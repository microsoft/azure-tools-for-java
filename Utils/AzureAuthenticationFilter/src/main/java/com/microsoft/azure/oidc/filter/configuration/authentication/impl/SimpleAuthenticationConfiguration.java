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
