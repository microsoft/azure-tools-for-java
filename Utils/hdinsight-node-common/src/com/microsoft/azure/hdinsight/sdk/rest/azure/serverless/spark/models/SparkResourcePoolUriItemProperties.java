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
