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

package com.microsoft.azure.sqlbigdata.serverexplore;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

public class SqlBigDataClusterNode extends RefreshableNode {
    private static final String SQL_BIG_DATA_CLUSTER_ID = SqlBigDataClusterNode.class.getName();
    private static final String ICON_PATH = CommonConst.ClusterIConPath;

    @NotNull
    private IClusterDetail clusterDetail;

    public SqlBigDataClusterNode(Node parent, @NotNull IClusterDetail clusterDetail) {
        super(SQL_BIG_DATA_CLUSTER_ID, clusterDetail.getTitle(), parent, ICON_PATH, true);
        this.clusterDetail = clusterDetail;
        this.loadActions();
        this.load(false);
    }

    @Override
    protected void loadActions() {
        super.loadActions();

        // TODO: Add Master UI link and Yarn UI link

        addAction("Unlink", new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                boolean choice = DefaultLoader.getUIHelper().showConfirmation("Do you really want to unlink the SQL big data cluster?",
                        "Unlink SQL Big Data Cluster", new String[]{"Yes", "No"}, null);
                if (choice) {
                    ClusterManagerEx.getInstance().removeAdditionalCluster(clusterDetail);
                    ((RefreshableNode) getParent()).load(false);
                }
            }
        });
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        this.load(false);
    }
}
