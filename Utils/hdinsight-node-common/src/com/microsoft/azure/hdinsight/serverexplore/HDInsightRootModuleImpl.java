/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.serverexplore;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.IconPathBuilder;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.ClusterNode;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;

public class HDInsightRootModuleImpl extends HDInsightRootModule {
    private static final String HDINSIGHT_NODE_EXPAND = "HDInsightExplorer.HDInsightNodeExpand";

    private static final String HDInsight_SERVICE_MODULE_ID = HDInsightRootModuleImpl.class.getName();
    private static final String ICON_PATH = IconPathBuilder
            .custom(CommonConst.HDExplorerIconName)
            .setBigSize()
            .setPathPrefix("")
            .build();
    private static final String BASE_MODULE_NAME = "HDInsight";

    public HDInsightRootModuleImpl(@NotNull Node parent) {
        super(HDInsight_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.HDInsight.MODULE;
    }

    @Override
    public HDInsightRootModule getNewNode(@NotNull Node node) {
        return new HDInsightRootModuleImpl(node);
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        synchronized (this) {
            ClusterManagerEx.getInstance().getCachedClusters().stream()
                    .filter(ClusterManagerEx.getInstance().getHDInsightClusterFilterPredicate())
                    .forEach(cluster -> addChildNode(new ClusterNode(this, cluster)));
        }
    }

    @Override
    protected void refreshFromAzure() throws Exception {
        synchronized (this) {
            ClusterManagerEx.getInstance().getClusterDetails();
        }
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        // Send telemetry for expanding node action
        AppInsightsClient.create(HDINSIGHT_NODE_EXPAND, null);
        EventUtil.logEvent(EventType.info, TelemetryConstants.HDINSIGHT, HDINSIGHT_NODE_EXPAND, null);

        super.onNodeClick(e);
    }

    @Override
    @NotNull
    public String getServiceName() {
        return TelemetryConstants.HDINSIGHT;
    }
}
