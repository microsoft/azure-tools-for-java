/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

public class JobViewNode extends RefreshableNode implements ILogger {
    private static String NODE_ID = JobViewNode.class.getName();
    private static String NODE_NAME = "Jobs";
    private static String NODE_ICON_PATH = CommonConst.StorageAccountFoldIConPath;

    @NotNull
    private IClusterDetail clusterDetail;

    public JobViewNode( Node parent, @NotNull IClusterDetail clusterDetail) {
        super(NODE_ID, NODE_NAME, parent, NODE_ICON_PATH, true);
        this.clusterDetail = clusterDetail;

        this.loadActions();
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        if (ClusterManagerEx.getInstance().isHdiReaderCluster(clusterDetail)) {
            HDInsightLoader.getHDInsightHelper().createRefreshHdiReaderJobsWarningForm(
                    getHDInsightRootModule(), (ClusterDetail) clusterDetail);
        } else {
            HDInsightLoader.getHDInsightHelper().openJobViewEditor(getProject(), clusterDetail.getName());
        }

        super.onNodeClick(e);
    }

    @NotNull
    private HDInsightRootModule getHDInsightRootModule() {
        ClusterNode clusterNode = (ClusterNode) this.getParent();
        HDInsightRootModule hdInsightRootModule = (HDInsightRootModule) clusterNode.getParent();
        return hdInsightRootModule;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
    }

    @Override
    public String getServiceName() {
        return TelemetryConstants.HDINSIGHT;
    }
}
