package com.microsoft.azure.toolkit.intellij.cosmos.dbtools;

import com.microsoft.azure.toolkit.intellij.dbtools.TelemetryConnectionInterceptor;

import static com.microsoft.azure.toolkit.intellij.cosmos.dbtools.AzureCosmosDbAccountParamEditor.KEY_COSMOS_ACCOUNT_ID;

public class AzureCosmosDbAccountConnectionInterceptor extends TelemetryConnectionInterceptor {
    protected AzureCosmosDbAccountConnectionInterceptor() {
        super(KEY_COSMOS_ACCOUNT_ID, "cosmos", "cosmos.connect_jdbc_from_dbtools");
    }
}
