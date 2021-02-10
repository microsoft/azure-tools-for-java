/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Base resource pool.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourcePool extends AnalyticsActivity {
}
