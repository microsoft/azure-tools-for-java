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
package com.microsoft.azure.hdinsight.spark.common;

import com.google.common.base.Joiner;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.intellij.openapi.compiler.CompilationException;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.packaging.impl.compiler.ArtifactsWorkspaceSettings;
import com.jcraft.jsch.*;
import com.microsoft.azure.hdinsight.common.HDInsightUtil;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.sdk.common.NotSupportExecption;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountTypeEnum;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.helpers.CallableSingleArg;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import rx.Single;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SparkSubmitHelper {
    private static SparkSubmitHelper ourInstance = new SparkSubmitHelper();

    private static final int MAX_INTERVAL_TIME = 5000;
    private static final int MIN_INTERVAL_TIME = 1000;
    private static final int INC_TIME = 100;

    private static final String applicationIdPattern = "Application report for ([^ ]*) \\(state: ACCEPTED\\)";

    private SparkJobLog sparkJobLog;

    public static SparkSubmitHelper getInstance() {
        return ourInstance;
    }

    private SparkSubmitHelper() {
    }

    private String JobLogFolderName = "SparkJobLog";
    public String writeLogToLocalFile(@NotNull Project project) throws IOException{
        if (sparkJobLog == null) {
            return null;
        }

        String applicationId = HDInsightUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().getApplicationId();
        String pluginRootPath = PluginUtil.getPluginRootDirectory();
        String folderPath = StringHelper.concat(pluginRootPath, File.separator, JobLogFolderName, File.separator, applicationId);
        String fullFileName = StringHelper.concat(folderPath, File.separator, "log.txt");

        File folder = null;
        FileWriter logFileWrite = null;
        BufferedWriter bufferedWriter = null;
        try {
            folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            logFileWrite = new FileWriter(fullFileName);
            bufferedWriter = new BufferedWriter(logFileWrite);
            for (String str : sparkJobLog.getLog()) {
                bufferedWriter.write(str);
                bufferedWriter.newLine();
            }
        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.flush();
                bufferedWriter.close();
            }

            if(logFileWrite != null) {
                logFileWrite.close();
            }

            return fullFileName;
        }
    }

    public void printRunningLogStreamingly(Project project, int id, IClusterDetail clusterDetail, Map<String, String> postEventProperty) throws IOException {
        try {
            boolean isFailedJob = false;
            boolean isKilledJob = false;

            int from_index = 0;
            int pre_index;
            int times = 0;
            HDInsightUtil.getSparkSubmissionToolWindowManager(project).setInfo("======================Begin printing out spark job log.=======================");
            while (true) {
                pre_index = from_index;
                if (HDInsightUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().isJobKilled()) {
                    isKilledJob = true;
                    break;
                }

                from_index = printoutJobLog(project, id, from_index, clusterDetail);
                HttpResponse statusHttpResponse = SparkBatchSubmission.getInstance().getBatchSparkJobStatus(JobUtils.getLivyConnectionURL(clusterDetail), id);

                SparkSubmitResponse status = new Gson().fromJson(statusHttpResponse.getMessage(), new TypeToken<SparkSubmitResponse>() {
                }.getType());

                // only the lines of the log are same between two http requests, we try to get the job status
                if (from_index == pre_index) {
                    String finalStatus = status.getState().toLowerCase();
                    if (finalStatus.equals("error") || finalStatus.equals("success") || finalStatus.equals("dead")) {
                        if (finalStatus.equals("error") || finalStatus.equals("dead")) {
                            isFailedJob = true;
                        }

                        if (!HDInsightUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().isJobKilled()) {
                            printoutJobLog(project, id, from_index, clusterDetail);
                            HDInsightUtil.getSparkSubmissionToolWindowManager(project).setInfo("======================Finish printing out spark job log.=======================");
                        } else {
                            isKilledJob = true;
                        }
                        break;
                    }
                }

                Thread.sleep(getIntervalTime(times));
                times++;
            }


            if (isKilledJob) {
                postEventProperty.put("IsKilled", "true");
                AppInsightsClient.create(HDInsightBundle.message("SparkSubmissionButtonClickEvent"), null, postEventProperty);
                return;
            }

            if (isFailedJob) {
                postEventProperty.put("IsRunningSucceed", "false");
                HttpResponse httpResponse = SparkBatchSubmission.getInstance()
                        .getBatchJobFullLog(clusterDetail.getConnectionUrl() + "/livy/batches", id);
                SparkSubmitResponse submitStatus = new Gson().fromJson(httpResponse.getMessage(),
                        new TypeToken<SparkSubmitResponse>() {
                        }.getType());
                if (submitStatus != null && submitStatus.getLog() != null && submitStatus.getLog().size() > 0) {
                    List<String> logs = submitStatus.getLog();
                    String log = logs.get(logs.size() - 1);
                    postEventProperty.put("SubmitFailedReason", HDInsightUtil.normalizeTelemetryMessage(log));
                }

                HDInsightUtil.getSparkSubmissionToolWindowManager(project).setError("Error : Your submitted job run failed");
            } else {
                postEventProperty.put("IsRunningSucceed", "true");
                HDInsightUtil.getSparkSubmissionToolWindowManager(project).setInfo("The Spark application completed successfully");
            }

            AppInsightsClient.create(HDInsightBundle.message("SparkSubmissionButtonClickEvent"), null, postEventProperty);
        } catch (Exception e) {
            if (HDInsightUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().isJobKilled() == false) {
                HDInsightUtil.getSparkSubmissionToolWindowManager(project).setError("Error : Failed to getting running log. Exception : " + e.toString());
                postEventProperty.put("SubmitFailedReason", HDInsightUtil.normalizeTelemetryMessage(e.toString()));
            } else {
                postEventProperty.put("IsKilled", "true");
                AppInsightsClient.create(HDInsightBundle.message("SparkSubmissionButtonClickEvent"), null, postEventProperty);
            }
        }
    }

    public Single<Artifact> buildArtifact(Project project, boolean isPrebuiltArtifact, Artifact artifact) {
        return Single.create(ob -> {
            if (isPrebuiltArtifact) {
                ob.onError(new NotSupportExecption());
                return;
            }

            final Set<Artifact> artifacts = Collections.singleton(artifact);
            ArtifactsWorkspaceSettings.getInstance(project).setArtifactsToBuild(artifacts);

            final CompileScope scope = ArtifactCompileScope.createArtifactsScope(project, artifacts, true);
            // Make is an async work
            CompilerManager.getInstance(project).make(scope, (aborted, errors, warnings, compileContext) -> {
                if (aborted || errors != 0) {
                    ob.onError(new CompilationException(Arrays.toString(compileContext.getMessages(CompilerMessageCategory.ERROR))));
                } else {
                    ob.onSuccess(artifact);
                }
            });
        });
    }

    private int printoutJobLog(Project project, int id, int from_index, IClusterDetail clusterDetail) throws IOException {
        HttpResponse httpResponse = SparkBatchSubmission.getInstance().getBatchJobFullLog(JobUtils.getLivyConnectionURL(clusterDetail), id);
        sparkJobLog = new Gson().fromJson(httpResponse.getMessage(), new TypeToken<SparkJobLog>() {
        }.getType());

        if (!HDInsightUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().isApplicationGenerated()) {
            String sparkLogs = Joiner.on("").join(sparkJobLog.getLog());
            String applicationId = getApplicationIdFromYarnLog(sparkLogs);
            if (applicationId != null) {
                HDInsightUtil.getSparkSubmissionToolWindowManager(project).setBrowserButtonState(true);
                HDInsightUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().setApplicationIdGenerated();
                HDInsightUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().setApplicationId(applicationId);
            }
        }

        int counter = 0;
        if (sparkJobLog.getLog().size() > 0) {
            for (String line : sparkJobLog.getLog()) {
                if (counter >= from_index && !StringHelper.isNullOrWhiteSpace(line)) {
                    HDInsightUtil.getSparkSubmissionToolWindowManager(project).setInfo(line, true);
                }

                counter++;
            }
        }

        return sparkJobLog.getTotal();
    }

    private int getIntervalTime(int times) {
        int interval = MIN_INTERVAL_TIME + times * INC_TIME;
        return interval > MAX_INTERVAL_TIME ? MAX_INTERVAL_TIME : interval;
    }

    private String getApplicationIdFromYarnLog(String yarnLog) {
        Pattern r = Pattern.compile(applicationIdPattern);
        Matcher m = r.matcher(yarnLog);
        if (m.find()) {
            return m.group(1);
        }

        return null;
    }

    public static boolean isLocalArtifactPath(String path) {
        if (StringHelper.isNullOrWhiteSpace(path)) {
            return false;
        }

        if (path.endsWith("!/")) {
            path = path.substring(0, path.length() - 2);
        }

        return path.endsWith(".jar");

    }

    public static final String HELP_LINK = "http://go.microsoft.com/fwlink/?LinkID=722349&clcid=0x409";
}
