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

import com.google.common.util.concurrent.FutureCallback;
import com.microsoft.azure.hdinsight.common.HttpResponseWithoutHeader;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;

public class MultiRestTask extends Task<List<String>> {
    protected final IClusterDetail clusterDetail;
    protected final List<String> paths;
    private final CredentialsProvider credentialsProvider =  new BasicCredentialsProvider();

    public MultiRestTask(@NotNull IClusterDetail clusterDetail, @NotNull List<String> paths, @NotNull FutureCallback<List<String>> callback) {
        super(callback);
        this.clusterDetail = clusterDetail;
        this.paths = paths;
        try {
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(clusterDetail.getHttpUserName(), clusterDetail.getHttpPassword()));
        } catch (HDIException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> call() throws Exception {
        CloseableHttpClient httpclient = HttpClients.custom()
                .useSystemProperties()
                .setDefaultCredentialsProvider(credentialsProvider).build();
        List<String> results = new ArrayList<>();
        for(String path: paths) {
            HttpGet httpGet = new HttpGet(path);
            httpGet.addHeader("Content-Type", "application/json");
            CloseableHttpResponse response = httpclient.execute(httpGet);
            int code = response.getStatusLine().getStatusCode();
            if(code == 200 || code == 201) {
                results.add(EntityUtils.toString(response.getEntity()));
            } else {
                throw new HDIException(response.getStatusLine().getReasonPhrase(), code);
            }
        }

        return results;
    }
}
