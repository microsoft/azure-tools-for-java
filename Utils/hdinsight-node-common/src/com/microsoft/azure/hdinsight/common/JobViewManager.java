/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;

import com.microsoft.azure.hdinsight.sdk.rest.spark.Application;
import com.microsoft.azure.hdinsight.spark.jobs.ApplicationKey;
import com.microsoft.azure.hdinsight.spark.jobs.framework.JobViewPanel;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import javafx.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JobViewManager {
    private static Map<String, IClusterDetail> jobViewPanelMap = new ConcurrentHashMap<>();
    private static Map<String, List<Application>> sparkApplicationMap = new ConcurrentHashMap<>();

    public static void registerApplications(@NotNull String clusterName, @NotNull List<Application> apps) {
        sparkApplicationMap.put(clusterName, apps);
    }

    public static Application getCachedApp(@NotNull ApplicationKey key) {
        final String clusterName = key.getClusterDetails().getName();
        final String appId = key.getAppId();
        if(sparkApplicationMap.containsKey(clusterName)) {
            return sparkApplicationMap.get(clusterName)
                    .stream()
                    .filter(app-> app.getId().equalsIgnoreCase(appId))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public static void registerJovViewNode(@NotNull String clusterName, @NotNull IClusterDetail clusterDetail) {
        jobViewPanelMap.put(clusterName, clusterDetail);
    }

    @Nullable
    public static IClusterDetail getCluster(@NotNull String clusterName) {
        return jobViewPanelMap.get(clusterName);
    }

    public static void unRegisterJobView(@NotNull String clusterName) {
        jobViewPanelMap.remove(clusterName);
    }
}
