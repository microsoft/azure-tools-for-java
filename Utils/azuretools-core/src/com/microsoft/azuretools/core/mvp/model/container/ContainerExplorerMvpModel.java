/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.container;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.HashMap;
import java.util.Map;

public class ContainerExplorerMvpModel {

    private static final String URL_PREFIX = "https";
    private static final String REPOSITORY_PATH = "v2/_catalog";
    private static final String TAG_PATH = "v2/%s/tags/list";
    private static final String HEADER_AUTH = "Authorization";
    private static final String INVALID_URL = "The request URL is NULL.";
    private static final String BODY = "body";
    private static final String LINK_HEADER = "link";
    private static final String RESPONSE_FAIL_MSG = "Unexpected response %s. please make sure the admin user is " +
            "enabled and try again";

    private final OkHttpClient sharedClient = new OkHttpClient();

    private ContainerExplorerMvpModel() {
    }

    private static final class ContainerExplorerMvpModelHolder {
        private static final ContainerExplorerMvpModel INSTANCE = new ContainerExplorerMvpModel();
    }

    public static ContainerExplorerMvpModel getInstance() {
        return ContainerExplorerMvpModelHolder.INSTANCE;
    }

    /**
     * list repositories under the given private registry.
     */
    public Map<String, String> listRepositories(@NotNull String serverUrl, @NotNull String username,
                                                @NotNull String password, @Nullable Map<String, String> query)
            throws Exception {
        OkHttpClient client = createRestClient(username, password);
        HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
                .scheme(URL_PREFIX)
                .host(serverUrl)
                .addPathSegment(REPOSITORY_PATH);
        if (query != null) {
            for (String key : query.keySet()) {
                urlBuilder.addQueryParameter(key, query.get(key));
            }
        }
        return getResponse(client, urlBuilder.build());
    }

    /**
     * list tags under the given repository.
     */
    public Map<String, String> listTags(@NotNull String serverUrl, @NotNull String username, @NotNull String password,
                                        @NotNull String repo, @Nullable Map<String, String> query) throws Exception {
        OkHttpClient client = createRestClient(username, password);
        HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
                .scheme(URL_PREFIX)
                .host(serverUrl)
                .addPathSegment(String.format(TAG_PATH, repo));
        if (query != null) {
            for (String key : query.keySet()) {
                urlBuilder.addQueryParameter(key, query.get(key));
            }
        }
        return getResponse(client, urlBuilder.build());
    }

    @NotNull
    private Map<String, String> getResponse(@NotNull OkHttpClient client, HttpUrl url) throws Exception {
        if (url == null) {
            throw new NullPointerException(INVALID_URL);
        }
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                Map<String, String> responseMap = new HashMap<>();
                responseMap.put(BODY, response.body().string());
                responseMap.put(LINK_HEADER, response.header(LINK_HEADER));
                return responseMap;
            } else {
                throw new Exception(String.format(RESPONSE_FAIL_MSG, response));
            }
        }
    }

    @NotNull
    private OkHttpClient createRestClient(@NotNull String username, @NotNull String password) {
        return sharedClient.newBuilder()
                .authenticator((route, response) -> {
                    String credential = Credentials.basic(username, password);
                    if (credential.equals(response.request().header(HEADER_AUTH))) {
                        return null;
                    }
                    return response.request().newBuilder().header(HEADER_AUTH, credential).build();
                })
                .build();
    }
}
