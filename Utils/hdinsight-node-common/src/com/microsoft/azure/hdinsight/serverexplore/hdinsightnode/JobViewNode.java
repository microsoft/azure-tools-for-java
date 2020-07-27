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
