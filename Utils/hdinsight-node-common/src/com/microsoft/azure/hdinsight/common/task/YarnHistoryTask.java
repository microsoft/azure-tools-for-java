/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common.task;

import com.google.common.util.concurrent.FutureCallback;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNodeList;
import org.htmlunit.html.HtmlPage;

public class YarnHistoryTask extends Task<String> {

    protected final IClusterDetail clusterDetail;
    protected final String path;
    private final CredentialsProvider credentialsProvider  =  new BasicCredentialsProvider();
    private static final WebClient WEB_CLIENT = new WebClient();

    public YarnHistoryTask(@NotNull IClusterDetail clusterDetail, @NotNull String path, @NotNull FutureCallback<String> callback) {
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
        WEB_CLIENT.setCredentialsProvider(credentialsProvider);
        HtmlPage htmlPage = WEB_CLIENT.getPage(path);

        // parse pre tag from html response
        // there's only one 'pre' in response
        DomNodeList<DomElement> preTagElements = htmlPage.getElementsByTagName("pre");

        if (preTagElements.size() == 0) {
            throw new HDIException("No logs here or logs not available");
        } else {
            return preTagElements.get(0).asNormalizedText();
        }
    }
}
