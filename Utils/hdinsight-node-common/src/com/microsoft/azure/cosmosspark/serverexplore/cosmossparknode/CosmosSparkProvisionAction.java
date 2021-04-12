/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode;

import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import org.apache.commons.lang3.tuple.Pair;
import rx.subjects.PublishSubject;

public class CosmosSparkProvisionAction extends NodeActionListener {
    // TODO: Update adlAccount type
    @NotNull
    private final AzureSparkServerlessAccount adlAccount;
    @NotNull
    private final PublishSubject<Pair<AzureSparkServerlessAccount, CosmosSparkADLAccountNode>> provisionAction;
    @NotNull
    private final CosmosSparkADLAccountNode adlAccountNode;

    public CosmosSparkProvisionAction(@NotNull CosmosSparkADLAccountNode adlAccountNode,
                                      @NotNull AzureSparkServerlessAccount adlAccount,
                                      @NotNull PublishSubject<Pair<
                                                  AzureSparkServerlessAccount,
                                                  CosmosSparkADLAccountNode>> provisionAction) {
        super();
        this.adlAccount = adlAccount;
        this.provisionAction = provisionAction;
        this.adlAccountNode = adlAccountNode;
    }

    @Override
    protected void actionPerformed(NodeActionEvent e) {
        provisionAction.onNext(Pair.of(adlAccount, adlAccountNode));
    }
}
