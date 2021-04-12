/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.jobs;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.rest.spark.Application;
import com.microsoft.azure.hdinsight.sdk.rest.spark.event.JobStartEventLog;
import com.microsoft.azure.hdinsight.sdk.rest.spark.executor.Executor;
import com.microsoft.azure.hdinsight.sdk.rest.spark.job.Job;
import com.microsoft.azure.hdinsight.sdk.rest.spark.stage.Stage;
import com.microsoft.azure.hdinsight.sdk.rest.spark.task.Task;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.App;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.ApplicationMasterLogs;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class JobViewCacheManager {
    private static final LoadingCache<ApplicationKey, List<Job>> sparkJobLocalCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .initialCapacity(20)
            .build(new CacheLoader<ApplicationKey, List<Job>>() {
                @Override
                public List<Job> load(ApplicationKey key) throws Exception {
                    return SparkRestUtil.getLastAttemptJobsFromApp(key);
                }
            });

    private static final LoadingCache<ApplicationKey, List<Stage>> sparkStageLocalCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .initialCapacity(20)
            .build(new CacheLoader<ApplicationKey, List<Stage>>() {
                @Override
                public List<Stage> load(ApplicationKey key) throws Exception {
                    return SparkRestUtil.getAllStageFromApp(key);
                }
            });

    private static final LoadingCache<ApplicationKey, List<Executor>> sparkExecutorLocalCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .initialCapacity(20)
            .build(new CacheLoader<ApplicationKey, List<Executor>>() {
                @Override
                public List<Executor> load(ApplicationKey key) throws Exception {
                    return SparkRestUtil.getAllExecutorFromApp(key);
                }
            });

    private static final LoadingCache<String, List<Application>> sparkApplicationsLocalCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .initialCapacity(20)
            .build(new CacheLoader<String, List<Application>>() {
                @Override
                public List<Application> load(String key) throws Exception {
                    return SparkRestUtil.getSparkApplications(JobViewManager.getCluster(key));
                }
            });

    private static final LoadingCache<ApplicationKey, List<Task>> sparkTasksSummaryLocalCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .initialCapacity(20)
            .build(new CacheLoader<ApplicationKey, List<Task>>() {
                @Override
                public List<Task> load(ApplicationKey key) throws Exception {
                    List<Task> allTasks = new ArrayList<>();
                    List<Stage> stages = sparkStageLocalCache.get(key);
                    for (Stage stage: stages) {
                        int stageId = stage.getStageId();
                        int attemptedId = stage.getAttemptId();
                        List<Task> tasks = SparkRestUtil.getSparkTasks(key, stageId, attemptedId);
                        allTasks.addAll(tasks);
                    }
                    return allTasks;
                }
            });

    private static final LoadingCache<ApplicationKey, ApplicationMasterLogs> yarnAppLogLocalCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .initialCapacity(20)
            .build(new CacheLoader<ApplicationKey, ApplicationMasterLogs>() {
                @Override
                public ApplicationMasterLogs load(ApplicationKey key) throws Exception {
                    return JobUtils.getYarnLogs(key);
                }
            });

    private static final LoadingCache<ApplicationKey, App> yarnApplicationLocalCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .initialCapacity(20)
            .build(new CacheLoader<ApplicationKey, App>() {
                @Override
                public App load(ApplicationKey key) throws Exception {
                    return YarnRestUtil.getApp(key);
                }
            });

    private static final LoadingCache<ApplicationKey, List<JobStartEventLog>> sparkJobStartEventLogCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .initialCapacity(20)
            .build(new CacheLoader<ApplicationKey, List<JobStartEventLog>>() {
                @Override
                public List<JobStartEventLog> load(ApplicationKey key) throws Exception {
                    return SparkRestUtil.getSparkEventLogs(key);
                }
            });

    public static List<JobStartEventLog> getJobStartEventLogs(@NotNull ApplicationKey key) throws ExecutionException {
        return sparkJobStartEventLogCache.get(key);
    }

    public static ApplicationMasterLogs getYarnLogs(@NotNull ApplicationKey key) throws ExecutionException {
            return yarnAppLogLocalCache.get(key);
    }

    public static App getYarnApp(@NotNull ApplicationKey key) throws ExecutionException {
        return yarnApplicationLocalCache.get(key);
    }

    public static List<Application> getSparkApplications(@NotNull IClusterDetail clusterDetail) throws ExecutionException {
        return sparkApplicationsLocalCache.get(clusterDetail.getName());
    }

    public static Application getSingleSparkApplication(@NotNull ApplicationKey key) throws ExecutionException {
        List<Application> apps = sparkApplicationsLocalCache.get(key.getClusterDetails().getName());
        for(Application application : apps) {
            if (application.getId().equalsIgnoreCase(key.getAppId())) {
                return application;
            }
        }
        return null;
    }

    public static List<Executor> getExecutors(@NotNull ApplicationKey key) throws ExecutionException {
        return sparkExecutorLocalCache.get(key);
    }

    public static List<Job> getJob(@NotNull ApplicationKey key) throws ExecutionException {
        return sparkJobLocalCache.get(key);
    }

    public static List<Task> getTasks(@NotNull ApplicationKey key) throws ExecutionException {
        return sparkTasksSummaryLocalCache.get(key);
    }

    public static List<Stage> getStages(@NotNull ApplicationKey key) throws ExecutionException {
        return sparkStageLocalCache.get(key);
    }
}
