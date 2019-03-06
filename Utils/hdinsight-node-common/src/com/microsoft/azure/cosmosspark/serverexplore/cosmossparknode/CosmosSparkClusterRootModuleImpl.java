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

package com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosClusterManager;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;

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
    protected void refreshItems() throws AzureCmdException{
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
        return AzureSparkCosmosClusterManager.getInstance().isFeatureEnabled().toBlocking().singleOrDefault(false);
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
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                try {
                    Desktop.getDesktop().browse(URI.create(SPARK_NOTEBOOK_LINK));
                } catch (IOException ignore) {
                }
            }
        });
    }
}
