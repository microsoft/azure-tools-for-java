/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.spark.jobs;

import com.jcraft.jsch.JSch;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.sdk.rest.ObjectConvertUtils;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.App;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.AppResponse;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.YarnApplicationResponse;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class YarnRestUtil {
    private static final String YARN_UI_HISTORY_URL = "%s/yarnui/ws/v1/%s";

    private static List<App> getSparkAppFromYarn(@NotNull final IClusterDetail clusterDetail) throws IOException, HDIException {
        final HttpEntity entity = getYarnRestEntity(clusterDetail, "cluster/apps");
        Optional<YarnApplicationResponse> allApps = ObjectConvertUtils.convertEntityToObject(entity, YarnApplicationResponse.class);
        return allApps.orElse(YarnApplicationResponse.EMPTY)
                .getAllApplication()
                .orElse(App.EMPTY_LIST)
                .stream()
                .filter(app -> app.isLivyJob())
                .collect(Collectors.toList());
    }

    private static HttpEntity getYarnRestEntity(@NotNull IClusterDetail clusterDetail, @NotNull String restUrl) throws HDIException, IOException {
        final String url = String.format(YARN_UI_HISTORY_URL, clusterDetail.getConnectionUrl(), restUrl);
        return JobUtils.getEntity(clusterDetail, url);
    }
}
