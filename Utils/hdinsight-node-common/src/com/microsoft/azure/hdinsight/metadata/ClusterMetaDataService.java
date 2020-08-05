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
