/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.samples.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class GithubOrganization {
    private static final ObjectMapper JSON_MAPPER = new JsonMapper()
        .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
        .configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    public static final int DEFAULT_PAGE_SIZE = 10;

    @Nonnull
    private final String searchUrl;

    public GithubOrganization(@Nonnull String organization) {
        this.searchUrl = "https://api.github.com/search/repositories?q=language:Java+org:" + URLEncoder.encode(organization, StandardCharsets.UTF_8);
    }

    public SearchResult search(@Nullable String keyword) {
        return this.search(keyword, 1);
    }

    public SearchResult search(@Nullable String keyword, int page) {
        return this.search(keyword, page, DEFAULT_PAGE_SIZE);
    }

    public SearchResult search(@Nullable String keyword, int page, int pageSize) {
        final StringBuilder url = new StringBuilder(this.searchUrl);
        if (!StringUtils.isBlank(keyword)) {
            url.append("+").append(URLEncoder.encode(keyword, StandardCharsets.UTF_8));
        }
        url.append("&page=").append(page);
        url.append("&per_page=").append(pageSize);
        try {
            return JSON_MAPPER.readerFor(SearchResult.class).readValue(new URL(url.toString()));
        } catch (final IOException exception) {
            AzureMessager.getMessager().error(exception);
            return new SearchResult(0, true, Collections.emptyList());
        }
    }
}

