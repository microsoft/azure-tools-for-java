/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest;

import com.microsoft.azure.hdinsight.sdk.rest.spark.Attempt;

public class AttemptWithAppId {
    private final String appId;
    private final Attempt attempt;
    private final String clusterName;

    public AttemptWithAppId(String cn, String id, Attempt attempt) {
        this.clusterName = cn;
        this.appId = id;
        this.attempt = attempt;
    }

    public String getAppId() {
        return appId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getStartTime ()
    {
        return attempt.getStartTime();
    }

    public String getSparkUser ()
    {
        return attempt.getSparkUser();
    }

    public String getDuration ()
    {
        return attempt.getDuration();
    }

    public String getLastUpdatedEpoch ()
    {
        return attempt.getLastUpdatedEpoch();
    }

    public String getStartTimeEpoch ()
    {
        return attempt.getStartTimeEpoch();
    }

    public String getLastUpdated ()
    {
        return attempt.getLastUpdated();
    }

    public String getEndTimeEpoch ()
    {
        return attempt.getEndTimeEpoch();
    }

    public String getEndTime ()
    {
        return attempt.getEndTime();
    }

    public String getCompleted ()
    {
        return attempt.getCompleted();
    }

    public String getAttemptId ()
    {
        return attempt.getAttemptId();
    }
}
