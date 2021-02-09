/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.serverexplore;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public class CosmosSparkClusterDestoryModel implements Cloneable {
    @NotNull
    private String clusterName;

    @Nullable
    private String errorMessage;

    @Nullable
    private String requestId;

    @NotNull
    public String getClusterName() {
        return clusterName;
    }

    public CosmosSparkClusterDestoryModel setClusterName(@NotNull String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    public CosmosSparkClusterDestoryModel setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    @Nullable
    public String getRequestId() {
        return requestId;
    }

    public CosmosSparkClusterDestoryModel setRequestId(@Nullable String requestId) {
        this.requestId = requestId;
        return this;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        // Here is a shadow clone, not deep clone
        return super.clone();
    }
}
