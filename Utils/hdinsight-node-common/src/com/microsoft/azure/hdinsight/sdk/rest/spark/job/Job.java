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

package com.microsoft.azure.hdinsight.sdk.rest.spark.job;

import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

import java.util.ArrayList;
import java.util.List;

/**
 * An spark job resource contains information about a particular application that was submitted to a cluster.
 *
 * Based on Spark 2.1.0, refer to http://spark.apache.org/docs/latest/monitoring.html
 *
 *   http://<spark http address:port>/applications/[app-id]/jobs
 *
 * HTTP Operations Supported
 *   GET
 *
 * Query Parameters Supported
 *   None
 */
public class Job implements IConvertible {
    private int jobId;
    private String name;
    private String submissionTime;
    private String completionTime;
    private int[] stageIds;
    private String status;

    private int numTasks;
    private int numActiveTasks;
    private int numCompletedTasks;
    private int numSkippedTasks;
    private int numFailedTasks;

    private int numActiveStages;
    private int numCompletedStages;
    private int numSkippedStages;
    private int numFailedStages;

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(String submissionTime) {
        this.submissionTime = submissionTime;
    }

    public String getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(String completionTime) {
        this.completionTime = completionTime;
    }

    public int[] getStageIds() {
        return stageIds;
    }

    public void setStageIds(int[] stageIds) {
        this.stageIds = stageIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getNumTasks() {
        return numTasks;
    }

    public void setNumTasks(int numTasks) {
        this.numTasks = numTasks;
    }

    public int getNumActiveTasks() {
        return numActiveTasks;
    }

    public void setNumActiveTasks(int numActiveTasks) {
        this.numActiveTasks = numActiveTasks;
    }

    public int getNumCompletedTasks() {
        return numCompletedTasks;
    }

    public void setNumCompletedTasks(int numCompletedTasks) {
        this.numCompletedTasks = numCompletedTasks;
    }

    public int getNumSkippedTasks() {
        return numSkippedTasks;
    }

    public void setNumSkippedTasks(int numSkippedTasks) {
        this.numSkippedTasks = numSkippedTasks;
    }

    public int getNumFailedTasks() {
        return numFailedTasks;
    }

    public void setNumFailedTasks(int numFailedTasks) {
        this.numFailedTasks = numFailedTasks;
    }

    public int getNumActiveStages() {
        return numActiveStages;
    }

    public void setNumActiveStages(int numActiveStages) {
        this.numActiveStages = numActiveStages;
    }

    public int getNumCompletedStages() {
        return numCompletedStages;
    }

    public void setNumCompletedStages(int numCompletedStages) {
        this.numCompletedStages = numCompletedStages;
    }

    public int getNumSkippedStages() {
        return numSkippedStages;
    }

    public void setNumSkippedStages(int numSkippedStages) {
        this.numSkippedStages = numSkippedStages;
    }

    public int getNumFailedStages() {
        return numFailedStages;
    }

    public void setNumFailedStages(int numFailedStages) {
        this.numFailedStages = numFailedStages;
    }

    public static final List<Job> EMPTY_LIST = new ArrayList<>(0);
    public static final Job[] EMPTY_ARRAY = new Job[0];
}
