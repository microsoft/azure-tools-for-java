/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common.azure.serverless;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class AzureSparkResourcePoolNotReadyException extends RuntimeException {
    public AzureSparkResourcePoolNotReadyException(@NotNull String msg) {
        super(msg);
    }
}
