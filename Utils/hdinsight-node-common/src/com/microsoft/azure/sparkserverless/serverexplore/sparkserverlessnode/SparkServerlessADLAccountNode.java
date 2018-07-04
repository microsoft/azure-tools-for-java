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

public class SparkServerlessADLAccountNode extends AzureRefreshableNode {
    // TODO: Update icon path
    private static final String ICON_PATH = CommonConst.AZURE_SERVERLESS_SPARK_ACCOUNT_ICON_PATH;
    @NotNull
    private final AzureSparkServerlessAccount adlAccount;

    public SparkServerlessADLAccountNode(@NotNull Node parent, @NotNull AzureSparkServerlessAccount adlAccount) {
        super(adlAccount.getName(), adlAccount.getName(), parent, ICON_PATH, true);
        this.adlAccount = adlAccount;
        this.loadActions();
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        /**
         * FIXME: If we get clusters from cache first, we have to maintain the state of cache, which means:
         *  a) We have to remove the cluster from adlAccount when we destroy a cluster
         *  b) We have to add the cluster to adlAccount when we provision a cluster
         * But It seems that class AzureSparkServerlessAccount does not support these operations.
         */
        adlAccount.get().subscribe(account -> {
            account.getClusters().forEach(cluster -> {
                try {
                    AzureSparkServerlessCluster serverlessCluster = (AzureSparkServerlessCluster) cluster;
                    // refresh the cluster
                    serverlessCluster.getConfigurationInfo();
                    addChildNode(new SparkServerlessClusterNode(this, serverlessCluster, adlAccount));
                } catch (Exception ignore) {
                    // FIXME: Do we need to log this exception?
                }
            });
        });
    }

    @Override
    protected void loadActions() {
        super.loadActions();

        addAction("Provision Spark Cluster", new SparkServerlessProvisionAction(
                this, adlAccount, SparkServerlessClusterOps.getInstance().getProvisionAction()));
    }

    @NotNull
    public AzureSparkServerlessAccount getAdlAccount() {
        return adlAccount;
    }

    // TODO: implement refreshWithoutAsync()
}
