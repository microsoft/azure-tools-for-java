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

package com.microsoft.azure.hdinsight.sdk.rest.spark;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Attempt implements IConvertible {
    private String startTime;

    private String sparkUser;

    private String duration;

    private String lastUpdatedEpoch;

    private String startTimeEpoch;

    private String lastUpdated;

    private String endTimeEpoch;

    private String endTime;

    private String completed;

    private String attemptId;

    private String appSparkVersion;

    public String getAppSparkVersion(){
        return appSparkVersion;
    }

    public String getStartTime ()
    {
        return startTime;
    }

    public void setStartTime (String startTime)
    {
        this.startTime = startTime;
    }

    public String getSparkUser ()
    {
        return sparkUser;
    }

    public void setSparkUser (String sparkUser)
    {
        this.sparkUser = sparkUser;
    }

    public String getDuration ()
    {
        return duration;
    }

    public void setDuration (String duration)
    {
        this.duration = duration;
    }

    public String getLastUpdatedEpoch ()
    {
        return lastUpdatedEpoch;
    }

    public void setLastUpdatedEpoch (String lastUpdatedEpoch)
    {
        this.lastUpdatedEpoch = lastUpdatedEpoch;
    }

    public String getStartTimeEpoch ()
    {
        return startTimeEpoch;
    }

    public void setStartTimeEpoch (String startTimeEpoch)
    {
        this.startTimeEpoch = startTimeEpoch;
    }

    public String getLastUpdated ()
    {
        return lastUpdated;
    }

    public void setLastUpdated (String lastUpdated)
    {
        this.lastUpdated = lastUpdated;
    }

    public String getEndTimeEpoch ()
    {
        return endTimeEpoch;
    }

    public void setEndTimeEpoch (String endTimeEpoch)
    {
        this.endTimeEpoch = endTimeEpoch;
    }

    public String getEndTime ()
    {
        return endTime;
    }

    public void setEndTime (String endTime)
    {
        this.endTime = endTime;
    }

    public String getCompleted ()
    {
        return completed;
    }

    public void setCompleted (String completed)
    {
        this.completed = completed;
    }

    public String getAttemptId ()
    {
        return attemptId;
    }

    public void setAttemptId (String attemptId)
    {
        this.attemptId = attemptId;
    }
}
