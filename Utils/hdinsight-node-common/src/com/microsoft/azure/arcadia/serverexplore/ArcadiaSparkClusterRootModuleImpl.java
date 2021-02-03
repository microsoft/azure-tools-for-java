/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.arcadia.serverexplore;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkComputeManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

public class ArcadiaSparkClusterRootModuleImpl extends HDInsightRootModule {
    private static final String SERVICE_MODULE_ID = ArcadiaSparkClusterRootModuleImpl.class.getName();
    private static final String ICON_PATH = CommonConst.ARCADIA_WORKSPACE_MODULE_ICON_PATH;
    private static final String BASE_MODULE_NAME = "Apache Spark on Azure Synapse";

    public ArcadiaSparkClusterRootModuleImpl(@NotNull Node parent) {
        super(SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH, true);
        this.loadActions();
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.ApacheSparkOnAzureSynapse.MODULE;
    }

    @Override
    protected synchronized void refreshItems() throws AzureCmdException {
        if (!isFeatureEnabled()) {
            return;
        }

        ArcadiaSparkComputeManager.getInstance().refresh();
        ArcadiaSparkComputeManager.getInstance().getWorkspaces().forEach(workSpace -> {
            addChildNode(new ArcadiaSparkWorkspaceNode(this, workSpace));
        });
    }

    @Override
    public boolean isFeatureEnabled() {
        return true;
    }

    @Override
    public HDInsightRootModule getNewNode(Node parent) {
        return new ArcadiaSparkClusterRootModuleImpl(parent);
    }

    @Override
    public String getServiceName() {
        return TelemetryConstants.SPARK_ON_ARCADIA;
    }
}
