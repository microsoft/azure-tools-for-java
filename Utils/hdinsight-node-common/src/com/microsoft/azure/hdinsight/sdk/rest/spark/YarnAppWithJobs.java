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
