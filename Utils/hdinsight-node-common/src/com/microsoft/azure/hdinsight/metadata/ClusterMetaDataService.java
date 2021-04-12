/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.metadata;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.List;
import java.util.stream.Collectors;


public class ClusterMetaDataService {
    private static ClusterMetaDataService instance = new ClusterMetaDataService();
    private ImmutableList<IClusterDetail> cachedClusters = ImmutableList.of();

    private ClusterMetaDataService() {
    }

    public static ClusterMetaDataService getInstance() {
        return instance;
    }

    public ImmutableList<IClusterDetail> getCachedClusterDetails () {
        return cachedClusters;
    }

    public void addCachedClusters(@NotNull List<IClusterDetail> clusterDetails) {
        cachedClusters = ImmutableList.copyOf(clusterDetails);
    }

    public boolean addClusterToCache(@NotNull IClusterDetail clusterDetail) {
        if (cachedClusters.stream().map(IClusterDetail::getName).anyMatch(clusterDetail.getName()::equals)) {
            return false;
        }

        cachedClusters = new ImmutableList.Builder<IClusterDetail>().addAll(cachedClusters).add(clusterDetail).build();

        return true;
    }

    public boolean isCachedClusterExist(@NotNull IClusterDetail clusterDetail) {
        for (IClusterDetail iClusterDetail : cachedClusters) {
            if (iClusterDetail.getName().equals(clusterDetail.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean removeClusterFromCache(@NotNull IClusterDetail clusterDetailToRemove) {
        if (cachedClusters.stream().map(IClusterDetail::getName).noneMatch(clusterDetailToRemove.getName()::equals)) {
            return false;
        }

        cachedClusters = new ImmutableList.Builder<IClusterDetail>()
                .addAll(cachedClusters.stream()
                                      .filter(clusterDetail -> !clusterDetail.getName().equals(clusterDetailToRemove.getName()))
                                      .collect(Collectors.toList()))
                .build();

        return true;
    }
}
