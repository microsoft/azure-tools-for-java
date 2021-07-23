/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.spark.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobStartEventLog {
    @JsonProperty("Event")
    private String event;

    @JsonProperty("Job ID")
    private int jobId;

    @JsonProperty("Submission Time")
    private String submissionTime;

    @JsonProperty("Stage Infos")
    private StageInfo[] stageInfos;

    @JsonProperty("Stage IDs")
    private int[] stageIds;

    @JsonProperty("Properties")
    private Map<String, String> properties;

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(String submissionTime) {
        this.submissionTime = submissionTime;
    }

    public StageInfo[] getStageInfos() {
        return stageInfos;
    }

    public void setStageInfos(StageInfo[] stageInfos) {
        this.stageInfos = stageInfos;
    }

    public int[] getStageIds() {
        return stageIds;
    }

    public void setStageIds(int[] stageIds) {
        this.stageIds = stageIds;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
