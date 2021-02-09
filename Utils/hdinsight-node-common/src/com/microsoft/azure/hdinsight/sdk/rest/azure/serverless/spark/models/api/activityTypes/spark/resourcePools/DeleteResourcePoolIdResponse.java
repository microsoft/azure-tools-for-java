/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.api.activityTypes.spark.resourcePools;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * response of 'delete /activityTypes/spark/resourcePools/{resourcePoolId}'
 */
public class DeleteResourcePoolIdResponse {
    public static final Map<Integer, String> successfulResponses = ImmutableMap.of(
            200, "Successfully stopped the resource pool",
            202, "Successfully initiated the stoppage of the specified resource pool",
            204, "The specified resource pool was not found");
}
