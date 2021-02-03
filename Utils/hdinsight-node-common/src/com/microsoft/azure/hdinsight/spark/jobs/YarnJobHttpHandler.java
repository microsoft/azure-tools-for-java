/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.jobs;

import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.rest.ObjectConvertUtils;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.App;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.ApplicationMasterLogs;
import com.microsoft.azure.hdinsight.spark.jobs.framework.JobRequestDetails;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class YarnJobHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        JobRequestDetails requestDetail = JobRequestDetails.getJobRequestDetail(httpExchange);
        String path = requestDetail.getRequestPath();
        try {
            if (path.contains("/apps/app") && requestDetail.isSpecificApp()) {
                App app = JobViewCacheManager.getYarnApp(new ApplicationKey(requestDetail.getCluster(), requestDetail.getAppId()));
                Optional<String> responseString = ObjectConvertUtils.convertObjectToJsonString(app);
                JobUtils.setResponse(httpExchange, responseString.orElseThrow(IOException::new));
            } else if (path.contains("/apps/logs") && requestDetail.isSpecificApp()) {
                ApplicationMasterLogs logs = JobViewCacheManager.getYarnLogs(new ApplicationKey(requestDetail.getCluster(), requestDetail.getAppId()));
                Optional<String> responseString = ObjectConvertUtils.convertObjectToJsonString(logs);
                JobUtils.setResponse(httpExchange, responseString.orElseThrow(IOException::new));
            }
        } catch (ExecutionException e) {
            JobUtils.setResponse(httpExchange, e.getMessage(), 500);
        }
    }
}
