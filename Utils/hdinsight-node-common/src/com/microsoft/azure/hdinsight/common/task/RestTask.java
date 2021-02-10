/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common.task;

import com.google.common.util.concurrent.FutureCallback;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.common.HttpResponseWithoutHeader;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RestTask extends Task<String> {

    protected final IClusterDetail clusterDetail;
    protected final String path;
    private final CredentialsProvider credentialsProvider =  new BasicCredentialsProvider();

    public RestTask(@NotNull IClusterDetail clusterDetail, @NotNull String path, @NotNull FutureCallback<String> callback) {
        super(callback);
        this.clusterDetail = clusterDetail;
        this.path = path;
        try {
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(clusterDetail.getHttpUserName(), clusterDetail.getHttpPassword()));
        } catch (HDIException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String call() throws Exception {
        CloseableHttpClient httpclient = HttpClients.custom()
                .useSystemProperties()
                .setDefaultCredentialsProvider(credentialsProvider).build();
        HttpGet httpGet = new HttpGet(path);
        httpGet.addHeader("Content-Type", "application/json");

        CloseableHttpResponse response = httpclient.execute(httpGet);
        HttpResponseWithoutHeader header = getResultFromHttpResponse(response);
        if(header.getStatusCode() == 200 || header.getStatusCode() == 201) {
            return header.getMessage();
        } else {
            throw new HDIException(header.getReason(), header.getStatusCode());
        }
    }

    private static HttpResponseWithoutHeader getResultFromHttpResponse(@NotNull CloseableHttpResponse response) throws IOException {
        int code = response.getStatusLine().getStatusCode();
        String reason = response.getStatusLine().getReasonPhrase();
        HttpEntity entity = response.getEntity();
        try (InputStream inputStream = entity.getContent()) {
            String response_content = getResultFromInputStream(inputStream);
            return new HttpResponseWithoutHeader(code, response_content, reason);
        }
    }

    private static String getResultFromInputStream(@NotNull InputStream inputStream) throws IOException {
//      change string buffer to string builder for thread-safe
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }
}
