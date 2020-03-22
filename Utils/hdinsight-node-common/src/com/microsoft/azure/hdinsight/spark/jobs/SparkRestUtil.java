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

import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.rest.AttemptWithAppId;
import com.microsoft.azure.hdinsight.sdk.rest.ObjectConvertUtils;
import com.microsoft.azure.hdinsight.sdk.rest.RestUtil;
import com.microsoft.azure.hdinsight.sdk.rest.spark.Application;
import com.microsoft.azure.hdinsight.sdk.rest.spark.event.JobStartEventLog;
import com.microsoft.azure.hdinsight.sdk.rest.spark.executor.Executor;
import com.microsoft.azure.hdinsight.sdk.rest.spark.job.Job;
import com.microsoft.azure.hdinsight.sdk.rest.spark.stage.Stage;
import com.microsoft.azure.hdinsight.sdk.rest.spark.task.Task;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.json.JSONObject;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SparkRestUtil {
    public static final String SPARK_REST_API_ENDPOINT = "%s/sparkhistory/api/v1/applications/%s";

    @NotNull
    public static List<Application> getSparkApplications(@NotNull IClusterDetail clusterDetail) throws HDIException, IOException {
        HttpEntity entity = getSparkRestEntity(clusterDetail, "");
        Optional<List<Application>> apps = ObjectConvertUtils.convertEntityToList(entity, Application.class);

        // spark job has at least one attempt
        return apps.orElse(RestUtil.getEmptyList(Application.class))
                .stream()
                .filter(app -> app.getAttempts().size() != 0 && app.getAttempts().get(0).getAttemptId() != null)
                .collect(Collectors.toList());
    }

    public static List<Executor> getAllExecutorFromApp(@NotNull ApplicationKey key) throws IOException, HDIException, ExecutionException {
        final AttemptWithAppId attemptWithAppId = getLastAttemptFromLocalCache(key);
        final HttpEntity entity = getSparkRestEntity(key.getClusterDetails(), String.format("/%s/%s/executors", attemptWithAppId.getAppId(), attemptWithAppId.getAttemptId()));
        Optional<List<Executor>> executors = ObjectConvertUtils.convertEntityToList(entity, Executor.class);
        return executors.orElse(RestUtil.getEmptyList(Executor.class));
    }

    public static List<Stage> getAllStageFromApp(@NotNull ApplicationKey key) throws IOException, HDIException, ExecutionException {
        final AttemptWithAppId attemptWithAppId = getLastAttemptFromLocalCache(key);
        final HttpEntity entity = getSparkRestEntity(key.getClusterDetails(), String.format("/%s/%s/stages", attemptWithAppId.getAppId(), attemptWithAppId.getAttemptId()));
        final Optional<List<Stage>> stages = ObjectConvertUtils.convertEntityToList(entity, Stage.class);
        return stages.orElse(RestUtil.getEmptyList(Stage.class));
    }

    public static List<Job> getLastAttemptJobsFromApp(@NotNull ApplicationKey key) throws IOException, HDIException, ExecutionException {
        AttemptWithAppId attemptWithAppId = getLastAttemptFromLocalCache(key);
        return getSparkJobsFromApp(key.getClusterDetails(), key.getAppId(), attemptWithAppId.getAttemptId());
    }

    public static List<Job> getSparkJobsFromApp(@NotNull IClusterDetail clusterDetail, @NotNull String appId, @NotNull String attemptId) throws IOException, HDIException {
        HttpEntity entity = getSparkRestEntity(clusterDetail, String.format("/%s/%s/jobs", appId, attemptId));
        Optional<List<Job>> apps = ObjectConvertUtils.convertEntityToList(entity, Job.class);
        return apps.orElse(RestUtil.getEmptyList(Job.class));
    }

    public static List<Task> getSparkTasks(@NotNull ApplicationKey key, @NotNull int stage, int attemptId) throws IOException, ExecutionException, HDIException {
        AttemptWithAppId attemptWithAppId = getLastAttemptFromLocalCache(key);
        String url = String.format("/%s/%s/stages/%s/%s/taskList", attemptWithAppId.getAppId(), attemptWithAppId.getAttemptId(),stage, attemptId);
        HttpEntity entity = getSparkRestEntity(key.getClusterDetails(), url);

        Optional<List<Task>> tasks = ObjectConvertUtils.convertEntityToList(entity, Task.class);
        return tasks.orElse(RestUtil.getEmptyList(Task.class));
    }

    public static List<JobStartEventLog> getSparkEventLogs(@NotNull ApplicationKey key) throws HDIException, IOException {
        String url = String.format("%s/logs", key.getAppId());
        String eventLogsPath = String.format("%s/SparkEventLogs/%s/eventLogs.zip", HDInsightLoader.getHDInsightHelper().getPluginRootPath(), key.getAppId());
        File file = new File(eventLogsPath);
        HttpEntity entity = getSparkRestEntity(key.getClusterDetails(), url);
        InputStream inputStream = entity.getContent();
        FileUtils.copyInputStreamToFile(inputStream, file);
        IOUtils.closeQuietly(inputStream);

        ZipFile zipFile = new ZipFile(file);
        List<? extends ZipEntry> entities =  Collections.list(zipFile.entries());
        // every application has an attempt in event log
        // and the entity name should be in formation "{appId}_{attemptId}"
        String entityName = String.format("%s_%s", key.getAppId(), entities.size());
        ZipEntry lastEntity = zipFile.getEntry(entityName);
        if (lastEntity == null) {
            throw new HDIException(String.format("No Spark event log entity found for app: %s", key.getAppId()));
        }
        InputStream zipFileInputStream = zipFile.getInputStream(lastEntity);
        String entityContent = IOUtils.toString(zipFileInputStream, Charset.forName("utf-8"));

        String[] lines = entityContent.split("\n");
        List<JobStartEventLog> jobStartEvents = Arrays.stream(lines)
                .filter(line -> {
                    JSONObject jsonObject = new JSONObject(line);
                    String eventName = jsonObject.getString("Event");
                    return eventName.equalsIgnoreCase("SparkListenerJobStart");
                    })
                .map(oneLine -> ObjectConvertUtils.convertToObjectQuietly(oneLine, JobStartEventLog.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return jobStartEvents;
    }

    private static AttemptWithAppId getLastAttemptFromLocalCache(@NotNull ApplicationKey key) throws ExecutionException, HDIException {
        List<Application> sparkApplications = JobViewCacheManager.getSparkApplications(key.getClusterDetails());
        Optional<Application> selectedApplication = sparkApplications.stream().filter(application -> application.getId().equalsIgnoreCase(key.getAppId())
        ).findFirst();
        return selectedApplication.orElseThrow(()-> new HDIException(String.format("application %s on cluster %s can't find", key.getAppId(), key.getClusterDetails().getName()))).getLastAttemptWithAppId(key.getClusterDetails().getName());
    }

    private static HttpEntity getSparkRestEntity(@NotNull IClusterDetail clusterDetail, @NotNull String restUrl) throws HDIException, IOException {
        String connUrl = clusterDetail.getConnectionUrl();
        connUrl = connUrl.endsWith("/") ? connUrl.substring(0, connUrl.length() - 1) : connUrl;
        final String url = String.format(SPARK_REST_API_ENDPOINT, connUrl, restUrl);
        return JobUtils.getEntity(clusterDetail, url);
    }
}
