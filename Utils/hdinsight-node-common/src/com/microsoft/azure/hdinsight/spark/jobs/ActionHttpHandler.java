/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.jobs;

import com.microsoft.azure.hdinsight.sdk.rest.spark.Application;
import com.microsoft.azure.hdinsight.spark.jobs.framework.JobRequestDetails;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ActionHttpHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        JobRequestDetails requestDetail = JobRequestDetails.getJobRequestDetail(httpExchange);

        final String path = requestDetail.getRequestPath();
        final String clusterConnectString = requestDetail.getCluster().getConnectionUrl();
        if (path.contains("yarnui")) {
            JobUtils.openYarnUIHistory(clusterConnectString, requestDetail.getAppId());
        } else if (path.contains("sparkui")) {
            try {
                Application application = JobViewCacheManager.getSingleSparkApplication(new ApplicationKey(requestDetail.getCluster(), requestDetail.getAppId()));
                JobUtils.openSparkUIHistory(clusterConnectString, requestDetail.getAppId(), application.getLastAttemptId());
                JobUtils.setResponse(httpExchange, "open browser successfully");
            } catch (ExecutionException e) {
                JobUtils.setResponse(httpExchange, "open browser error", 500);
                DefaultLoader.getUIHelper().showError(e.getMessage(), "open browser error");
            }
        }
    }
}

