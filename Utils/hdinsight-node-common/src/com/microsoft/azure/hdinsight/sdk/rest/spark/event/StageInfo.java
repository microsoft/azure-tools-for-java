/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.spark.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StageInfo {

    @JsonProperty("Stage ID")
    private int stageId;

    @JsonProperty("Stage Attempt ID")
    private int stageAttemptId;

    @JsonProperty("Stage Name")
    private String stageName;

    @JsonProperty("Number of Tasks")
    private int numberOfTasks;

    @JsonProperty("Parent IDs")
    private int[] parentIds;

    @JsonProperty("Details")
    private String details;

    @JsonProperty("Accumulables")
    private String[] accumulables;

    @JsonProperty("RDD Info")
    private RDDInfo[] rddInfos;

    public int getStageId() {
        return stageId;
    }

    public void setStageId(int stageId) {
        this.stageId = stageId;
    }

    public int getStageAttemptId() {
        return stageAttemptId;
    }

    public void setStageAttemptId(int stageAttemptId) {
        this.stageAttemptId = stageAttemptId;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public int getNumberOfTasks() {
        return numberOfTasks;
    }

    public void setNumberOfTasks(int numberOfTasks) {
        this.numberOfTasks = numberOfTasks;
    }

    public int[] getParentIds() {
        return parentIds;
    }

    public void setParentIds(int[] parentIds) {
        this.parentIds = parentIds;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String[] getAccumulables() {
        return accumulables;
    }

    public void setAccumulables(String[] accumulables) {
        this.accumulables = accumulables;
    }

    public RDDInfo[] getRddInfos() {
        return rddInfos;
    }

    public void setRddInfos(RDDInfo[] rddInfos) {
        this.rddInfos = rddInfos;
    }
}
