/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.jobs.framework;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

/**
 * Rest API request object
 *
 */
public interface IRequest {

    @NotNull
    String getRequestUrl();

    @NotNull
    IClusterDetail getCluster();

    @NotNull
    HttpRequestType getRestType();
}
