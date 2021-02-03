/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common.task;

import com.google.common.util.concurrent.FutureCallback;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;

public class LivyTask extends Task<String> {
    private static Logger LOG = Logger.getLogger(LivyTask.class);

    protected final IClusterDetail clusterDetail;
    protected final String path;
    private final CredentialsProvider credentialsProvider =  new BasicCredentialsProvider();

    public LivyTask(@NotNull IClusterDetail clusterDetail, @NotNull String path, @NotNull FutureCallback<String> callback ) {
        super(callback);
        this.clusterDetail = clusterDetail;
        this.path = path;
        this.callback = callback;
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
        int code = response.getStatusLine().getStatusCode();

        HttpEntity httpEntity = response.getEntity();

        return IOUtils.toString(httpEntity.getContent(), Charset.forName("utf-8"));
    }
}
