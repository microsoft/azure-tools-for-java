/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.spark;

import com.microsoft.azure.hdinsight.sdk.rest.spark.event.JobStartEventLog;
import com.microsoft.azure.hdinsight.sdk.rest.spark.job.Job;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.App;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.List;

public class YarnAppWithJobs {
    private App app;
    private List<Job> jobs;
    private List<JobStartEventLog> startEventLogs;

    public YarnAppWithJobs() {
    }

    public YarnAppWithJobs(@NotNull App app, @NotNull List<Job> jobs, List<JobStartEventLog> startEventLogs) {
        this.app = app;
        this.jobs = jobs;
        this.startEventLogs = startEventLogs;
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    public List<Job> getJobs() {
        return jobs;
    }

    public void setJobs(List<Job> job) {
        this.jobs = job;
    }

    public List<JobStartEventLog> getStartEventLogs() {
        return startEventLogs;
    }

    public void setStartEventLogs(List<JobStartEventLog> startEventLogs) {
        this.startEventLogs = startEventLogs;
    }

}
