/**
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018 JetBrains s.r.o.
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
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.metadata.ClusterMetaDataService;
import com.microsoft.azure.hdinsight.sdk.cluster.*;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClusterManagerEx implements ILogger {

    private static final String OSTYPE = "linux";

    private static ClusterManagerEx instance = null;

    /**
     * additionalClusterDetails contains all kinds of linked clusters, which consists of:
     * 1. HDInsightAdditionalClusterDetail
     * 2. HDInsightLivyLinkClusterDetail
     * 3. SqlBigDataLivyLinkClusterDetail
     */
    private List<IClusterDetail> additionalClusterDetails = new ArrayList<>();
    private List<IClusterDetail> emulatorClusterDetails = new ArrayList<>();

    private boolean isListClusterSuccess = false;
    private boolean isListAdditionalClusterSuccess = false;
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

    public boolean isListAdditionalClusterSuccess() {
        return isListAdditionalClusterSuccess;
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

    @Nullable
    public IClusterDetail findClusterDetail(Predicate<IClusterDetail> predicate, boolean isLinkedCluster) {
        Stream<IClusterDetail> clusterDetailStream =
                isLinkedCluster
                        ? getAdditionalClusterDetails().stream()
                        : ClusterMetaDataService.getInstance().getCachedClusterDetails().stream();
        return clusterDetailStream.filter(predicate).findFirst().orElse(null);
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

    public synchronized ImmutableList<IClusterDetail> getCachedClusters() {
        return ClusterMetaDataService.getInstance().getCachedClusterDetails();
    }

    public Predicate<IClusterDetail> getHDInsightClusterFilterPredicate() {
        return clusterDetail -> clusterDetail instanceof ClusterDetail ||
                clusterDetail instanceof HDInsightAdditionalClusterDetail ||
                clusterDetail instanceof HDInsightLivyLinkClusterDetail ||
                clusterDetail instanceof EmulatorClusterDetail;
    }

    synchronized Observable<List<ClusterDetail>> getSubscriptionHDInsightClustersOfType(List<SubscriptionDetail> list) {
        setSelectedSubscriptionExist(list.stream().anyMatch(SubscriptionDetail::isSelected));
        return ClusterManager.getInstance().getHDInsightClustersWithSpecificType(list, OSTYPE)
                .doOnNext(clusters -> isListClusterSuccess = true)
                .doOnError(err -> {
                    log().warn("Error Refreshing HDInsight clusters. " + ExceptionUtils.getStackTrace(err));
                    isListClusterSuccess = false;
                });
    }

    public List<IClusterDetail> getAdditionalClusterDetails() {
        return additionalClusterDetails;
    }

    void setAdditionalClusterDetails(List<IClusterDetail> additionalClusterDetails) {
        this.additionalClusterDetails = additionalClusterDetails;
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
    List<ClusterDetail> getSubscriptionHDInsightClusters(@Nullable AzureManager manager) {
        if (manager == null) {
            return new ArrayList<>();
        }

        return Observable.fromCallable(() -> manager.getSubscriptionManager().getSelectedSubscriptionDetails())
                .doOnError(err ->
                        DefaultLoader.getUIHelper().showError("Failed to get HDInsight Clusters. " +
                                        "Please check your subscription and login at Azure Explorer (View -> Tool Windows -> Azure Explorer).",
                                "List HDInsight Cluster Error"))
                .flatMap(this::getSubscriptionHDInsightClustersOfType)
                .onErrorResumeNext(Observable.just(new ArrayList<>()))
                .toBlocking()
                .singleOrDefault(new ArrayList<>());
    }

    /**
     * Load all kinds of cluster details and set these clusters to cache. These clusters includes:
     * 1. HDInsight clusters under user's azure subscription
     * 2. HDInsight linked clusters
     * 3. Emulator clusters
     * 4. SQL Big Data clusters
     * @return all kinds of cluster details
     */
    public synchronized ImmutableList<IClusterDetail> getClusterDetails() {
        if (!isListAdditionalClusterSuccess()) {
            setAdditionalClusterDetails(loadAdditionalClusters());
        }

        if (!isListEmulatorClusterSuccess()) {
            setEmulatorClusterDetails(getEmulatorClusters());
        }

        isListClusterSuccess = false;

        // Prepare a cloned additional clusters to handle the intersect clusters in both subscription and linked
        List<IClusterDetail> allAdditionalClusters = new ArrayList<>(getAdditionalClusterDetails());

        // Get clusters from Subscription, an empty list for non-logged in user.
        Stream<ClusterDetail> clusterDetailsFromSubscription = getSubscriptionHDInsightClusters(getAzureManager())
                .stream();

        // Merge the clusters from subscription with linked one if the name is same
        List<IClusterDetail> mergedClusters = clusterDetailsFromSubscription
                .map(cluster -> { // replace the duplicated cluster with the linked one
                    Optional<IClusterDetail> inLinkedAndSubscriptionCluster = allAdditionalClusters.stream()
                            .filter(linkedCluster -> linkedCluster instanceof HDInsightAdditionalClusterDetail ||
                                    linkedCluster instanceof HDInsightLivyLinkClusterDetail)
                            .filter(linkedCluster -> linkedCluster.getName().equals(cluster.getName()))
                            .findFirst();

                    // remove the duplicated cluster from additional clusters
                    inLinkedAndSubscriptionCluster.ifPresent(allAdditionalClusters::remove);

                    return  inLinkedAndSubscriptionCluster.orElse(cluster);
                })
                .collect(Collectors.toList());

        mergedClusters.addAll(allAdditionalClusters);
        mergedClusters.addAll(getEmulatorClusterDetails());

        setCachedClusters(mergedClusters);
        return getCachedClusters();
    }

    public synchronized  void addEmulatorCluster(EmulatorClusterDetail emulatorClusterDetail) {
        emulatorClusterDetails.add(emulatorClusterDetail);
        ClusterMetaDataService.getInstance().addClusterToCache(emulatorClusterDetail);

        saveEmulatorClusters();
    }

    public synchronized void addAdditionalCluster(@NotNull IClusterDetail hdInsightClusterDetail) {
        additionalClusterDetails.add(hdInsightClusterDetail);
        ClusterMetaDataService.getInstance().addClusterToCache(hdInsightClusterDetail);
        saveAdditionalClusters();
    }

    public synchronized void updateHdiAdditionalClusterDetail(@NotNull HDInsightAdditionalClusterDetail clusterDetailToUpdate) {
        // Remove the cluster which is a linked HDI cluster and share the same cluster name with clusterDetailToUpdate
        this.additionalClusterDetails = additionalClusterDetails.stream()
                .filter(clusterDetail1 ->
                        !(clusterDetail1 instanceof HDInsightAdditionalClusterDetail
                                && clusterDetail1.getName().equals(clusterDetailToUpdate.getName())))
                .collect(Collectors.toList());
        ClusterMetaDataService.getInstance().removeClusterFromCache(clusterDetailToUpdate);
        addAdditionalCluster(clusterDetailToUpdate);
    }

    public synchronized void removeEmulatorCluster(EmulatorClusterDetail emulatorClusterDetail) {
        emulatorClusterDetails.remove(emulatorClusterDetail);
        ClusterMetaDataService.getInstance().removeClusterFromCache(emulatorClusterDetail);

        saveEmulatorClusters();
    }

    public synchronized void removeAdditionalCluster(@NotNull IClusterDetail hdInsightClusterDetail) {
        additionalClusterDetails.remove(hdInsightClusterDetail);
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
                IHDIStorageAccount storageAccount = clusterDetail.getStorageAccount();
                if (storageAccount == null) {
                    return 0;
                } else if (storageAccount.getName().equals(storageName)) {
                    return 1;
                }

                List<HDStorageAccount> additionalStorageAccount = clusterDetail.getAdditionalStorageAccounts();
                if (additionalStorageAccount != null) {
                    for (HDStorageAccount account : additionalStorageAccount) {
                        if (account.getName().equals(storageName)) {
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
        List<IClusterDetail> hdiAdditionalClusters = new ArrayList<>();
        List<IClusterDetail> hdiLivyLinkClusters = new ArrayList<>();
        List<IClusterDetail> sqlBigDatalivyLinkClusters = new ArrayList<>();

        additionalClusterDetails.forEach(clusterDetail -> {
            if (clusterDetail instanceof HDInsightLivyLinkClusterDetail) {
                hdiLivyLinkClusters.add(clusterDetail);
            } else if (clusterDetail instanceof HDInsightAdditionalClusterDetail) {
                hdiAdditionalClusters.add(clusterDetail);
            } else if (clusterDetail instanceof SqlBigDataLivyLinkClusterDetail) {
                sqlBigDatalivyLinkClusters.add(clusterDetail);
            }
        });
        Gson gson = new Gson();
        String additionalClustersJson = gson.toJson(hdiAdditionalClusters);
        DefaultLoader.getIdeHelper().setApplicationProperty(CommonConst.HDINSIGHT_ADDITIONAL_CLUSTERS, additionalClustersJson);

        String livyLinkClustersJson = gson.toJson(hdiLivyLinkClusters);
        DefaultLoader.getIdeHelper().setApplicationProperty(CommonConst.HDINSIGHT_LIVY_LINK_CLUSTERS, livyLinkClustersJson);

        String sqlBigDatalivyLinkClustersJson = gson.toJson(sqlBigDatalivyLinkClusters);
        DefaultLoader.getIdeHelper().setApplicationProperty(CommonConst.SQL_BIG_DATA_LIVY_LINK_CLUSTERS, sqlBigDatalivyLinkClustersJson);
    }

    List<IClusterDetail> loadAdditionalClusters() {
        List<IClusterDetail> hdiAdditionalClusters = new ArrayList<>();
        List<IClusterDetail> hdiLivyLinkClusters = new ArrayList<>();
        List<IClusterDetail> sqlBigDataClusters = new ArrayList<>();

        isListAdditionalClusterSuccess = false;
        Gson gson = new Gson();
        String additionalClustersJson = DefaultLoader.getIdeHelper().getApplicationProperty(CommonConst.HDINSIGHT_ADDITIONAL_CLUSTERS);
        String livyLinkClustersJson = DefaultLoader.getIdeHelper().getApplicationProperty(CommonConst.HDINSIGHT_LIVY_LINK_CLUSTERS);
        String sqlBigDataClustersJson = DefaultLoader.getIdeHelper().getApplicationProperty(CommonConst.SQL_BIG_DATA_LIVY_LINK_CLUSTERS);
        if (!StringHelper.isNullOrWhiteSpace(additionalClustersJson) && !StringHelper.isNullOrWhiteSpace(livyLinkClustersJson)) {
            try {
                hdiAdditionalClusters = gson.fromJson(additionalClustersJson, new TypeToken<ArrayList<HDInsightAdditionalClusterDetail>>() {
                }.getType());
                hdiLivyLinkClusters = gson.fromJson(livyLinkClustersJson, new TypeToken<ArrayList<HDInsightLivyLinkClusterDetail>>() {
                }.getType());
                sqlBigDataClusters = gson.fromJson(sqlBigDataClustersJson, new TypeToken<ArrayList<SqlBigDataLivyLinkClusterDetail>>() {
                }.getType());
            } catch (JsonSyntaxException e) {
                isListAdditionalClusterSuccess = false;
                // clear local cache if we cannot get information from local json
                DefaultLoader.getIdeHelper().unsetApplicationProperty(CommonConst.HDINSIGHT_ADDITIONAL_CLUSTERS);
                DefaultLoader.getIdeHelper().unsetApplicationProperty(CommonConst.HDINSIGHT_LIVY_LINK_CLUSTERS);
                DefaultLoader.getIdeHelper().unsetApplicationProperty(CommonConst.SQL_BIG_DATA_LIVY_LINK_CLUSTERS);
                DefaultLoader.getUIHelper().showException("Failed to list linked clusters", e, "List Linked Clusters", false, true);
                return new ArrayList<>();
            }
        }

        isListAdditionalClusterSuccess = true;
        Stream<IClusterDetail> hdiLinkedClusters = Stream.concat(hdiAdditionalClusters.stream(), hdiLivyLinkClusters.stream());
        if (sqlBigDataClusters == null) {
            return hdiLinkedClusters.collect(Collectors.toList());
        } else {
            return Stream.concat(hdiLinkedClusters, sqlBigDataClusters.stream()).collect(Collectors.toList());
        }
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

    public boolean isHdiReaderCluster(@NotNull IClusterDetail clusterDetail) {
        return clusterDetail instanceof ClusterDetail && ((ClusterDetail) clusterDetail).isRoleTypeReader();
    }
}
