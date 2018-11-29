package com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode;

import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import org.apache.commons.lang3.tuple.Pair;
import rx.subjects.PublishSubject;

public class CosmosSparkServerlessSubmitAction extends NodeActionListener {
    // TODO: Update adlAccount type
    @NotNull
    private final AzureSparkServerlessAccount adlAccount;
    @NotNull
    private final PublishSubject<Pair<AzureSparkServerlessAccount, CosmosSparkADLAccountNode>> provisionAction;
    @NotNull
    private final CosmosSparkADLAccountNode adlAccountNode;

    public CosmosSparkServerlessSubmitAction(@NotNull CosmosSparkADLAccountNode adlAccountNode,
                                          @NotNull AzureSparkServerlessAccount adlAccount,
                                          @NotNull PublishSubject<Pair<
                                                  AzureSparkServerlessAccount,
                                                  CosmosSparkADLAccountNode>> provisionAction) {
        super(adlAccountNode);
        this.adlAccount = adlAccount;
        this.provisionAction = provisionAction;
        this.adlAccountNode = adlAccountNode;
    }

    @Override
    @NotNull
    protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
        provisionAction.onNext(Pair.of(adlAccount, adlAccountNode));
    }
}
