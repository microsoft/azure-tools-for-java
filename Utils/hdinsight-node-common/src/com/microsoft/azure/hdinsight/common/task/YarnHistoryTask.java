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
package com.microsoft.azure.hdinsight.common.task;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.util.concurrent.FutureCallback;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

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
            return preTagElements.get(0).asText();
        }
    }
}
