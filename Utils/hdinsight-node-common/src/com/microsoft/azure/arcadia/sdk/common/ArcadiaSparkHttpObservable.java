/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.arcadia.sdk.common;

import com.microsoft.azure.hdinsight.sdk.common.ApiVersionParam;
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchArcadiaSubmission;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.NameValuePair;

import java.util.List;
import java.util.stream.Collectors;

public class ArcadiaSparkHttpObservable extends AzureHttpObservable {
    public ArcadiaSparkHttpObservable(@NotNull String tenantId) {
        super(tenantId, "");
    }

    @Override
    public List<NameValuePair> getDefaultParameters() {
        return super.getDefaultParameters()
                .stream()
                // parameter apiVersion is not needed for arcadia since it's already specified in the path of query url
                .filter(kvPair -> !kvPair.getName().equals(ApiVersionParam.NAME))
                .collect(Collectors.toList());
    }

    @Override
    public String getResourceEndpoint() {
        return SparkBatchArcadiaSubmission.ARCADIA_RESOURCE_ID;
    }
}
