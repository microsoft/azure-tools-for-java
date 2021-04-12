/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base analytics job.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyticsJob extends AnalyticsActivity {
    /**
     * the recurring batch job relationship information properties.
     */
    @JsonProperty(value = "related", access = JsonProperty.Access.WRITE_ONLY)
    private JobRelationshipProperties related;

    /**
     * Get the recurring batch job relationship information properties.
     *
     * @return the related value
     */
    public JobRelationshipProperties related() {
        return this.related;
    }

}
