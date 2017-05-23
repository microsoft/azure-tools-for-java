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

public class JobViewManager {
    private static Map<String, IClusterDetail> jobViewPanelMap = new HashMap<>();
    private static Map<String, List<Application>> sparkApplicationMap = new HashMap<>();

    public synchronized static void registerApplications(@NotNull String clusterName, @NotNull List<Application> apps) {
        sparkApplicationMap.put(clusterName, apps);
    }

    public synchronized static Application getCachedApp(@NotNull ApplicationKey key) {
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

    public synchronized static void registerJovViewNode(@NotNull String uuid, @NotNull IClusterDetail clusterDetail) {
        jobViewPanelMap.put(uuid, clusterDetail);
    }

    @Nullable
    public synchronized static IClusterDetail getCluster(@NotNull String clusterName) {
        return jobViewPanelMap.get(clusterName);
    }

    public synchronized static void unRegisterJobView(@NotNull String uuid) {
        jobViewPanelMap.remove(uuid);
    }
}
