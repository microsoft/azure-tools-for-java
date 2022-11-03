/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.azure.hdinsight.common.*;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.*;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.*;

import java.util.HashMap;
import java.util.Map;

public class ClusterNode extends RefreshableNode implements TelemetryProperties, ILogger {
    private static final String CLUSTER_MODULE_ID = ClusterNode.class.getName();
    private static final String ICON_PATH = CommonConst.ClusterIConPath;
    public static final String ASE_DEEP_LINK = "storageexplorer:///";
    @NotNull
    private IClusterDetail clusterDetail;

    public ClusterNode(Node parent, @NotNull IClusterDetail clusterDetail) {
        super(CLUSTER_MODULE_ID, clusterDetail.getTitle(), parent, ICON_PATH, true);
        this.clusterDetail = clusterDetail;
        this.loadActions();
    }

    @Override
    protected void loadActions() {
        super.loadActions();

        if (ClusterManagerEx.getInstance().isHdiReaderCluster(clusterDetail)) {
            // We need to refresh the whole HDInsight root node when we successfully linked the cluster
            // So we have to pass "hdinsightRootModule" to the link cluster action
            HDInsightRootModule hdinsightRootModule = (HDInsightRootModule) this.getParent();
            NodeActionListener linkClusterActionListener =
                    HDInsightLoader.getHDInsightHelper().createAddNewHDInsightReaderClusterAction(hdinsightRootModule,
                            (ClusterDetail) clusterDetail);
            addAction("Link This Cluster", linkClusterActionListener);
        }

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
                    final AzureString title =  OperationBundle.description("storage.open_azure_storage_explorer.account", clusterDetail.getName());
                    AzureTaskManager.getInstance().runInBackground(new AzureTask<>(title, () -> {
                        OpenHDIAzureStorageExplorerAction openHDIAzureStorageExplorerAction = new OpenHDIAzureStorageExplorerAction();
                        openHDIAzureStorageExplorerAction.openResource(clusterDetail);
                    }));
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
                                clusterDetail.getSubscription().getId(),
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
            NodeActionListener listener = new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    boolean choice = DefaultLoader.getUIHelper().showConfirmation("Do you really want to unlink the HDInsight cluster?",
                            "Unlink HDInsight Cluster", new String[]{"Yes", "No"}, null);
                    if (choice) {
                        ClusterManagerEx.getInstance().removeAdditionalCluster(clusterDetail);
                        ((RefreshableNode) getParent()).load(false);
                    }
                }
            };
            addAction("Unlink", new WrappedTelemetryNodeActionListener(
                    getServiceName(), TelemetryConstants.UNLINK_SPARK_CLUSTER, listener));
        } else if (clusterDetail instanceof EmulatorClusterDetail) {
            NodeActionListener listener = new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    boolean choice = DefaultLoader.getUIHelper().showConfirmation("Do you really want to unlink the Emulator cluster?",
                            "Unlink Emulator Cluster", new String[]{"Yes", "No"}, null);
                    if (choice) {
                        ClusterManagerEx.getInstance().removeEmulatorCluster((EmulatorClusterDetail) clusterDetail);
                        ((RefreshableNode) getParent()).load(false);
                    }
                }
            };
            addAction("Unlink", new WrappedTelemetryNodeActionListener(
                    getServiceName(), TelemetryConstants.UNLINK_SPARK_CLUSTER, listener));
        }
    }

    @Override
    protected boolean refreshEnabledWhenNotSignIn() {
        // HDInsight cluster users should be accessible to their linked clusters
        // when not sign in their Azure accounts
        return true;
    }

    @Override
    protected void refreshItems() {
        if(!clusterDetail.isEmulator()) {
            JobViewManager.registerJovViewNode(clusterDetail.getName(), clusterDetail);
            JobViewNode jobViewNode = new JobViewNode(this, clusterDetail);
            boolean isIntelliJ = HDInsightLoader.getHDInsightHelper().isIntelliJPlugin();
            boolean isLinux = System.getProperty("os.name").toLowerCase().contains("linux");
            if(isIntelliJ || !isLinux) {
                addChildNode(jobViewNode);
            }

            RefreshableNode storageAccountNode = new StorageAccountFolderNode(this, clusterDetail);
            addChildNode(storageAccountNode);
        }
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
        properties.put(AppInsightsConstants.SubscriptionId, this.clusterDetail.getSubscription().getId());
        properties.put(AppInsightsConstants.Region, this.clusterDetail.getLocation());
        return properties;
    }

    @Override
    @NotNull
    public String getServiceName() {
        return TelemetryConstants.HDINSIGHT;
    }
}
