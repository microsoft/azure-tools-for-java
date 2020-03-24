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

package com.microsoft.azure.hdinsight.spark.jobs;

import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.rest.ObjectConvertUtils;
import com.microsoft.azure.hdinsight.sdk.rest.spark.Application;
import com.microsoft.azure.hdinsight.sdk.rest.spark.YarnAppWithJobs;
import com.microsoft.azure.hdinsight.sdk.rest.spark.event.JobStartEventLog;
import com.microsoft.azure.hdinsight.sdk.rest.spark.executor.Executor;
import com.microsoft.azure.hdinsight.sdk.rest.spark.job.Job;
import com.microsoft.azure.hdinsight.sdk.rest.spark.stage.Stage;
import com.microsoft.azure.hdinsight.sdk.rest.spark.task.Task;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.App;
import com.microsoft.azure.hdinsight.spark.jobs.framework.JobRequestDetails;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class SparkJobHttpHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        JobRequestDetails requestDetail = JobRequestDetails.getJobRequestDetail(httpExchange);
        try {
            String path = requestDetail.getRequestPath();
            if (path.equalsIgnoreCase("/applications/") && requestDetail.getAppId().equalsIgnoreCase("0")) {
                try {
                    List<Application> applications = SparkRestUtil.getSparkApplications(requestDetail.getCluster());
                    Optional<String> responseString = ObjectConvertUtils.convertObjectToJsonString(applications);
                    JobUtils.setResponse(httpExchange, responseString.orElseThrow(IOException::new));
                } catch (HDIException e) {
                    DefaultLoader.getUIHelper().logError("get applications list error", e);
                }
            } else if (path.contains("application_graph")) {
                ApplicationKey key = new ApplicationKey(requestDetail.getCluster(), requestDetail.getAppId());
                List<Job> jobs = JobViewCacheManager.getJob(key);
                App app = JobViewCacheManager.getYarnApp(key);
                List<JobStartEventLog> jobStartEventLogs = JobViewCacheManager.getJobStartEventLogs(key);
                YarnAppWithJobs yarnAppWithJobs = new YarnAppWithJobs(app, jobs, jobStartEventLogs);
                Optional<String> responseString = ObjectConvertUtils.convertObjectToJsonString(yarnAppWithJobs);
                JobUtils.setResponse(httpExchange, responseString.orElseThrow(IOException::new));
            } else if (path.contains("stages_summary")) {
                List<Stage> stages = JobViewCacheManager.getStages(new ApplicationKey(requestDetail.getCluster(), requestDetail.getAppId()));
                Optional<String> responseString = ObjectConvertUtils.convertObjectToJsonString(stages);
                JobUtils.setResponse(httpExchange, responseString.orElseThrow(IOException::new));
            } else if (path.contains("executors_summary")) {
                List<Executor> executors = JobViewCacheManager.getExecutors(new ApplicationKey(requestDetail.getCluster(), requestDetail.getAppId()));
                Optional<String> responseString = ObjectConvertUtils.convertObjectToJsonString(executors);
                JobUtils.setResponse(httpExchange, responseString.orElseThrow(IOException::new));
            } else if (path.contains("tasks_summary")) {
                List<Task> tasks = JobViewCacheManager.getTasks(new ApplicationKey(requestDetail.getCluster(), requestDetail.getAppId()));
                Optional<String> responseString = ObjectConvertUtils.convertObjectToJsonString(tasks);
                JobUtils.setResponse(httpExchange, responseString.orElseThrow(IOException::new));
            }
        } catch (ExecutionException e) {
            JobUtils.setResponse(httpExchange, e.getMessage(), 500);
        }
    }
}

