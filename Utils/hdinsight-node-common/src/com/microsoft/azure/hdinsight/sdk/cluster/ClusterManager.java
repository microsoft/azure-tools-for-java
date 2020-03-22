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
package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightNewAPI.ClusterOperationNewAPIImpl;
import com.microsoft.azure.hdinsight.sdk.common.AuthType;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ClusterManager implements ILogger {
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

    private List<ClusterRawInfo> deduplicateClusters(@NotNull List<ClusterRawInfo> clusters) {
        Set<String> clusterNameSet = new HashSet<>(clusters.size());
        List<ClusterRawInfo> resultClusters = new ArrayList<>(clusters.size());
        clusters.forEach(clusterRawInfo -> {
            // If we try to add an element that already exists to a set, it will return false
            if (clusterNameSet.add(clusterRawInfo.getName())) {
                resultClusters.add(clusterRawInfo);
            }
        });
        return resultClusters;
    }

    /**
     * get hdinsight detailed cluster info list with specific cluster type: Spark and RServer
     *
     * @param subscriptions
     * @return detailed cluster info list with specific cluster type
     */
    public synchronized Observable<List<ClusterDetail>> getHDInsightClustersWithSpecificType(
            List<SubscriptionDetail> subscriptions,
            String osType) {
        return Observable.from(subscriptions)
                .flatMap(subscriptionDetail ->
                        Observable.fromCallable(() ->
                                new ClusterOperationImpl().listCluster(subscriptionDetail))
                                // Run time-consuming list clusters job in IO thread
                                .subscribeOn(Schedulers.io())
                                // Remove duplicate clusters that share the same cluster name
                                .map(this::deduplicateClusters)
                                .flatMap(Observable::from)
                                // Extract RServer and Spark Cluster with required OS type we need
                                .filter(clusterRawInfo -> {
                                    ClusterType rawClusterType = ClusterDetail.getType(clusterRawInfo);
                                    String rawOsType = ClusterDetail.getOSType(clusterRawInfo);
                                    return (rawClusterType.equals(ClusterType.rserver)
                                            || rawClusterType.equals(ClusterType.spark))
                                                && StringUtils.equalsIgnoreCase(rawOsType, osType);
                                })
                                .flatMap(clusterRawInfo -> {
                                    ClusterOperationNewAPIImpl probeClusterNewApiOperation = new ClusterOperationNewAPIImpl(subscriptionDetail);
                                    if (isHDInsightNewSDKEnabled()) {
                                        return isProbeNewApiSucceed(probeClusterNewApiOperation, clusterRawInfo)
                                                // Run the time-consuming probe job concurrently in IO thread
                                                .subscribeOn(Schedulers.io())
                                                .map(isProbeSucceed -> isProbeSucceed
                                                        ? (isMfaEspCluster(clusterRawInfo)
                                                            ? new MfaClusterDetail(subscriptionDetail, clusterRawInfo, probeClusterNewApiOperation)
                                                            : new ClusterDetail(subscriptionDetail, clusterRawInfo, probeClusterNewApiOperation))
                                                        : new ClusterDetail(subscriptionDetail, clusterRawInfo, new ClusterOperationImpl()));
                                    } else {
                                        return Observable.just(new ClusterDetail(subscriptionDetail, clusterRawInfo, new ClusterOperationImpl()));
                                    }
                                })
                )
                .doOnNext(clusterDetail -> {
                    String debugMsg = String.format("Thread: %s. Sub: %s. Cluster: %s",
                            Thread.currentThread().getName(),
                            clusterDetail.getSubscription().getSubscriptionName(),
                            clusterDetail.getName());
                    log().info(debugMsg);
                })
                .toList();
    }

    public boolean isHDInsightNewSDKEnabled() {
        return DefaultLoader.getIdeHelper().isApplicationPropertySet(CommonConst.ENABLE_HDINSIGHT_NEW_SDK)
                && Boolean.valueOf(DefaultLoader.getIdeHelper().getApplicationProperty(CommonConst.ENABLE_HDINSIGHT_NEW_SDK));
    }

    private Observable<Boolean> isProbeNewApiSucceed(
            @NotNull ClusterOperationNewAPIImpl clusterOperation,
            @NotNull ClusterRawInfo clusterRawInfo) {
        return clusterOperation.isProbeGetConfigurationSucceed(clusterRawInfo);
    }

    public boolean isMfaEspCluster(ClusterRawInfo rawInfo) {
        // A way is to check `idbrokernode` type role in `computerProfile`
        Optional<List<Role>> rolesOption = Optional.ofNullable(rawInfo.getProperties())
                .map(ClusterProperties::getComputeProfile)
                .map(ComputeProfile::getRoles);

        if (rolesOption.isPresent()) {
            return rolesOption.get().stream().anyMatch(role -> role.getName().equalsIgnoreCase("idbrokernode"));
        }

        // Fallback way is to challenge the authentication type
        try {
            return SparkBatchSubmission.getInstance().probeAuthType(
                    ClusterManagerEx.getInstance().getClusterConnectionString(rawInfo.getName())) == AuthType.AADAuth;
        } catch (IOException ex) {
            log().warn("Can't probe HDInsight cluster authentication type: " + rawInfo.getId(), ex);

            return false;
        }
    }
}