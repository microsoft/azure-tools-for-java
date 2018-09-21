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

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.hdinsight.metadata.ClusterMetaDataService;
import com.microsoft.azure.hdinsight.sdk.cluster.*;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azure.hdinsight.sdk.common.AggregatedException;
import com.microsoft.azure.hdinsight.sdk.common.AuthenticationErrorHandler;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClusterManagerEx {

    private static final String OSTYPE = "linux";

    private static ClusterManagerEx instance = null;

    private List<IClusterDetail> hdinsightAdditionalClusterDetails = new ArrayList<>();
    private List<IClusterDetail> emulatorClusterDetails = new ArrayList<>();

    private boolean isListClusterSuccess = false;
    private boolean isLIstAdditionalClusterSuccess = false;
    private boolean isListEmulatorClusterSuccess = false;
    private boolean isSelectedSubscriptionExist = false;

    private ClusterManagerEx() {
    }

    public static ClusterManagerEx getInstance() {
        if (instance == null) {
            synchronized (ClusterManagerEx.class) {
                if (instance == null) {
                    instance = new ClusterManagerEx();
                }
            }
        }

        return instance;
    }

    public String getClusterConnectionString(@NotNull final String clusterName) {
        String formatString = HDIEnvironment.getHDIEnvironment().getClusterConnectionFormat();
        return String.format(formatString, clusterName);
    }

    public String getBlobFullName(@NotNull final String storageName) {
        return String.format(HDIEnvironment.getHDIEnvironment().getBlobFullNameFormat(), storageName);
    }

    void setSelectedSubscriptionExist(boolean selectedSubscriptionExist) {
        isSelectedSubscriptionExist = selectedSubscriptionExist;
    }

    public boolean isSelectedSubscriptionExist() {
        return isSelectedSubscriptionExist;
    }

    public boolean isListClusterSuccess() {
        return isListClusterSuccess;
    }

    public boolean isLIstAdditionalClusterSuccess() {
        return isLIstAdditionalClusterSuccess;
    }

    public boolean isListEmulatorClusterSuccess() { return isListEmulatorClusterSuccess; }

    public ImmutableList<IClusterDetail> getClusterDetailsWithoutAsync() {
        return getClusterDetailsWithoutAsync(false);
    }

    public ImmutableList<IClusterDetail> getClusterDetailsWithoutAsync(boolean isIgnoreErrorCluster) {
        final ImmutableList<IClusterDetail> cachedClusterDetails =
                Optional.of(ClusterMetaDataService.getInstance().getCachedClusterDetails())
                        .filter(clusters -> !clusters.isEmpty())
                        .orElseGet(this::getClusterDetails);

        if (isIgnoreErrorCluster) {
            List<IClusterDetail> result = new ArrayList<>();
            for (IClusterDetail clusterDetail : cachedClusterDetails) {
                if (clusterDetail instanceof ClusterDetail && !clusterDetail.getState().equalsIgnoreCase("Running")) {
                    continue;
                }
                result.add(clusterDetail);
            }
            return ImmutableList.copyOf(result);
        } else {
            return cachedClusterDetails;
        }
    }

    public Optional<IClusterDetail> getClusterDetailByName(String clusterName) {
        return getClusterDetailsWithoutAsync(true)
                .stream()
                .filter(cluster -> cluster.getName().equals(clusterName))
                .findFirst()
                .flatMap(cluster -> {
                    try {
                        cluster.getConfigurationInfo();

                        return Optional.of(cluster);
                    } catch (Exception ignore) {
                        return Optional.empty();
                    }
                });
    }

    synchronized void setCachedClusters(@NotNull List<IClusterDetail> clusterDetails) {
        ClusterMetaDataService.getInstance().addCachedClusters(clusterDetails);
    }

    synchronized ImmutableList<IClusterDetail> getCachedClusters() {
        return ClusterMetaDataService.getInstance().getCachedClusterDetails();
    }

    synchronized Optional<List<IClusterDetail>> getSubscriptionHDInsightClustersOfType(
                                                            List<SubscriptionDetail> list) {
        setSelectedSubscriptionExist(list.stream().anyMatch(SubscriptionDetail::isSelected));

        try {
            Optional<List<IClusterDetail>> clusters = Optional.of(
                    ClusterManager.getInstance().getHDInsightClustersWithSpecificType(list, OSTYPE));

            // TODO: so far we have not a good way to judge whether it is token expired as
            //       we have changed the way to list HDInsight clusters
            isListClusterSuccess = clusters.isPresent();

            return clusters;
        } catch (AggregatedException aggregateException) {
            if (dealWithAggregatedException(aggregateException)) {
                DefaultLoader.getUIHelper().showError(
                        "Failed to get HDInsight Cluster, Please make sure there's no login problem first",
                        "List HDInsight Cluster Error");
            }

            return Optional.empty();
        }
    }

    public List<IClusterDetail> getHdinsightAdditionalClusterDetails() {
        return hdinsightAdditionalClusterDetails;
    }

    void setHdinsightAdditionalClusterDetails(List<IClusterDetail> hdinsightAdditionalClusterDetails) {
        this.hdinsightAdditionalClusterDetails = hdinsightAdditionalClusterDetails;
    }

    List<IClusterDetail> getEmulatorClusterDetails() {
        return emulatorClusterDetails;
    }

    void setEmulatorClusterDetails(List<IClusterDetail> emulatorClusterDetails) {
        this.emulatorClusterDetails = emulatorClusterDetails;
    }

    AzureManager getAzureManager() {
        try {
            return AuthMethodManager.getInstance().getAzureManager();
        } catch (Exception ex) {
            return null;
        }
    }

    @NotNull
    List<IClusterDetail> getSubscriptionHDInsightClusters(@Nullable AzureManager manager) {
        return Optional.ofNullable(manager)
                .map(AzureManager::getSubscriptionManager)
                .flatMap(subscriptionManager -> {
                    try {
                        return Optional.ofNullable(subscriptionManager.getSubscriptionDetails());
                    } catch (IOException e) {
                        DefaultLoader.getUIHelper().showError("Failed to get HDInsight Clusters. " +
                                        "Please check your subscription and login at Azure Explorer (View -> Tool Windows -> Azure Explorer).",
                                "List HDInsight Cluster Error");

                        return Optional.empty();
                    }
                })
                .flatMap(this::getSubscriptionHDInsightClustersOfType)
                .orElse(new ArrayList<>());
    }

    public synchronized ImmutableList<IClusterDetail> getClusterDetails() {
        if (!isLIstAdditionalClusterSuccess) {
            setHdinsightAdditionalClusterDetails(getAdditionalClusters());
        }

        if (!isListEmulatorClusterSuccess) {
            setEmulatorClusterDetails(getEmulatorClusters());
        }

        isListClusterSuccess = false;

        // Prepare a cloned additional clusters to handle the intersect clusters in both subscription and linked
        List<IClusterDetail> additionalClusters = new ArrayList<>(getHdinsightAdditionalClusterDetails());

        // Get clusters from Subscription, an empty list for non-logged in user.
        Stream<IClusterDetail> clusterDetailsFromSubscription = getSubscriptionHDInsightClusters(getAzureManager())
                                                                    .stream();

        // Merge the clusters from subscription with linked one if the name is same
        List<IClusterDetail> mergedClusters = clusterDetailsFromSubscription
                .filter(clusterDetail -> clusterDetail.getSubscription().isSelected())
                .map(cluster -> { // replace the duplicated cluster with the linked one
                    Optional<IClusterDetail> inLinkedAndSubscriptionCluster = additionalClusters.stream()
                            .filter(linkedCluster -> linkedCluster.getName().equals(cluster.getName()))
                            .findFirst();

                    // remove the duplicated cluster from additional clusters
                     inLinkedAndSubscriptionCluster.ifPresent(additionalClusters::remove);

                    return  inLinkedAndSubscriptionCluster.orElse(cluster);
                })
                .collect(Collectors.toList());

        mergedClusters.addAll(additionalClusters);
        mergedClusters.addAll(getEmulatorClusterDetails());

        setCachedClusters(mergedClusters);
        return getCachedClusters();
    }

    public synchronized  void addEmulatorCluster(EmulatorClusterDetail emulatorClusterDetail) {
        emulatorClusterDetails.add(emulatorClusterDetail);
        ClusterMetaDataService.getInstance().addClusterToCache(emulatorClusterDetail);

        saveEmulatorClusters();
    }

    public synchronized void addHDInsightAdditionalCluster(HDInsightAdditionalClusterDetail hdInsightClusterDetail) {

        hdinsightAdditionalClusterDetails.add(hdInsightClusterDetail);
        ClusterMetaDataService.getInstance().addClusterToCache(hdInsightClusterDetail);

        saveAdditionalClusters();
    }

    public synchronized  void removeEmulatorCluster(EmulatorClusterDetail emulatorClusterDetail) {
        emulatorClusterDetails.remove(emulatorClusterDetail);
        ClusterMetaDataService.getInstance().removeClusterFromCache(emulatorClusterDetail);

        saveEmulatorClusters();
    }

    public synchronized void removeHDInsightAdditionalCluster(HDInsightAdditionalClusterDetail hdInsightClusterDetail) {

        hdinsightAdditionalClusterDetails.remove(hdInsightClusterDetail);
        ClusterMetaDataService.getInstance().removeClusterFromCache(hdInsightClusterDetail);

        saveAdditionalClusters();
    }

    /*
        return 0: cluster can be added to additional cluster list
        return 1: cluster already exist in current cluster list
        return 2: cluster is valid to add to cluster list but storage account is not default
     */
    @Deprecated
    public int isHDInsightAdditionalStorageExist(String clusterName, String storageName) {
        final ImmutableList<IClusterDetail> cachedClusterDetails =
                Optional.of(ClusterMetaDataService.getInstance().getCachedClusterDetails())
                        .filter(clusters -> !clusters.isEmpty())
                        .orElseGet(this::getClusterDetails);

        for (IClusterDetail clusterDetail : cachedClusterDetails) {
            if (clusterDetail.getName().equals(clusterName) && clusterDetail instanceof HDInsightAdditionalClusterDetail) {
                try {
                    if (clusterDetail.getStorageAccount().getName().equals(storageName)) {
                        return 1;
                    }
                } catch (HDIException e) {
                    return 0;
                }

                List<HDStorageAccount> additionalStorageAccount = clusterDetail.getAdditionalStorageAccounts();
                if (additionalStorageAccount != null) {
                    for (HDStorageAccount storageAccount : additionalStorageAccount) {
                        if (storageAccount.getName().equals(storageName)) {
                            return 2;
                        }
                    }
                }
            }
        }

        return 0;
    }

    public boolean isEmulatorClusterExist(String clusterName) {
        final ImmutableList<IClusterDetail> cachedClusterDetails =
                Optional.of(ClusterMetaDataService.getInstance().getCachedClusterDetails())
                        .filter(clusters -> !clusters.isEmpty())
                        .orElseGet(this::getClusterDetails);

        for( IClusterDetail clusterDetail : cachedClusterDetails) {
            if( clusterDetail.getName().equals(clusterName)) {
                return true;
            }
        }

        return false;
    }

    private void saveEmulatorClusters() {
        Gson gson = new Gson();
        String json = gson.toJson(emulatorClusterDetails);
        DefaultLoader.getIdeHelper().setApplicationProperty(CommonConst.EMULATOR_CLUSTERS, json);
    }

    private void saveAdditionalClusters() {
        Gson gson = new Gson();
        String json = gson.toJson(hdinsightAdditionalClusterDetails);
        DefaultLoader.getIdeHelper().setApplicationProperty(CommonConst.HDINSIGHT_ADDITIONAL_CLUSTERS, json);
    }

    List<IClusterDetail> getAdditionalClusters() {
        Gson gson = new Gson();
        String json = DefaultLoader.getIdeHelper().getApplicationProperty(CommonConst.HDINSIGHT_ADDITIONAL_CLUSTERS);
        List<IClusterDetail> hdiLocalClusters = new ArrayList<>();

        isLIstAdditionalClusterSuccess = false;
        if (!StringHelper.isNullOrWhiteSpace(json)) {
            try {
                hdiLocalClusters = gson.fromJson(json, new TypeToken<ArrayList<HDInsightAdditionalClusterDetail>>() {
                }.getType());
            } catch (JsonSyntaxException e) {

                isLIstAdditionalClusterSuccess = false;
                // clear local cache if we cannot get information from local json
                DefaultLoader.getIdeHelper().unsetApplicationProperty(CommonConst.HDINSIGHT_ADDITIONAL_CLUSTERS);
                DefaultLoader.getUIHelper().logError("Failed to list additional HDInsight cluster", e);
                return new ArrayList<>();
            }
        }

        isLIstAdditionalClusterSuccess = true;
        return hdiLocalClusters;
    }

    List<IClusterDetail> getEmulatorClusters() {
        Gson gson = new Gson();
        String json = DefaultLoader.getIdeHelper().getApplicationProperty(CommonConst.EMULATOR_CLUSTERS);
        List<IClusterDetail> emulatorClusters = new ArrayList<>();

        isListEmulatorClusterSuccess = false;
        if(!StringHelper.isNullOrWhiteSpace(json)){
            try {
                emulatorClusters = gson.fromJson(json, new TypeToken<ArrayList<EmulatorClusterDetail>>(){
                }.getType());
            } catch (JsonSyntaxException e){
                isListEmulatorClusterSuccess = false;
                DefaultLoader.getIdeHelper().unsetApplicationProperty(CommonConst.EMULATOR_CLUSTERS);
                DefaultLoader.getUIHelper().logError("Failed to list emulator cluster", e);
                return new ArrayList<>();
            }
        }

        isListEmulatorClusterSuccess = true;
        return emulatorClusters;
    }

    private boolean dealWithAggregatedException(AggregatedException aggregateException) {
        boolean isReAuth = false;
        for (Exception exception : aggregateException.getExceptionList()) {
            if (exception instanceof HDIException) {
                if (((HDIException) exception).getErrorCode() == AuthenticationErrorHandler.AUTH_ERROR_CODE) {
                    isReAuth = true;
                    break;
                }
            }
        }

        return isReAuth;
    }
}
