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

