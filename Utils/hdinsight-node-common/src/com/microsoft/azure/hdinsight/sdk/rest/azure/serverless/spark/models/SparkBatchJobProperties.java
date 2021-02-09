/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Spark Batch job information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SparkBatchJobProperties {
    /**
     * Livy like response payload for the spark serverless job.
     */
    @JsonProperty(value = "responsePayload")
    private SparkBatchJobResponsePayload responsePayload;

    /**
     * Spark Master UI Url. Only available when the job is running.
     */
    @JsonProperty(value = "sparkMasterUI", access = JsonProperty.Access.WRITE_ONLY)
    private String sparkMasterUI;

    /**
     * Livy api endpoint. Only available when the job is running.
     */
    @JsonProperty(value = "livyServerAPI", access = JsonProperty.Access.WRITE_ONLY)
    private String livyServerAPI;

    /**
     * Get livy like response payload for the spark serverless job.
     *
     * @return the responsePayload value
     */
    public SparkBatchJobResponsePayload responsePayload() {
        return this.responsePayload;
    }

    /**
     * Set livy like response payload for the spark serverless job.
     *
     * @param responsePayload the responsePayload value to set
     * @return the SparkBatchJobProperties object itself.
     */
    public SparkBatchJobProperties withResponsePayload(SparkBatchJobResponsePayload responsePayload) {
        this.responsePayload = responsePayload;
        return this;
    }

    /**
     * Get spark Master UI Url. Only available when the job is running.
     *
     * @return the sparkMasterUI value
     */
    public String sparkMasterUI() {
        return this.sparkMasterUI;
    }

    /**
     * Get livy api endpoint. Only available when the job is running.
     *
     * @return the livyServerAPI value
     */
    public String livyServerAPI() {
        return this.livyServerAPI;
    }

}
