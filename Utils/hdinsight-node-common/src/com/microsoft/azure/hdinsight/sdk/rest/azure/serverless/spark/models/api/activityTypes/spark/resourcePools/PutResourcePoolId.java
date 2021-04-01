/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.api.activityTypes.spark.resourcePools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.CreateSparkResourcePool;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

/**
 * Submits a resource pool creation request to the specified Data Lake Analytics account
 */
public class PutResourcePoolId implements IConvertible {
    /**
     * The parameters to submit a spark resource pool creation request
     */
    @NotNull
    @JsonProperty(value = "parameters", required = true)
    private CreateSparkResourcePool parameters;

    /**
     * get the parameters value
     * @return the parameters value
     */
    @NotNull
    public CreateSparkResourcePool getParameters() {
        return parameters;
    }

    /**
     * set the parameters value
     * @param parameters the parameters value to set
     */
    public PutResourcePoolId withParameters(@NotNull CreateSparkResourcePool parameters) {
        this.parameters = parameters;
        return this;
    }
}
