/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitResponse;

/**
 *  * To leverage Livy(HDInsight) Spark Batch Job codes, some fields are commented
 *  * since they can be inherited from the base class SparkSubmissionParameter
 *
 * Spark Batch job information equivalent to livy response payload in OSS.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SparkBatchJobResponsePayload extends SparkSubmitResponse {
//    /**
//     * The session id.
//     */
//    @JsonProperty(value = "id", access = JsonProperty.Access.WRITE_ONLY)
//    private Integer id;
//
//    /**
//     * The application Id of the session.
//     */
//    @JsonProperty(value = "appId", access = JsonProperty.Access.WRITE_ONLY)
//    private String appId;
//
//    /**
//     * The detailed applicationInfo.
//     */
//    @JsonProperty(value = "appInfo", access = JsonProperty.Access.WRITE_ONLY)
//    private Map<String, String> appInfo;
//
//    /**
//     * The log lines.
//     */
//    @JsonProperty(value = "log", access = JsonProperty.Access.WRITE_ONLY)
//    private List<String> log;
//
//    /**
//     * The batch state.
//     */
//    @JsonProperty(value = "state", access = JsonProperty.Access.WRITE_ONLY)
//    private String state;
//
//    /**
//     * Get the session id.
//     *
//     * @return the id value
//     */
//    public Integer id() {
//        return this.id;
//    }
//
//    /**
//     * Get the application Id of the session.
//     *
//     * @return the appId value
//     */
//    public String appId() {
//        return this.appId;
//    }
//
//    /**
//     * Get the detailed applicationInfo.
//     *
//     * @return the appInfo value
//     */
//    public Map<String, String> appInfo() {
//        return this.appInfo;
//    }
//
//    /**
//     * Get the log lines.
//     *
//     * @return the log value
//     */
//    public List<String> log() {
//        return this.log;
//    }
//
//    /**
//     * Get the batch state.
//     *
//     * @return the state value
//     */
//    public String state() {
//        return this.state;
//    }

}
