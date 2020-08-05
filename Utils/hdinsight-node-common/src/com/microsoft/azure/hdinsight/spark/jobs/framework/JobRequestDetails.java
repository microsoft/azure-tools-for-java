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

package com.microsoft.azure.hdinsight.spark.jobs.framework;

import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.sun.net.httpserver.HttpExchange;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class JobRequestDetails {

    private final Map<String, String> myQueriesMap;
    private final IClusterDetail myClusterDetail;
    private final String myRequestPath;
    private final HttpRequestType myHttpRequestType;
    private final String myAppId;

    private static final String HTTP_TYPE_TAG = "http-type";
    private static final String CLUSTER_NAME_TAG = "cluster-name";
    private static final String APP_ID_QUERY_KEY = "appId";

    private JobRequestDetails (@NotNull String requestPath, @NotNull Map<String, String> queriesMap) {
        this.myRequestPath = requestPath;
        this.myQueriesMap = queriesMap;
        final String clusterName = myQueriesMap.get(CLUSTER_NAME_TAG);
        this.myClusterDetail = JobViewManager.getCluster(clusterName);

        final String requestType = myQueriesMap.get(HTTP_TYPE_TAG);
        this.myHttpRequestType = HttpRequestType.fromString(requestType);

        this.myAppId = myQueriesMap.getOrDefault(APP_ID_QUERY_KEY, "0");
    }

    public String getAppId() {
        return myAppId;
    }

    public String getRequestPath() {
            return myRequestPath;
    }

    public static JobRequestDetails getJobRequestDetail(@NotNull HttpExchange httpExchange) {
        final URI myUri = httpExchange.getRequestURI();
        final String path = myUri.getPath();
        Map<String, String> queriesMap = splitQueryString(myUri);

        assert queriesMap.containsKey("cluster-name");
        assert queriesMap.containsKey("http-type");

        return new JobRequestDetails(path, queriesMap);
    }

    public boolean isSpecificApp() {
        return !myAppId.equals("0");
    }

    public IClusterDetail getCluster() {
        return myClusterDetail;
    }

    public HttpRequestType getRestType() {
        return myHttpRequestType;
    }

    private static Map<String, String> splitQueryString(URI uri) {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String query = uri.getRawQuery();

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                String key = URLDecoder.decode(pair.substring(0, idx), "utf-8");
                String value = URLDecoder.decode(pair.substring(idx + 1), "utf-8");
                query_pairs.put(key, value);
            } catch (Exception e) {
                DefaultLoader.getUIHelper().showError(e.getMessage(), "Spark job view http request decode error");
            }
        }
        return query_pairs;
    }
}
