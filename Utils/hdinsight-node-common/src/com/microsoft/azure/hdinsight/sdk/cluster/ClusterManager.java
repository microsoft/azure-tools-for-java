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
package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightNewAPI.ClusterOperationNewAPIImpl;
import com.microsoft.azure.hdinsight.sdk.common.AggregatedException;
import com.microsoft.azure.hdinsight.sdk.common.CommonRunnable;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClusterManager {

    private final int MAX_CONCURRENT = 5;
    private final int TIME_OUT = 5 * 60;

    // Singleton Instance
    private static ClusterManager instance = null;

    public static ClusterManager getInstance() {
        if (instance == null) {
            synchronized (ClusterManager.class) {
                if (instance == null) {
                    instance = new ClusterManager();
                }
            }
        }

        return instance;
    }

    private ClusterManager() {
    }


    /**
     * get hdinsight detailed cluster info list
     *
     * @param subscriptions
     * @return detailed cluster info list
     * @throws AggregatedException
     */
    public synchronized List<IClusterDetail> getHDInsightClusers(
            List<SubscriptionDetail> subscriptions) throws AggregatedException {

        return getClusterDetails(subscriptions);
    }

    /**
     * get hdinsight detailed cluster info list with specific cluster type: Spark and RServer
     *
     * @param subscriptions
     * @return detailed cluster info list with specific cluster type
     * @throws AggregatedException
     */
    public synchronized List<IClusterDetail> getHDInsightClustersWithSpecificType(
            List<SubscriptionDetail> subscriptions,
            String osType) throws AggregatedException {
        List<IClusterDetail> clusterDetailList = getClusterDetails(subscriptions);

        Map<String, IClusterDetail> filterClusterDetailMap = new HashMap<>();
        for (IClusterDetail clusterDetail : clusterDetailList) {
            ClusterType clusterType = clusterDetail.getType();
            String myOsType = clusterDetail.getOSType();
            if (clusterType.equals(ClusterType.rserver) || clusterType.equals(ClusterType.spark)) {

                // remove Windows cluster
               if (myOsType != null && osType != null && !myOsType.equalsIgnoreCase(osType)) {
                   continue;
               }
               filterClusterDetailMap.put(clusterDetail.getName(), clusterDetail);
            }
        }

        List<IClusterDetail> filterClusterDetailList = new ArrayList<>();
        filterClusterDetailList.addAll(filterClusterDetailMap.values());

        return filterClusterDetailList;
    }

    public boolean isHDInsightNewSDKEnabled() {
        return DefaultLoader.getIdeHelper().isApplicationPropertySet(CommonConst.ENABLE_HDINSIGHT_NEW_SDK)
                && Boolean.valueOf(DefaultLoader.getIdeHelper().getApplicationProperty(CommonConst.ENABLE_HDINSIGHT_NEW_SDK));
    }

    private boolean isProbeNewApiSucceed(
            @NotNull ClusterOperationNewAPIImpl clusterOperation,
            @NotNull String clusterId) throws IOException {
        return clusterOperation.isProbeGetConfigurationSucceed(clusterId)
                        .toBlocking()
                        .singleOrDefault(false);
    }

    private List<IClusterDetail> getClusterDetails(List<SubscriptionDetail> subscriptions) throws AggregatedException {
        ExecutorService taskExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT);
        final List<IClusterDetail> cachedClusterList = new ArrayList<>();
        final List<Exception> aggregateExceptions = new ArrayList<>();

        for (SubscriptionDetail subscription : subscriptions) {
            taskExecutor.execute(new CommonRunnable<SubscriptionDetail, Exception>(subscription) {
                @Override
                public void runSpecificParameter(SubscriptionDetail parameter) throws IOException, HDIException, AzureCmdException {
                    IClusterOperation clusterOperation = new ClusterOperationImpl();
                    List<ClusterRawInfo> clusterRawInfoList = clusterOperation.listCluster(parameter);
                    if (clusterRawInfoList != null) {
                        for (ClusterRawInfo item : clusterRawInfoList) {
                            IClusterDetail tempClusterDetail = null;
                            ClusterOperationNewAPIImpl probeClusterNewApiOperation = new ClusterOperationNewAPIImpl(parameter);
                            if (isHDInsightNewSDKEnabled()
                                    && isProbeNewApiSucceed(probeClusterNewApiOperation, item.getId())) {
                                tempClusterDetail = new ClusterDetail(parameter, item, probeClusterNewApiOperation);
                            } else {
                                tempClusterDetail = new ClusterDetail(parameter, item, new ClusterOperationImpl());
                            }

                            synchronized (ClusterManager.class) {
                                cachedClusterList.add(tempClusterDetail);
                            }
                        }
                    }
                }

                @Override
                public void exceptionHandle(Exception e) {
                    synchronized (aggregateExceptions) {
                        aggregateExceptions.add(e);
                    }
                }
            });
        }

        taskExecutor.shutdown();
        try {
            taskExecutor.awaitTermination(TIME_OUT, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            aggregateExceptions.add(exception);
        }

        if (aggregateExceptions.size() > 0) {
            throw new AggregatedException(aggregateExceptions);
        }

        return cachedClusterList;
    }
}