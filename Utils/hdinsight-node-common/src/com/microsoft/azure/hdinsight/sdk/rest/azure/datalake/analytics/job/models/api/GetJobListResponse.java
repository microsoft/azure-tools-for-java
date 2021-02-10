/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models.api;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Lists the jobs, if any, associated with the specified Data Lake Analytics account.
 * The response includes a link to the next page of results, if any.
 */
public class GetJobListResponse {
    public static final Map<Integer, String> successfulResponses = ImmutableMap.of(
            200, "Successfully retrieved the list of jobs.");
}
