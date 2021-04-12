/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

public abstract class HDInsightRootModule extends AzureRefreshableNode {

    public HDInsightRootModule(String id, String name, Node parent, String iconPath) {
        super(id, name, parent, iconPath);
    }

    public HDInsightRootModule(String id, String name, Node parent, String iconPath, boolean delayActionLoading) {
        super(id, name, parent, iconPath, delayActionLoading);
    }

    public abstract HDInsightRootModule getNewNode(Node parent);

    public boolean isFeatureEnabled() {
        return false;
    }
}
