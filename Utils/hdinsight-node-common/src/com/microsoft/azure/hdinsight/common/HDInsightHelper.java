/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;

import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

public interface HDInsightHelper {
    void openJobViewEditor(@NotNull Object projectObject, @NotNull String uuid);
    void closeJobViewEditor(@NotNull Object projectObject, @NotNull String uuid);
    String getPluginRootPath();
    String getInstallationId();
    boolean isIntelliJPlugin();
    boolean isOptIn();

    @Nullable
    default NodeActionListener createAddNewHDInsightReaderClusterAction(@NotNull HDInsightRootModule module, @NotNull ClusterDetail clusterDetail) {
        return null;
    }

    default void createRefreshHdiReaderJobsWarningForm(@NotNull HDInsightRootModule module, @NotNull ClusterDetail clusterDetail) {
    }

    default void createRefreshHdiReaderStorageAccountsWarningForm(@NotNull RefreshableNode node, @NotNull String aseDeepLink) {
    }

    default void createRefreshHdiLinkedClusterStorageAccountsWarningForm(@NotNull RefreshableNode node, @NotNull String aseDeepLink) {
    }
}
