/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Lake Analytics Spark Resource Pool URI Collection.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SparkResourcePoolUriItemProperties {
    /**
     * Livy API Url.
     */
    @JsonProperty(value = "livyAPI", access = JsonProperty.Access.WRITE_ONLY)
    private String livyAPI;

    /**
     * Livy UI Url.
     */
    @JsonProperty(value = "livyUI", access = JsonProperty.Access.WRITE_ONLY)
    private String livyUI;

    /**
     * Spark Master API Url.
     */
    @JsonProperty(value = "sparkMasterAPI", access = JsonProperty.Access.WRITE_ONLY)
    private String sparkMasterAPI;

    /**
     * Spark Master UI Url.
     */
    @JsonProperty(value = "sparkMasterUI", access = JsonProperty.Access.WRITE_ONLY)
    private String sparkMasterUI;

    /**
     * Spark History API Url.
     */
    @JsonProperty(value = "sparkHistoryAPI", access = JsonProperty.Access.WRITE_ONLY)
    private String sparkHistoryAPI;

    /**
     * Spark History UI Url.
     */
    @JsonProperty(value = "sparkHistoryUI", access = JsonProperty.Access.WRITE_ONLY)
    private String sparkHistoryUI;

    /**
     * Get livy API Url.
     *
     * @return the livyAPI value
     */
    public String livyAPI() {
        return this.livyAPI;
    }

    /**
     * Get livy UI Url.
     *
     * @return the livyUI value
     */
    public String livyUI() {
        return this.livyUI;
    }

    /**
     * Get spark Master API Url.
     *
     * @return the sparkMasterAPI value
     */
    public String sparkMasterAPI() {
        return this.sparkMasterAPI;
    }

    /**
     * Get spark Master UI Url.
     *
     * @return the sparkMasterUI value
     */
    public String sparkMasterUI() {
        return this.sparkMasterUI;
    }

    /**
     * Get spark History API Url.
     *
     * @return the sparkHistoryAPI value
     */
    public String sparkHistoryAPI() {
        return this.sparkHistoryAPI;
    }

    /**
     * Get spark History UI Url.
     *
     * @return the sparkHistoryUI value
     */
    public String sparkHistoryUI() {
        return this.sparkHistoryUI;
    }

}
