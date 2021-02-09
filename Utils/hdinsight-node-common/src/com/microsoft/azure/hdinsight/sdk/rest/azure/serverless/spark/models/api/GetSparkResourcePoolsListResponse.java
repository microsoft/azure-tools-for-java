/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkResourcePoolList;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.Map;

/**
 * response of 'get /activityTypes/spark/resourcePools'
 */
public class GetSparkResourcePoolsListResponse {
    public static final Map<Integer, String> successfulResponses = ImmutableMap.of(
            200, "Successfully retrieved list of resource pools");

    @NotNull
    @JsonProperty(value = "sparkResourcePoolList")
    private SparkResourcePoolList sparkResourcePoolList;

    /**
     * Get the sparkResourcePoolList value.
     *
     * @return the sparkResourcePoolList value
     */
    @NotNull
    public SparkResourcePoolList sparkResourcePoolList() {
        return sparkResourcePoolList;
    }
}
