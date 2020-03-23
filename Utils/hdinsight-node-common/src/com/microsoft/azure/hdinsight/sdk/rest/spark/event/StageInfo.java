/*
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
 */

package com.microsoft.azure.hdinsight.sdk.rest.spark.event;

import com.fasterxml.jackson.annotation.JsonProperty;

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
