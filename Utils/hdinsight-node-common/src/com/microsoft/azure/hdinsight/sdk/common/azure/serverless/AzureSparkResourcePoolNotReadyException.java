package com.microsoft.azure.hdinsight.sdk.common.azure.serverless;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class AzureSparkResourcePoolNotReadyException extends RuntimeException {
    public AzureSparkResourcePoolNotReadyException(@NotNull String msg) {
        super(msg);
    }
}
