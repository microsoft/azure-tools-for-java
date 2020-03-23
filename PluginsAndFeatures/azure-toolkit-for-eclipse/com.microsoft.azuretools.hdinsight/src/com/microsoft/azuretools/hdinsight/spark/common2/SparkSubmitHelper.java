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

package com.microsoft.azuretools.hdinsight.spark.common2;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.HDINSIGHT;

import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.sdk.cluster.EmulatorClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountType;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.azure.hdinsight.spark.common.SparkJobLog;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitResponse;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.tooling.msservices.helpers.CallableSingleArg;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.hdinsight.Activator;
import com.microsoft.azuretools.hdinsight.common2.HDInsightUtil;
import com.microsoft.azuretools.core.utils.Messages;

public class SparkSubmitHelper {
    private static SparkSubmitHelper ourInstance = new SparkSubmitHelper();

    private static final int MAX_INTERVAL_TIME = 5000;
    private static final int MIN_INTERVAL_TIME = 1000;
    private static final int INC_TIME = 100;

    private static final String APPLICATION_ID_PATTERN = "Application report for ([^ ]*) \\(state: ACCEPTED\\)";
    public static final String HELP_LINK = "http://go.microsoft.com/fwlink/?LinkID=722349&clcid=0x409";

    private SparkJobLog sparkJobLog;

    public static SparkSubmitHelper getInstance() {
        return ourInstance;
    }

    private SparkSubmitHelper() {
    }

    private String JobLogFolderName = "SparkJobLog";

