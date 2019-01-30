/**
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
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
