/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Full definition of the spark batch job entity.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SparkBatchJob extends AnalyticsJob {
    /**
     * Properties specific to a serverless spark job.
     */
    @JsonProperty(value = "properties")
    private SparkBatchJobProperties properties;

    /**
     * Get properties specific to a serverless spark job.
     *
     * @return the properties value
     */
    public SparkBatchJobProperties properties() {
        return this.properties;
    }

    /**
     * Set properties specific to a serverless spark job.
     *
     * @param properties the properties value to set
     * @return the SparkBatchJob object itself.
     */
    public SparkBatchJob withProperties(SparkBatchJobProperties properties) {
        this.properties = properties;
        return this;
    }

}
