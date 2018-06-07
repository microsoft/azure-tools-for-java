package com.microsoft.azure.sparkserverless.serverexplore.sparkserverlessnode;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

public abstract class SparkServerlessClusterRootModule extends AzureRefreshableNode {
    public SparkServerlessClusterRootModule(
            @NotNull String id,
            @NotNull String name,
            @NotNull Node parent,
            @NotNull String iconPath) {
        super(id, name, parent, iconPath);
    }

    public SparkServerlessClusterRootModule(
            @NotNull String id,
            @NotNull String name,
            @NotNull Node parent,
            @NotNull String iconPath,
            @NotNull boolean delayActionLoading) {
        super(id, name, parent, iconPath, delayActionLoading);
    }

}
