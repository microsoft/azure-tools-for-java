/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.azure.hdinsight.common.*;
import com.microsoft.azure.hdinsight.sdk.cluster.*;
import com.microsoft.azure.hdinsight.sdk.common.CommonConstant;
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azure.sqlbigdata.serverexplore.SqlBigDataClusterModule;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

import java.awt.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClusterNode extends RefreshableNode implements TelemetryProperties {
    private static final String CLUSTER_MODULE_ID = ClusterNode.class.getName();
    private static final String ICON_PATH = CommonConst.ClusterIConPath;

    @NotNull
    private IClusterDetail clusterDetail;

    public ClusterNode(Node parent, @NotNull IClusterDetail clusterDetail) {
        super(CLUSTER_MODULE_ID, getClusterNameWitStatus(clusterDetail), parent, ICON_PATH, true);
        this.clusterDetail = clusterDetail;
        this.loadActions();
        this.load(false);
    }

    @Override
    protected void loadActions() {
        super.loadActions();

        if (clusterDetail instanceof ClusterDetail || clusterDetail instanceof HDInsightAdditionalClusterDetail ||
                clusterDetail instanceof EmulatorClusterDetail) {
            addAction("Open Spark History UI", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    String sparkHistoryUrl = clusterDetail.isEmulator() ?
                            ((EmulatorClusterDetail)clusterDetail).getSparkHistoryEndpoint() :
                            ClusterManagerEx.getInstance().getClusterConnectionString(clusterDetail.getName()) + "/sparkhistory";
                    openUrlLink(sparkHistoryUrl);
                }
            });

            addAction("Open Azure Storage Explorer for storage", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    String aseDeepLink = "storageexplorer:///";
                    openUrlLink(aseDeepLink);
                }
            });

            addAction("Open Cluster Management Portal(Ambari)", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    String ambariUrl = clusterDetail.isEmulator() ?
                            ((EmulatorClusterDetail)clusterDetail).getAmbariEndpoint() :
                            ClusterManagerEx.getInstance().getClusterConnectionString(clusterDetail.getName());
                    openUrlLink(ambariUrl);
                }
            });
        }

        if (clusterDetail instanceof ClusterDetail) {
            addAction("Open Jupyter Notebook", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    final String jupyterUrl = ClusterManagerEx.getInstance().getClusterConnectionString(clusterDetail.getName()) + "/jupyter/tree";
                    openUrlLink(jupyterUrl);
                }
            });

            addAction("Open Azure Management Portal", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    String resourceGroupName = clusterDetail.getResourceGroup();
                    if (resourceGroupName != null) {

                        String webPortHttpLink = String.format(HDIEnvironment.getHDIEnvironment().getPortal() + "#resource/subscriptions/%s/resourcegroups/%s/providers/Microsoft.HDInsight/clusters/%s",
                                clusterDetail.getSubscription().getSubscriptionId(),
                                resourceGroupName,
                                clusterDetail.getName());
                        openUrlLink(webPortHttpLink);
                    } else {
                        DefaultLoader.getUIHelper().showError("Failed to get resource group name.", "HDInsight Explorer");
                    }
                }
            });
        }

        if (clusterDetail instanceof HDInsightAdditionalClusterDetail || clusterDetail instanceof HDInsightLivyLinkClusterDetail) {
            addAction("Unlink", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    boolean choice = DefaultLoader.getUIHelper().showConfirmation("Do you really want to unlink the HDInsight cluster?",
                            "Unlink HDInsight Cluster", new String[]{"Yes", "No"}, null);
                    if (choice) {
                        ClusterManagerEx.getInstance().removeAdditionalCluster(clusterDetail);
                        ((HDInsightRootModule) getParent()).refreshWithoutAsync();
                    }
                }
            });
        } else if (clusterDetail instanceof SqlBigDataLivyLinkClusterDetail) {
            addAction("Unlink", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    boolean choice = DefaultLoader.getUIHelper().showConfirmation("Do you really want to unlink the SQL big data cluster?",
                            "Unlink SQL Big Data Cluster", new String[]{"Yes", "No"}, null);
                    if (choice) {
                        ClusterManagerEx.getInstance().removeAdditionalCluster(clusterDetail);
                        ((SqlBigDataClusterModule) getParent()).refreshWithoutAsync();
                    }
                }
            });
        } else if (clusterDetail instanceof EmulatorClusterDetail) {
            addAction("Unlink", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    boolean choice = DefaultLoader.getUIHelper().showConfirmation("Do you really want to unlink the Emulator cluster?",
                            "Unlink Emulator Cluster", new String[]{"Yes", "No"}, null);
                    if (choice) {
                        ClusterManagerEx.getInstance().removeEmulatorCluster((EmulatorClusterDetail) clusterDetail);
                        ((HDInsightRootModule) getParent()).refreshWithoutAsync();
                    }
                }
            });
        }
    }

    @Override
    protected void refreshItems() {
        this.load(false);

        if(!clusterDetail.isEmulator()) {
            JobViewManager.registerJovViewNode(clusterDetail.getName(), clusterDetail);
            JobViewNode jobViewNode = new JobViewNode(this, clusterDetail.getName());
            boolean isIntelliJ = HDInsightLoader.getHDInsightHelper().isIntelliJPlugin();
            boolean isLinux = System.getProperty("os.name").toLowerCase().contains("linux");
            if(isIntelliJ || !isLinux) {
                addChildNode(jobViewNode);
            }

            RefreshableNode storageAccountNode = new StorageAccountFolderNode(this, clusterDetail);
            addChildNode(storageAccountNode);
        }
    }

    private static String getClusterNameWitStatus(IClusterDetail clusterDetail) {
        String state = clusterDetail.getState();
        if (!StringHelper.isNullOrWhiteSpace(state) && !state.equalsIgnoreCase("Running")) {
            return String.format("%s (State:%s)", clusterDetail.getTitle(), state);
        }

        return clusterDetail.getTitle();
    }

    private void openUrlLink(@NotNull String linkUrl) {
        if (!StringHelper.isNullOrWhiteSpace(clusterDetail.getName())) {
            try {
                DefaultLoader.getIdeHelper().openLinkInBrowser(linkUrl);
            } catch (Exception exception) {
                DefaultLoader.getUIHelper().showError(exception.getMessage(), "HDInsight Explorer");
            }
        }
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.clusterDetail.getSubscription().getSubscriptionId());
        properties.put(AppInsightsConstants.Region, this.clusterDetail.getLocation());
        return properties;
    }
}
