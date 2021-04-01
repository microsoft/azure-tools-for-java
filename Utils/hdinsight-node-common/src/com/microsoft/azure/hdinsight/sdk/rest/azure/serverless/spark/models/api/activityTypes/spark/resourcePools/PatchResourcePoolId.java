/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.api.activityTypes.spark.resourcePools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.UpdateSparkResourcePool;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

/**
 * Updates the resource pool for the specified resource pool ID
 */
public class PatchResourcePoolId {

    /**
     * The parameters to update a spark resource pool
     */
    @NotNull
    @JsonProperty(value = "updateSparkResourcePool")
    private UpdateSparkResourcePool parameters;

    /**
     * set the parameters value
     * @param parameters the parameters value to set
     */
    public PatchResourcePoolId withParameters(@NotNull UpdateSparkResourcePool parameters) {
        this.parameters = parameters;
        return this;
    }
}
