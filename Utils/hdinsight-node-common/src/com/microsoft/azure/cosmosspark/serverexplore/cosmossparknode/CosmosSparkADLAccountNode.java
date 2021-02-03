/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import rx.Observable;

public class CosmosSparkADLAccountNode extends AzureRefreshableNode implements ILogger {
    private static final String ICON_PATH = CommonConst.AZURE_SERVERLESS_SPARK_ACCOUNT_ICON_PATH;
    @NotNull
    private final AzureSparkServerlessAccount adlAccount;

    public CosmosSparkADLAccountNode(@NotNull Node parent, @NotNull AzureSparkServerlessAccount adlAccount) {
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
        adlAccount.get()
                .onErrorResumeNext(err -> {
                    log().warn(String.format(
                            "Got exceptions when listing Azure Data Lake account(%s) for listing Spark pools: %s",
                            adlAccount.getName(),
                            err));

                    return Observable.empty();
                })
                .subscribe(account -> account.getClusters().forEach(cluster -> {
                    try {
                        AzureSparkCosmosCluster serverlessCluster = (AzureSparkCosmosCluster) cluster;
                        // refresh the cluster
                        serverlessCluster.getConfigurationInfo();
                        addChildNode(new CosmosSparkClusterNode(this, serverlessCluster, adlAccount));
                    } catch (Exception ex) {
                        log().warn(String.format("Got exceptions when adding Azure Data Lake account node(%s):%s",
                                                 adlAccount.getName(), ex));
                    }
                }));
    }

    @Override
    protected void loadActions() {
        super.loadActions();

        addAction("Provision Spark Cluster", new CosmosSparkProvisionAction(
                this, adlAccount, CosmosSparkClusterOps.getInstance().getProvisionAction()));
        addAction("Submit Apache Spark on Cosmos Serverless Job", new CosmosServerlessSparkSubmitAction(
                this, adlAccount, CosmosSparkClusterOps.getInstance().getServerlessSubmitAction()));
        addAction("View Apache Spark on Cosmos Serverless Jobs", new CosmosServerlessSparkViewJobsAction(
                this, adlAccount, CosmosSparkClusterOps.getInstance().getViewServerlessJobsAction()));
    }

    @NotNull
    public AzureSparkServerlessAccount getAdlAccount() {
        return adlAccount;
    }

    @Override
    public String getServiceName() {
        return TelemetryConstants.SPARK_ON_COSMOS;
    }
}
