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
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class SqlBigDataClusterNode extends RefreshableNode {
    private static final String SQL_BIG_DATA_CLUSTER_ID = SqlBigDataClusterNode.class.getName();
    private static final String ICON_PATH = CommonConst.ClusterIConPath;

    @NotNull
    private SqlBigDataLivyLinkClusterDetail cluster;

    public SqlBigDataClusterNode(Node parent, @NotNull SqlBigDataLivyLinkClusterDetail clusterDetail) {
        super(SQL_BIG_DATA_CLUSTER_ID, clusterDetail.getTitle(), parent, ICON_PATH, true);
        this.cluster = clusterDetail;
        this.loadActions();
    }

    @Override
    protected void loadActions() {
        super.loadActions();

        addAction("Open Spark History UI", new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                try {
                    Desktop.getDesktop().browse(URI.create(cluster.getSparkHistoryUrl()));
                } catch (IOException ignore) {
                }
            }
        });

        addAction("Open Yarn UI", new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                try {
                    Desktop.getDesktop().browse(URI.create(cluster.getYarnNMConnectionUrl()));
                } catch (IOException ignore) {
                }
            }
        });

        addAction("Unlink", new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                boolean choice = DefaultLoader.getUIHelper().showConfirmation("Do you really want to unlink the SQL Server big data cluster?",
                        "Unlink SQL Server Big Data Cluster", new String[]{"Yes", "No"}, null);
                if (choice) {
                    ClusterManagerEx.getInstance().removeAdditionalCluster(cluster);
                    ((RefreshableNode) getParent()).load(false);
                }
            }
        });
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
    }
}
