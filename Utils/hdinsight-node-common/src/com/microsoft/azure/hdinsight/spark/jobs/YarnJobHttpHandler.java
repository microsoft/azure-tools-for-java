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
