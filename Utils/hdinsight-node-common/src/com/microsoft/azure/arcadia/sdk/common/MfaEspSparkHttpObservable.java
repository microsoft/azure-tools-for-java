/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.arcadia.sdk.common;

import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchEspMfaSubmission;

public class MfaEspSparkHttpObservable extends AzureHttpObservable {
    public MfaEspSparkHttpObservable(String tenantId) {
        this(tenantId, "");
    }

    public MfaEspSparkHttpObservable(String tenantId, String apiVersion) {
        super(tenantId, apiVersion);
    }

    @Override
    public String getResourceEndpoint() {
        return SparkBatchEspMfaSubmission.resource;
    }
}
