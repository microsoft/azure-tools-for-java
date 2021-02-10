/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.sqlbigdata.serverexplore;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.*;

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
            protected void actionPerformed(NodeActionEvent e) {
                try {
                    DefaultLoader.getIdeHelper().openLinkInBrowser(cluster.getSparkHistoryUrl());
                } catch (Exception ignore) {
                }
            }
        });

        addAction("Open Yarn UI", new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                try {
                    DefaultLoader.getIdeHelper().openLinkInBrowser(cluster.getYarnUIUrl());
                } catch (Exception ignore) {
                }
            }
        });

        NodeActionListener listener = new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                boolean choice = DefaultLoader.getUIHelper().showConfirmation("Do you really want to unlink the SQL Server big data cluster?",
                        "Unlink SQL Server Big Data Cluster", new String[]{"Yes", "No"}, null);
                if (choice) {
                    ClusterManagerEx.getInstance().removeAdditionalCluster(cluster);
                    ((RefreshableNode) getParent()).load(false);
                }
            }
        };
        addAction("Unlink", new WrappedTelemetryNodeActionListener(
                getServiceName(), TelemetryConstants.UNLINK_SPARK_CLUSTER, listener));
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
    }

    @Override
    public String getServiceName() {
        return TelemetryConstants.SPARK_ON_SQL_SERVER;
    }
}
