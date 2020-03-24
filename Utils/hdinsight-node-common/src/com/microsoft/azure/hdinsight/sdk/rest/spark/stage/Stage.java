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

package com.microsoft.azure.hdinsight.sdk.rest.spark.stage;

import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An spark stage resource contains information about a particular application that was submitted to a cluster.
 *
 * Based on Spark 2.1.0, refer to http://spark.apache.org/docs/latest/monitoring.html
 *
 *   http://<spark http address:port>/applications/[app-id]/stages
 *
 * HTTP Operations Supported
 *   GET
 *
 * Query Parameters Supported
 *   None
 */

@JsonIgnoreProperties(ignoreUnknown=true)
public class Stage implements IConvertible {
    private String status;
    private int stageId;
    private int attemptId;

    private int numActiveTasks;
    private int numCompleteTasks;
    private int numFailedTasks;

    private int executorRunTime;
    private String submissionTime;
    private String firstTaskLaunchedTime;
    private String completionTime;

    private long inputBytes;
    private long inputRecords;
    private long outputBytes;
    private long outputRecords;
    private long shuffleReadBytes;
    private long shuffleReadRecords;
    private long shuffleWriteBytes;
    private long shuffleWriteRecords;
    private long memoryBytesSpilled;
    private long diskBytesSpilled;

    private String name;
    private String details;
    private String schedulingPool;
    private AccumulatorUpdate[] accumulatorUpdates;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getStageId() {
        return stageId;
    }

    public void setStageId(int stageId) {
        this.stageId = stageId;
    }

    public int getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(int attemptId) {
        this.attemptId = attemptId;
    }

    public int getNumActiveTasks() {
        return numActiveTasks;
    }

    public void setNumActiveTasks(int numActiveTasks) {
        this.numActiveTasks = numActiveTasks;
    }

    public int getNumCompleteTasks() {
        return numCompleteTasks;
    }

    public void setNumCompleteTasks(int numCompleteTasks) {
        this.numCompleteTasks = numCompleteTasks;
    }

    public int getNumFailedTasks() {
        return numFailedTasks;
    }

    public void setNumFailedTasks(int numFailedTasks) {
        this.numFailedTasks = numFailedTasks;
    }

    public int getExecutorRunTime() {
        return executorRunTime;
    }

    public void setExecutorRunTime(int executorRunTime) {
        this.executorRunTime = executorRunTime;
    }

    public String getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(String submissionTime) {
        this.submissionTime = submissionTime;
    }

    public String getFirstTaskLaunchedTime() {
        return firstTaskLaunchedTime;
    }

    public void setFirstTaskLaunchedTime(String firstTaskLaunchedTime) {
        this.firstTaskLaunchedTime = firstTaskLaunchedTime;
    }

    public String getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(String completionTime) {
        this.completionTime = completionTime;
    }

    public long getInputBytes() {
        return inputBytes;
    }

    public void setInputBytes(long inputBytes) {
        this.inputBytes = inputBytes;
    }

    public long getOutputBytes() {
        return outputBytes;
    }

    public void setOutputBytes(long outputBytes) {
        this.outputBytes = outputBytes;
    }

    public long getOutputRecords() {
        return outputRecords;
    }

    public void setOutputRecords(long outputRecords) {
        this.outputRecords = outputRecords;
    }

    public long getShuffleReadBytes() {
        return shuffleReadBytes;
    }

    public void setShuffleReadBytes(long shuffleReadBytes) {
        this.shuffleReadBytes = shuffleReadBytes;
    }

    public long getShuffleReadRecords() {
        return shuffleReadRecords;
    }

    public void setShuffleReadRecords(long shuffleReadRecords) {
        this.shuffleReadRecords = shuffleReadRecords;
    }

    public long getShuffleWriteBytes() {
        return shuffleWriteBytes;
    }

    public void setShuffleWriteBytes(long shuffleWriteBytes) {
        this.shuffleWriteBytes = shuffleWriteBytes;
    }

    public long getShuffleWriteRecords() {
        return shuffleWriteRecords;
    }

    public void setShuffleWriteRecords(long shuffleWriteRecords) {
        this.shuffleWriteRecords = shuffleWriteRecords;
    }

    public long getMemoryBytesSpilled() {
        return memoryBytesSpilled;
    }

    public void setMemoryBytesSpilled(long memoryBytesSpilled) {
        this.memoryBytesSpilled = memoryBytesSpilled;
    }

    public long getDiskBytesSpilled() {
        return diskBytesSpilled;
    }

    public void setDiskBytesSpilled(long diskBytesSpilled) {
        this.diskBytesSpilled = diskBytesSpilled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getSchedulingPool() {
        return schedulingPool;
    }

    public void setSchedulingPool(String schedulingPool) {
        this.schedulingPool = schedulingPool;
    }

    public AccumulatorUpdate[] getAccumulatorUpdates() {
        return accumulatorUpdates;
    }

    public void setAccumulatorUpdates(AccumulatorUpdate[] accumulatorUpdates) {
        this.accumulatorUpdates = accumulatorUpdates;
    }

    public long getInputRecords() {
        return inputRecords;
    }

    public void setInputRecords(long inputRecords) {
        this.inputRecords = inputRecords;
    }

    public static final List<Stage> EMPTY_LIST = Collections.unmodifiableList(new ArrayList<>(0));
}
