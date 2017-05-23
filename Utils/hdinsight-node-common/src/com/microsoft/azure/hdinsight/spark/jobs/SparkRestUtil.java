/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.spark.jobs;

import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.rest.AttemptWithAppId;
import com.microsoft.azure.hdinsight.sdk.rest.ObjectConvertUtils;
import com.microsoft.azure.hdinsight.sdk.rest.spark.Application;
import com.microsoft.azure.hdinsight.sdk.rest.spark.executor.Executor;
import com.microsoft.azure.hdinsight.sdk.rest.spark.job.Job;
import com.microsoft.azure.hdinsight.sdk.rest.spark.stage.Stage;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.HttpEntity;


import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SparkRestUtil {
    public static final String SPARK_REST_API_ENDPOINT = "%s/sparkhistory/api/v1/%s";

    @NotNull
    public static List<Application> getSparkApplications(@NotNull IClusterDetail clusterDetail) throws HDIException, IOException {
        HttpEntity entity = getSparkRestEntity(clusterDetail, "applications");
        Optional<List<Application>> apps = ObjectConvertUtils.convertEntityToList(entity, Application.class);

        // spark job has at least one attempt
        return apps.orElse(Application.EMPTY_LIST)
                .stream()
                .filter(app -> app.getAttempts().size() != 0 && app.getAttempts().get(0).getAttemptId() != null)
                .collect(Collectors.toList());
    }

    public static List<Executor> getAllExecutorFromApp(@NotNull ApplicationKey key) throws IOException, HDIException {
        final Application app = JobViewManager.getCachedApp(key);
        final AttemptWithAppId attemptWithAppId = app.getLastAttemptWithAppId();
        final HttpEntity entity = getSparkRestEntity(key.getClusterDetails(), String.format("/%s/%s/executors", app.getId(), app.getLastAttemptId()));
        Optional<List<Executor>> executors = ObjectConvertUtils.convertEntityToList(entity, Executor.class);
        return executors.orElse(Executor.EMPTY_LIST);
    }

    public static List<Stage> getAllStageFromApp(@NotNull ApplicationKey key) throws IOException, HDIException {
        final Application app = JobViewManager.getCachedApp(key);
        final HttpEntity entity = getSparkRestEntity(key.getClusterDetails(), String.format("/%s/%s/jobs", app.getId(), app.getLastAttemptId()));
        final Optional<List<Stage>> stages = ObjectConvertUtils.convertEntityToList(entity, Stage.class);
        return stages.orElse(Stage.EMPTY_LIST);
    }

    public static List<Job> getLastAttemptJobsFromApp(@NotNull ApplicationKey key) throws IOException, HDIException {
        final Application app = JobViewManager.getCachedApp(key);
        AttemptWithAppId attemptWithAppId = app.getLastAttemptWithAppId();
        return getSparkJobsFromApp(key.getClusterDetails(), key.getAppId(), attemptWithAppId.getAttemptId());
    }

    public static List<Job> getSparkJobsFromApp(@NotNull IClusterDetail clusterDetail, @NotNull String appId, @NotNull String attemptId) throws IOException, HDIException {
        HttpEntity entity = getSparkRestEntity(clusterDetail, String.format("/%s/%s/jobs", appId, attemptId));
        Optional<List<Job>> apps = ObjectConvertUtils.convertEntityToList(entity, Job.class);
        return apps.orElse(Job.EMPTY_LIST);
    }

    private static HttpEntity getSparkRestEntity(@NotNull IClusterDetail clusterDetail, @NotNull String restUrl) throws HDIException, IOException {
        final String url = String.format(SPARK_REST_API_ENDPOINT, clusterDetail.getConnectionUrl(), restUrl);
        return JobUtils.getEntity(clusterDetail, url);
    }
}
