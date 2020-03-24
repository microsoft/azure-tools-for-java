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

@JsonIgnoreProperties(ignoreUnknown=true)
public class TaskMetrics {
    private ShuffleReadMetrics shuffleReadMetrics;

    private long memoryBytesSpilled;

    private InputMetrics inputMetrics;

    private long jvmGcTime;

    private ShuffleWriteMetrics shuffleWriteMetrics;

    private long resultSerializationTime;

    private OutputMetrics outputMetrics;

    private long executorRunTime;

    private long diskBytesSpilled;

    private long resultSize;

    private long executorDeserializeTime;

    public ShuffleReadMetrics getShuffleReadMetrics ()
    {
        return shuffleReadMetrics;
    }

    public void setShuffleReadMetrics (ShuffleReadMetrics shuffleReadMetrics)
    {
        this.shuffleReadMetrics = shuffleReadMetrics;
    }

    public long getMemoryBytesSpilled ()
    {
        return memoryBytesSpilled;
    }

    public void setMemoryBytesSpilled (long memoryBytesSpilled)
    {
        this.memoryBytesSpilled = memoryBytesSpilled;
    }

    public InputMetrics getInputMetrics ()
    {
        return inputMetrics;
    }

    public void setInputMetrics (InputMetrics inputMetrics)
    {
        this.inputMetrics = inputMetrics;
    }

    public long getJvmGcTime ()
    {
        return jvmGcTime;
    }

    public void setJvmGcTime (long jvmGcTime)
    {
        this.jvmGcTime = jvmGcTime;
    }

    public ShuffleWriteMetrics getShuffleWriteMetrics ()
    {
        return shuffleWriteMetrics;
    }

    public void setShuffleWriteMetrics (ShuffleWriteMetrics shuffleWriteMetrics)
    {
        this.shuffleWriteMetrics = shuffleWriteMetrics;
    }

    public long getResultSerializationTime ()
    {
        return resultSerializationTime;
    }

    public void setResultSerializationTime (long resultSerializationTime)
    {
        this.resultSerializationTime = resultSerializationTime;
    }

    public OutputMetrics getOutputMetrics ()
    {
        return outputMetrics;
    }

    public void setOutputMetrics (OutputMetrics outputMetrics)
    {
        this.outputMetrics = outputMetrics;
    }

    public long getExecutorRunTime ()
    {
        return executorRunTime;
    }

    public void setExecutorRunTime (long executorRunTime)
    {
        this.executorRunTime = executorRunTime;
    }

    public long getDiskBytesSpilled ()
    {
        return diskBytesSpilled;
    }

    public void setDiskBytesSpilled (long diskBytesSpilled)
    {
        this.diskBytesSpilled = diskBytesSpilled;
    }

    public long getResultSize ()
    {
        return resultSize;
    }

    public void setResultSize (long resultSize)
    {
        this.resultSize = resultSize;
    }

    public long getExecutorDeserializeTime ()
    {
        return executorDeserializeTime;
    }

    public void setExecutorDeserializeTime (long executorDeserializeTime)
    {
        this.executorDeserializeTime = executorDeserializeTime;
    }
}
