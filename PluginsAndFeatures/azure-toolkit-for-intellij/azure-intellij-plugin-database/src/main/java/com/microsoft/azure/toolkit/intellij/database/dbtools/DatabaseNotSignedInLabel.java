package com.microsoft.azure.toolkit.intellij.database.dbtools;


import com.microsoft.azure.toolkit.intellij.dbtools.NotSignedInTipLabel;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import javax.annotation.Nonnull;

public class DatabaseNotSignedInLabel extends NotSignedInTipLabel {
    private static final String NOT_SIGNIN_TIPS = "<html><a href=\"\">Sign in</a> to select an existing %s server in Azure.</html>";
    @Nonnull
    private final DatabaseServerClass databaseServerClass;

    public DatabaseNotSignedInLabel(@Nonnull DatabaseServerClass databaseServerClass) {
        super(String.format(NOT_SIGNIN_TIPS, databaseServerClass.getServerName()));
        this.databaseServerClass = databaseServerClass;
    }

    @Override
    @AzureOperation(name = "user/$database.signin_from_dbtools")
    protected void signIn() {
        OperationContext.current().setTelemetryProperty("serviceName", databaseServerClass.getServiceName());
        super.signIn();
    }
}