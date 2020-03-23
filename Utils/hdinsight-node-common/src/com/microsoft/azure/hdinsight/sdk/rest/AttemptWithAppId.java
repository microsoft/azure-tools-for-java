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
