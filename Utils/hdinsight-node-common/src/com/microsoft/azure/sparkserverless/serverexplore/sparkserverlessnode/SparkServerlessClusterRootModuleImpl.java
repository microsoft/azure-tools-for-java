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
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessClusterManager;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

public class SparkServerlessClusterRootModuleImpl extends HDInsightRootModule {
    private static final String SERVICE_MODULE_ID = SparkServerlessClusterRootModuleImpl.class.getName();
    // TODO: Update icon path
    private static final String ICON_PATH = CommonConst.AZURE_SERVERLESS_SPARK_ROOT_ICON_PATH;
    // TODO: determine root node name
    private static final String BASE_MODULE_NAME = "Azure Data Lake Spark Pool";

    public SparkServerlessClusterRootModuleImpl(@NotNull Node parent) {
        super(SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH, true);
        this.loadActions();
    }

    @Override
    protected void refreshItems() throws AzureCmdException{
        if (!isFeatureEnabled()) {
            return;
        }

        AzureSparkServerlessClusterManager.getInstance().refresh();
        AzureSparkServerlessClusterManager.getInstance().getAccounts().forEach(account -> {
            addChildNode(new SparkServerlessADLAccountNode(this, account));
        });
    }

    @Override
    public boolean isFeatureEnabled() {
        return AzureSparkServerlessClusterManager.getInstance().isFeatureEnabled().toBlocking().singleOrDefault(false);
    }

    @Override
    public HDInsightRootModule getNewNode(Node parent) {
        return new SparkServerlessClusterRootModuleImpl(parent);
    }

    @Override
    public void refreshWithoutAsync() {

    }

    // TODO: refreshWithoutAsync() is called when unlink an HDInsight cluster. Maybe we also need to implement this method here?
    // public void refreshWithoutAsync()
}
