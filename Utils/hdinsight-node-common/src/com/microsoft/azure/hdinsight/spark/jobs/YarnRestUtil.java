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

package com.microsoft.azure.hdinsight.spark.jobs;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.rest.ObjectConvertUtils;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.App;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.AppResponse;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.YarnApplicationResponse;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.HttpEntity;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class YarnRestUtil {
    private static final String YARN_UI_HISTORY_URL = "%s/yarnui/ws/v1/cluster/%s";

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

    public static App getApp(@NotNull ApplicationKey key) throws HDIException, IOException {
        HttpEntity entity = getYarnRestEntity(key.getClusterDetails(), String.format("/apps/%s", key.getAppId()));
        return ObjectConvertUtils.convertEntityToObject(entity, AppResponse.class).orElseThrow(()-> new HDIException(String.format("get Yarn app %s on cluster %s error", key.getAppId(), key.getClusterDetails().getName()))).getApp();
    }

    private static HttpEntity getYarnRestEntity(@NotNull IClusterDetail clusterDetail, @NotNull String restUrl) throws HDIException, IOException {
        final String url = String.format(YARN_UI_HISTORY_URL, clusterDetail.getConnectionUrl(), restUrl);
        return JobUtils.getEntity(clusterDetail, url);
    }
}
