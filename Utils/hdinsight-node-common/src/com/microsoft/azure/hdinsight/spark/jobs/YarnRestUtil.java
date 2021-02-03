/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
