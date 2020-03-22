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

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestDetail {
    private final String clusterFormatId;
    private final String restUrl;
    private final HttpRequestType apiType;

    private static Pattern clusterPattern = Pattern.compile("^/clusters/([^/]*)(/.*)");


    private final String getSparkPreRestUrl(@NotNull final String clusterName) {
        return ClusterManagerEx.getInstance().getClusterConnectionString(clusterName) + "sparkhistory/api/v1";
    }

    private final String getYarnPreRestUrl(@NotNull final String clusterName) {
        return ClusterManagerEx.getInstance().getClusterConnectionString(clusterName) + "yarnui/ws/v1";
    }

    private  final String getYarnHistoryUrl(@NotNull final String clusterName) {
        return ClusterManagerEx.getInstance().getClusterConnectionString(clusterName) + "yarnui";
    }
    private final String getLivyBatchesRestUrl(@NotNull final String clusterName) {
        return ClusterManagerEx.getInstance().getClusterConnectionString(clusterName) + "livy/batches";
    }

    @NotNull
    private IClusterDetail clusterDetail;

    private Map<String, String> queriesMap = new HashMap<>();

    public static RequestDetail getRequestDetail(@NotNull HttpExchange httpExchange) {
        final Headers headers = httpExchange.getRequestHeaders();
        final String clusterName = headers.getFirst("Cluster-name");
        final String ResetType = headers.getFirst("Rest-type");

        // TODO :
        return null;
    }

    @Nullable
    public static RequestDetail getRequestDetail(@NotNull URI myUrl) {
        String[] queries = myUrl.getQuery() == null ? null : myUrl.getQuery().split("&");
        String path = myUrl.getPath();
        Matcher matcher = clusterPattern.matcher(path);
        if (matcher.find()) {
            return new RequestDetail(matcher.group(1), matcher.group(2), queries);
        }
        return null;
    }

    public RequestDetail(@NotNull String clusterFormatId, @NotNull String restUrl, @Nullable String[] queries) {
        this.clusterFormatId = clusterFormatId;
        clusterDetail = JobViewManager.getCluster(clusterFormatId);

        this.restUrl = restUrl;
        if (queries != null) {
            for (String str : queries) {
                String[] conditions = str.split("=");
                if (conditions.length == 2) {
                    this.queriesMap.put(conditions[0], conditions[1]);
                }
            }
        }

        if (queriesMap.containsKey("restType")) {
            String type = queriesMap.get("restType");
            if (type.equalsIgnoreCase("yarn")) {
                apiType = HttpRequestType.YarnRest;
            } else if (type.equalsIgnoreCase("yarnhistory")) {
                apiType = HttpRequestType.YarnHistory;
            } else if (type.equalsIgnoreCase("livy")) {
                apiType = HttpRequestType.LivyBatchesRest;
            } else {
                apiType = HttpRequestType.SparkRest;
            }
        } else if(isMultiQuery()) {
            apiType = HttpRequestType.MultiTask;
        } else {
            apiType = HttpRequestType.SparkRest;
        }
    }

    public IClusterDetail getClusterDetail() {
        return JobViewManager.getCluster(clusterFormatId);
    }

    @NotNull
    private String getPreURl(@NotNull final String clusterName) {
        String preUrl = null;
        switch (getApiType()) {
            case YarnHistory:
                preUrl = getYarnHistoryUrl(clusterName);
                break;
            case YarnRest:
                preUrl = getYarnPreRestUrl(clusterName);
                break;
            case LivyBatchesRest:
                preUrl = getLivyBatchesRestUrl(clusterName);
                break;
            default:
                preUrl = getSparkPreRestUrl(clusterName);
        }
        return preUrl;
    }

    public String getQueryUrl() {
        String queryUrl = getPreURl(clusterDetail.getName()) + getRestUrl();
        // get error message for Yarn website
        if (getApiType() == HttpRequestType.YarnHistory) {
            if(queryUrl.endsWith("stderr")) {
                queryUrl = queryUrl + "?start=0";
            }
        }
        return queryUrl;
    }

    private static final String MULTI_STAGE_TAG = "multi-stages";

    public boolean isMultiQuery() {
        return queriesMap.containsKey(MULTI_STAGE_TAG);
    }

    private int getQueryNumber() {
        return Integer.valueOf(queriesMap.get(MULTI_STAGE_TAG));
    }

    public List<String> getQueryUrls() {
        if(isMultiQuery()) {
            String applicationId = queriesMap.get("applicationId");
            String attemptdId = queriesMap.get("attemptId");

            List<String> queries = new ArrayList<>();
            for(int i = 0; i < getQueryNumber(); ++i) {
                String url = getSparkPreRestUrl(clusterDetail.getName()) + "/applications/%s/%s/stages/%s";
                String query = String.format(url, applicationId, attemptdId, String.valueOf(i));
                queries.add(query);
            }
            return queries;
        } else {
            return null;
        }

    }

    public String getClusterFormatId() {
        return clusterFormatId;
    }

    private String getRestUrl() {
        return restUrl;
    }

    public HttpRequestType getApiType() {
        return apiType;
    }

    @Nullable
    public String getProperty(@NotNull String key) {
        return queriesMap.get(key);
    }
}