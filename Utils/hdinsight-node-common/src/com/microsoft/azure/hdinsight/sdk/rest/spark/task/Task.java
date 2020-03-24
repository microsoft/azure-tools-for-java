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

package com.microsoft.azure.hdinsight.sdk.rest.spark.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A spark task resource contains information about a task of that was submitted to a cluster.
 *
 * Based on Spark 2.1.0, refer to http://spark.apache.org/docs/latest/monitoring.html
 *
 *   http://<spark http address:port>/applications/[app-id]/stages/[stage-id]/[stage-attempt-id]/taskList
 *
 * HTTP Operations Supported
 *   GET
 *
 * Query Parameters Supported
 *   None
 */

@JsonIgnoreProperties(ignoreUnknown=true)
public class Task {
    private String attempt;

    private String taskId;

    private String executorId;

    private String index;

    private String host;

    private String launchTime;

    private TaskMetrics taskMetrics;

    private String speculative;

    private String taskLocality;

    private String[] accumulatorUpdates;

    public String getAttempt ()
    {
        return attempt;
    }

    public void setAttempt (String attempt)
    {
        this.attempt = attempt;
    }

    public String getTaskId ()
    {
        return taskId;
    }

    public void setTaskId (String taskId)
    {
        this.taskId = taskId;
    }

    public String getExecutorId ()
    {
        return executorId;
    }

    public void setExecutorId (String executorId)
    {
        this.executorId = executorId;
    }

    public String getIndex ()
    {
        return index;
    }

    public void setIndex (String index)
    {
        this.index = index;
    }

    public String getHost ()
    {
        return host;
    }

    public void setHost (String host)
    {
        this.host = host;
    }

    public String getLaunchTime ()
    {
        return launchTime;
    }

    public void setLaunchTime (String launchTime)
    {
        this.launchTime = launchTime;
    }

    public TaskMetrics getTaskMetrics ()
    {
        return taskMetrics;
    }

    public void setTaskMetrics (TaskMetrics taskMetrics)
    {
        this.taskMetrics = taskMetrics;
    }

    public String getSpeculative ()
    {
        return speculative;
    }

    public void setSpeculative (String speculative)
    {
        this.speculative = speculative;
    }

    public String getTaskLocality ()
    {
        return taskLocality;
    }

    public void setTaskLocality (String taskLocality)
    {
        this.taskLocality = taskLocality;
    }

    public String[] getAccumulatorUpdates ()
    {
        return accumulatorUpdates;
    }

    public void setAccumulatorUpdates (String[] accumulatorUpdates)
    {
        this.accumulatorUpdates = accumulatorUpdates;
    }
}
