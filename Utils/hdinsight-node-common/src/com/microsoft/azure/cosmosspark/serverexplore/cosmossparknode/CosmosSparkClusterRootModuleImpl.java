/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosClusterManager;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class CosmosSparkClusterRootModuleImpl extends HDInsightRootModule {
    private static final String SERVICE_MODULE_ID = CosmosSparkClusterRootModuleImpl.class.getName();
    private static final String ICON_PATH = CommonConst.AZURE_SERVERLESS_SPARK_ROOT_ICON_PATH;
    private static final String BASE_MODULE_NAME = "Apache Spark on Cosmos";

    private static final String SPARK_NOTEBOOK_LINK = "https://aka.ms/spkadlnb";

    public CosmosSparkClusterRootModuleImpl(@NotNull Node parent) {
        super(SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH, true);
        this.loadActions();
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.ApacheSparkOnCosmos.MODULE;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        if (!isFeatureEnabled()) {
            return;
        }

        AzureSparkCosmosClusterManager.getInstance().refresh();
        AzureSparkCosmosClusterManager.getInstance().getAccounts().forEach(account -> {
            addChildNode(new CosmosSparkADLAccountNode(this, account));
        });
    }

    @Override
    public boolean isFeatureEnabled() {
        return true;
    }

    @Override
    public HDInsightRootModule getNewNode(Node parent) {
        return new CosmosSparkClusterRootModuleImpl(parent);
    }

    @Override
    protected void loadActions() {
        super.loadActions();

        addAction("Open Notebook", new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                try {
                    Desktop.getDesktop().browse(URI.create(SPARK_NOTEBOOK_LINK));
                } catch (IOException ignore) {
                }
            }
        });
    }

    @Override
    public String getServiceName() {
        return TelemetryConstants.SPARK_ON_COSMOS;
    }
}
