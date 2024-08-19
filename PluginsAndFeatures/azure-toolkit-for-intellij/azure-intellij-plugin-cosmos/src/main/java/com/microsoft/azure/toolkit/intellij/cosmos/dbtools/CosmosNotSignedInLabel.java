package com.microsoft.azure.toolkit.intellij.cosmos.dbtools;

import com.microsoft.azure.toolkit.intellij.dbtools.NotSignedInTipLabel;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;

import javax.annotation.Nonnull;

public class CosmosNotSignedInLabel extends NotSignedInTipLabel {
    private static final String NOT_SIGNIN_TIPS = "<html><a href=\"\">Sign in</a> to select an existing Azure Cosmos DB account.</html>";
    @Nonnull
    private final DatabaseAccountKind kind;

    public CosmosNotSignedInLabel(@Nonnull DatabaseAccountKind kind) {
        super(NOT_SIGNIN_TIPS);
        this.kind = kind;
    }

    @Override
    @AzureOperation(name = "user/cosmos.signin_from_dbtools")
    protected void signIn() {
        OperationContext.action().setTelemetryProperty("kind", this.kind.getValue());
        super.signIn();
    }
}
