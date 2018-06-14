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

package com.microsoft.azure.sparkserverless.serverexplore.sparkserverlessnode;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class SparkServerlessClusterNode extends AzureRefreshableNode {
    @NotNull
    private static Logger LOG = Logger.getLogger(SparkServerlessClusterNode.class);
    @NotNull
    private final String CLUSTER_MODULE_ID;
    // TODO: Update icon path
    private static final String ICON_PATH = CommonConst.ClusterIConPath;
    @NotNull
    private final AzureSparkServerlessCluster cluster;
    @NotNull
    private final AzureSparkServerlessAccount adlAccount;

    public SparkServerlessClusterNode(@NotNull Node parent,
                                      @NotNull AzureSparkServerlessCluster cluster,
                                      @NotNull AzureSparkServerlessAccount adlAccount) {
        super(String.format("%s_%s", adlAccount.getName(), cluster.getName()),
                cluster.getTitle(),
                parent,
                ICON_PATH,
                true);
        this.cluster = cluster;
        this.adlAccount = adlAccount;
        this.CLUSTER_MODULE_ID = String.format("%s_%s", cluster.getName(), adlAccount.getName());
        this.loadActions();
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        cluster.get().toBlocking().singleOrDefault(cluster);
    }

    @Override
    protected void updateNodeNameAfterLoading() {
        setName(cluster.getTitle());
    }

    @Override
    protected void loadActions() {
        super.loadActions();
        String suffix = "/?adlaAccountName=" + adlAccount.getName();
        addAction("Delete", new SparkServerlessDestroyAction(
                this, cluster, adlAccount, SparkServerlessClusterOps.getInstance().getDestroyAction()));
        addAction("View Cluster Status", new SparkServerlessMonitorAction(
                this, cluster, SparkServerlessClusterOps.getInstance().getMonitorAction()));
        addAction("Open Spark Master UI", new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                try {
                    Desktop.getDesktop().browse(URI.create(String.valueOf(cluster.getSparkMasterUiUri() + suffix)));
                } catch (IOException ignore) {
                }
            }
        });
        addAction("Open Spark History UI", new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                try {
                    Desktop.getDesktop().browse(URI.create(String.valueOf(cluster.getSparkHistoryUiUri() + suffix)));
                } catch (IOException ignore) {
                }
            }
        });
    }

    @NotNull
    public String getClusterName() {
        return cluster.getName();
    }
}