    public String writeLogToLocalFile(/* @NotNull Project project */) throws IOException {
        if (sparkJobLog == null) {
            return null;
        }

        String applicationId = HDInsightUtil.getSparkSubmissionToolWindowView().getJobStatusManager()
                .getApplicationId();
        String pluginRootPath = PluginUtil.pluginFolder;// PluginUtil.getPluginRootDirectory();
        String folderPath = StringHelper.concat(pluginRootPath, File.separator, JobLogFolderName, File.separator,
                applicationId);
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

            if (logFileWrite != null) {
                logFileWrite.close();
            }

            return fullFileName;
        }
    }

    public void printRunningLogStreamingly(/* Project project, */ int id, IClusterDetail clusterDetail,
            Map<String, String> postEventProperty) throws IOException {
        try {
            boolean isFailedJob = false;
            boolean isKilledJob = false;

            int from_index = 0;
            int pre_index;
            int times = 0;
            HDInsightUtil.getSparkSubmissionToolWindowView()
                    .setInfo("======================Begin printing out spark job log.=======================");
            while (true) {
                pre_index = from_index;
                if (HDInsightUtil.getSparkSubmissionToolWindowView().getJobStatusManager().isJobKilled()) {
                    isKilledJob = true;
                    break;
                }

                from_index = printoutJobLog(/* project, */id, from_index, clusterDetail);
                HttpResponse statusHttpResponse = SparkBatchSubmission.getInstance()
                        .getBatchSparkJobStatus(clusterDetail.getConnectionUrl() + "/livy/batches", id);

                SparkSubmitResponse status = new Gson().fromJson(statusHttpResponse.getMessage(),
                        new TypeToken<SparkSubmitResponse>() {
                        }.getType());

                // only the lines of the log are same between two http requests,
                // we try to get the job status
                if (from_index == pre_index) {
                    String finalStatus = status.getState().toLowerCase();
                    if (finalStatus.equals("error") || finalStatus.equals("success") || finalStatus.equals("dead")) {
                        if (finalStatus.equals("error") || finalStatus.equals("dead")) {
                            isFailedJob = true;
                        }

                        if (!HDInsightUtil.getSparkSubmissionToolWindowView().getJobStatusManager().isJobKilled()) {
                            printoutJobLog(id, from_index, clusterDetail);
                            HDInsightUtil.getSparkSubmissionToolWindowView().setInfo(
                                    "======================Finish printing out spark job log.=======================");
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
                AppInsightsClient.create(Messages.SparkSubmissionButtonClickEvent,
                        Activator.getDefault().getBundle().getVersion().toString(), postEventProperty);
                EventUtil.logEvent(EventType.info, HDINSIGHT, Messages.SparkSubmissionButtonClickEvent, null);
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
                    postEventProperty.put("SubmitFailedReason", truncateTelemetryMessage(logs.get(logs.size() - 1)));
                }

                HDInsightUtil.getSparkSubmissionToolWindowView().setError("Error : Your submitted job run failed");
            } else {
                postEventProperty.put("IsRunningSucceed", "true");
                HDInsightUtil.getSparkSubmissionToolWindowView()
                        .setInfo("The Spark application completed successfully");
            }

            AppInsightsClient.create(Messages.SparkSubmissionButtonClickEvent,
                    Activator.getDefault().getBundle().getVersion().toString(), postEventProperty);
            EventUtil.logEvent(EventType.info, HDINSIGHT, Messages.SparkSubmissionButtonClickEvent, null);
        } catch (Exception e) {
            if (HDInsightUtil.getSparkSubmissionToolWindowView().getJobStatusManager().isJobKilled() == false) {
                HDInsightUtil.getSparkSubmissionToolWindowView()
                        .setError("Error : Failed to getting running log. Exception : " + e.toString());

                postEventProperty.put("SubmitFailedReason", truncateTelemetryMessage(e.toString()));
            } else {
                postEventProperty.put("IsKilled", "true");
            }
            AppInsightsClient.create(Messages.SparkSubmissionButtonClickEvent,
                    Activator.getDefault().getBundle().getVersion().toString(), postEventProperty);
            EventUtil.logEvent(EventType.info, HDINSIGHT, Messages.SparkSubmissionButtonClickEvent, null);
        }
    }

    public String uploadFileToADL(/* Project project, */ String localFile,
                                  IHDIStorageAccount storageAccount,
            String defaultContainerName, String uploadFolderPath) throws Exception {
        final File file = new File(localFile);
        if (storageAccount.getAccountType() == StorageAccountType.BLOB) {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                try (BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
                    final CallableSingleArg<Void, Long> callable = new CallableSingleArg<Void, Long>() {
                        @Override
                        public Void call(Long uploadedBytes) throws Exception {
                            double progress = ((double) uploadedBytes) / file.length();
                            return null;
                        }
                    };

                    HDStorageAccount blobStorageAccount = (HDStorageAccount) storageAccount;
                    BlobContainer defaultContainer = getSparkClusterDefaultContainer(blobStorageAccount,
                            defaultContainerName);
                    String path = String.format("SparkSubmission/%s/%s", uploadFolderPath, file.getName());
                    String uploadedPath = String.format("wasbs://%s@%s/%s", defaultContainerName,
                            blobStorageAccount.getFullStorageBlobName(), path);

                    HDInsightUtil.showInfoOnSubmissionMessageWindow(
                            String.format("Info : Begin uploading file %s to Azure Blob Storage Account %s ...",
                                    localFile, uploadedPath));

                    StorageClientSDKManager.getManager().uploadBlobFileContent(blobStorageAccount.getConnectionString(),
                            defaultContainer, path, bufferedInputStream, callable, 1024 * 1024, file.length());

                    HDInsightUtil.showInfoOnSubmissionMessageWindow(
                            String.format("Info : Submit file to azure blob '%s' successfully.", uploadedPath));
                    return uploadedPath;
                }
            }
        } else if (storageAccount.getAccountType() == StorageAccountType.ADLS) {
            String uploadPath = String.format("adl://%s.azuredatalakestore.net/%s/%s", storageAccount.getName(),
                    storageAccount.getDefaultContainerOrRootPath(), "SparkSubmission");
            HDInsightUtil.showInfoOnSubmissionMessageWindow(String
                    .format("Info : Begin uploading file %s to Azure Data Lake Store %s ...", localFile, uploadPath));
            String uploadedPath = StreamUtil.uploadArtifactToADLS(file, storageAccount, uploadFolderPath);
            HDInsightUtil.showInfoOnSubmissionMessageWindow(
                    String.format("Info : Submit file to azure blob '%s' successfully.", uploadedPath));
            return uploadedPath;
        } else {
            throw new UnsupportedOperationException("unkown storage account type");
        }
    }

    private String truncateTelemetryMessage(@NotNull String message) {
        int len = message.length();
        return len < 50 ? message : message.substring(0, 50);
    }

    private int printoutJobLog(/* Project project, */ int id, int from_index, IClusterDetail clusterDetail)
            throws IOException {
        HttpResponse httpResponse = SparkBatchSubmission.getInstance()
                .getBatchJobFullLog(clusterDetail.getConnectionUrl() + "/livy/batches", id);
        sparkJobLog = new Gson().fromJson(httpResponse.getMessage(), new TypeToken<SparkJobLog>() {
        }.getType());

        if (!HDInsightUtil.getSparkSubmissionToolWindowView().getJobStatusManager().isApplicationGenerated()) {
            String sparkLogs = Joiner.on("").join(sparkJobLog.getLog());
            String applicationId = getApplicationIdFromYarnLog(sparkLogs);
            if (applicationId != null) {
                HDInsightUtil.getSparkSubmissionToolWindowView().setBrowserButtonState(true);
                HDInsightUtil.getSparkSubmissionToolWindowView().getJobStatusManager().setApplicationIdGenerated();
                HDInsightUtil.getSparkSubmissionToolWindowView().getJobStatusManager().setApplicationId(applicationId);
            }
        }

        int counter = 0;
        if (sparkJobLog.getLog().size() > 0) {
            for (String line : sparkJobLog.getLog()) {
                if (counter >= from_index && !StringHelper.isNullOrWhiteSpace(line)) {
                    HDInsightUtil.getSparkSubmissionToolWindowView().setInfo(line, true);
                }

                counter++;
            }
        }

        return sparkJobLog.getTotal();
    }

    private BlobContainer getSparkClusterDefaultContainer(ClientStorageAccount storageAccount,
            String dealtContainerName) throws AzureCmdException {
        List<BlobContainer> containerList = StorageClientSDKManager.getManager()
                .getBlobContainers(storageAccount.getConnectionString());
        for (BlobContainer container : containerList) {
            if (container.getName().toLowerCase().equals(dealtContainerName.toLowerCase())) {
                return container;
            }
        }

        return null;
    }

    private int getIntervalTime(int times) {
        int interval = MIN_INTERVAL_TIME + times * INC_TIME;
        return interval > MAX_INTERVAL_TIME ? MAX_INTERVAL_TIME : interval;
    }

    private String getApplicationIdFromYarnLog(String yarnLog) {
        Pattern r = Pattern.compile(APPLICATION_ID_PATTERN);
        Matcher m = r.matcher(yarnLog);
        if (m.find()) {
            return m.group(1);
        }

        return null;
    }

    public static String uploadFileToEmulator(@NotNull IClusterDetail selectedClusterDetail,
            @NotNull String buildJarPath) throws Exception {
        HDInsightUtil.showInfoOnSubmissionMessageWindow(String.format("Info : Get target jar from %s.", buildJarPath));
        String uniqueFolderId = UUID.randomUUID().toString();
        String folderPath = String.format("../opt/livy/SparkSubmission/%s", uniqueFolderId);
        return String.format("/opt/livy/SparkSubmission/%s/%s", uniqueFolderId,
                SparkSubmitHelper.getInstance().sftpFileToEmulator(buildJarPath, folderPath, selectedClusterDetail));
    }

    public String sftpFileToEmulator(String localFile, String folderPath, IClusterDetail clusterDetail)
            throws IOException, HDIException, JSchException, SftpException {
        EmulatorClusterDetail emulatorClusterDetail = (EmulatorClusterDetail) clusterDetail;
        final File file = new File(localFile);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            try (BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
                String sshEndpoint = emulatorClusterDetail.getSSHEndpoint();
                URL url = new URL(sshEndpoint);
                String host = url.getHost();
                int port = url.getPort();

                JSch jsch = new JSch();
                Session session = jsch.getSession(emulatorClusterDetail.getHttpUserName(), host, port);
                session.setPassword(emulatorClusterDetail.getHttpPassword());

                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);

                session.connect();
                ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
                channel.connect();

                String[] folders = folderPath.split("/");
                for (String folder : folders) {
                    if (folder.length() > 0) {
                        try {
                            channel.cd(folder);
                        } catch (SftpException e) {
                            channel.mkdir(folder);
                            channel.cd(folder);
                        }
                    }
                }

                channel.put(bufferedInputStream, file.getName());
                channel.disconnect();
                session.disconnect();
                return file.getName();
            }
        }
    }

    public static String uploadFileToHDFS(@NotNull IClusterDetail selectedClusterDetail,
                                          @NotNull String buildJarPath)
            throws Exception {

        return JobUtils.uploadFileToHDFS(selectedClusterDetail, buildJarPath,
                                         HDInsightUtil.getToolWindowMessageSubject());
    }

    public static String getLivyConnectionURL(IClusterDetail clusterDetail) {
        if (clusterDetail.isEmulator()) {
            return clusterDetail.getConnectionUrl() + "/batches";
        }

        return clusterDetail.getConnectionUrl() + "/livy/batches";
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

    private static String getFormatPathByDate() {
        int year = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.YEAR);
        int month = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.MONTH);
        int day = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.DAY_OF_MONTH);

        String uniqueFolderId = UUID.randomUUID().toString();

        return String.format("%04d/%02d/%02d/%s", year, month, day, uniqueFolderId);
    }
}
