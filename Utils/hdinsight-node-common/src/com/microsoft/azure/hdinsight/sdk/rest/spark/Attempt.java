/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
