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

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.EmulatorClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.CommonConstant;
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

    private IClusterDetail clusterDetail;

    public ClusterNode(Node parent, IClusterDetail clusterDetail) {
        super(CLUSTER_MODULE_ID, getClusterNameWitStatus(clusterDetail), parent, ICON_PATH, true);
        this.clusterDetail = clusterDetail;
        this.loadActions();
        this.load(false);
    }

    @Override
    protected void loadActions() {
        super.loadActions();

        addAction("Open Spark History UI", new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                String sparkHistoryUrl = clusterDetail.isEmulator() ?
                        ((EmulatorClusterDetail)clusterDetail).getSparkHistoryEndpoint() :
                        String.format("https://%s.azurehdinsight.net/sparkhistory", clusterDetail.getName());
                openUrlLink(sparkHistoryUrl);
            }
        });

        addAction("Open Cluster Management Portal(Ambari)", new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                String ambariUrl = clusterDetail.isEmulator() ?
                        ((EmulatorClusterDetail)clusterDetail).getAmbariEndpoint() :
                        String.format(CommonConstant.DEFAULT_CLUSTER_ENDPOINT, clusterDetail.getName());
                openUrlLink(ambariUrl);
            }
        });

        if (clusterDetail instanceof ClusterDetail) {
            addAction("Open Jupyter Notebook", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    String jupyterUrl = String.format("https://%s.azurehdinsight.net/jupyter/tree", clusterDetail.getName());
                    openUrlLink(jupyterUrl);
                }
            });

            addAction("Open Azure Management Portal", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    String resourceGroupName = clusterDetail.getResourceGroup();
                    if (resourceGroupName != null) {
                        String webPortHttpLink = String.format("https://portal.azure.com/#resource/subscriptions/%s/resourcegroups/%s/providers/Microsoft.HDInsight/clusters/%s",
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

        if (clusterDetail instanceof HDInsightAdditionalClusterDetail) {
            addAction("Unlink", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    boolean choice = DefaultLoader.getUIHelper().showConfirmation("Do you really want to unlink the HDInsight cluster?",
                            "Unlink HDInsight Cluster", new String[]{"Yes", "No"}, null);
                    if(choice) {
                        ClusterManagerEx.getInstance().removeHDInsightAdditionalCluster((HDInsightAdditionalClusterDetail)clusterDetail);
                        ((HDInsightRootModule) getParent()).refreshWithoutAsync();
                    }
                }
            });
        }

        if(clusterDetail instanceof EmulatorClusterDetail) {
            addAction("Unlink", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    boolean choice = DefaultLoader.getUIHelper().showConfirmation("Do you really want to unlink the Emulator cluster?",
                            "Unlink HDInsight Cluster", new String[]{"Yes", "No"}, null);
                    if(choice) {
                        ClusterManagerEx.getInstance().removeEmulatorCluster((EmulatorClusterDetail) clusterDetail);
                        ((HDInsightRootModule) getParent()).refreshWithoutAsync();
                    }
                }
            });
        }
    }

    @Override
    protected void refreshItems() {
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
        String sparkVersion = clusterDetail.getSparkVersion();
        if (!StringHelper.isNullOrWhiteSpace(state) && !state.equalsIgnoreCase("Running")) {
            return String.format("%s (Spark:%s) (State:%s)", clusterDetail.getName(), sparkVersion, state);
        }

        boolean isAdditional = clusterDetail instanceof HDInsightAdditionalClusterDetail;
        boolean isEmulator = clusterDetail instanceof EmulatorClusterDetail;
        String optionalMessage = "";
        if (isAdditional) {
           optionalMessage = "Linked";
        } else if (isEmulator) {
            optionalMessage = "Emulator";
        }

        if (StringHelper.isNullOrWhiteSpace(optionalMessage)) {
            return String.format("%s (Spark: %s)", clusterDetail.getName(), sparkVersion);
        } else {
            return  String.format("%s (Spark: %s %s)", clusterDetail.getName(), sparkVersion, optionalMessage);
        }
    }

    private void openUrlLink(String linkUrl) {
        if (clusterDetail != null && !StringHelper.isNullOrWhiteSpace(clusterDetail.getName())) {
            try {
                Desktop.getDesktop().browse(new URI(linkUrl));
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
