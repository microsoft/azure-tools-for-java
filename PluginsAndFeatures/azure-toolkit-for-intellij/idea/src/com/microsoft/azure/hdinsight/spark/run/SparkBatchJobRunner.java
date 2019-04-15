/*
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
 *
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.BrowserHyperlinkInfo;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.InternalUrlMapping;
import com.microsoft.azure.hdinsight.sdk.common.*;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.ApiVersion;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountTypeEnum;
import com.microsoft.azure.hdinsight.spark.common.*;
import com.microsoft.azure.hdinsight.spark.run.action.SparkBatchJobDisconnectAction;
import com.microsoft.azure.hdinsight.spark.run.configuration.ArisSparkConfiguration;
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration;
import com.microsoft.azure.hdinsight.spark.ui.SparkJobLogConsoleView;
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observer;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SparkBatchJobRunner extends DefaultProgramRunner implements SparkSubmissionRunner, ILogger {
    @NotNull
    @Override
    public String getRunnerId() {
        return "SparkBatchJobRun";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return SparkBatchJobRunExecutor.EXECUTOR_ID.equals(executorId)
                && (profile.getClass() == LivySparkBatchJobRunConfiguration.class
                || profile.getClass() == ArisSparkConfiguration.class);
    }

    @Override
    @NotNull
    public ISparkBatchJob buildSparkBatchJob(@NotNull SparkSubmitModel submitModel,
                                             @NotNull Observer<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject) throws ExecutionException {
        // get storage account and access token from submitModel
        IHDIStorageAccount storageAccount = null;
        String accessToken = null;
        String accessKey = null;
        String destinationRootPath = null;
        HttpObservable httpObservable = null;
        Deployable jobDeploy = null;
        String clusterName = submitModel.getSubmissionParameter().getClusterName();
        IClusterDetail clusterDetail = ClusterManagerEx.getInstance().getClusterDetailByName(clusterName)
                .orElseThrow(() -> new ExecutionException("Can't find cluster named " + clusterName));

        SparkSubmitStorageType storageAcccountType = submitModel.getJobUploadStorageModel().getStorageAccountType();
        String subscription = submitModel.getJobUploadStorageModel().getSelectedSubscription();
        switch (storageAcccountType) {
            case BLOB:
                String storageAccountName = submitModel.getJobUploadStorageModel().getStorageAccount();
                if (StringUtils.isBlank(storageAccountName)) {
                    throw new ExecutionException("Can't get the valid storage account.");
                }

                String fullStorageBlobName = ClusterManagerEx.getInstance().getBlobFullName(storageAccountName);
                String key = submitModel.getJobUploadStorageModel().getStorageKey();
                String container = submitModel.getJobUploadStorageModel().getSelectedContainer();
                if(StringUtils.isBlank(key) || StringUtils.isBlank(container)) {
                    throw new ExecutionException("Can't get the valid key or container name.");
                }

                storageAccount = new HDStorageAccount(clusterDetail, fullStorageBlobName, key, false, container);
                break;
            case DEFAULT_STORAGE_ACCOUNT:
                try {
                    clusterDetail.getConfigurationInfo();
                    storageAccount = clusterDetail.getStorageAccount();
                } catch (Exception ex) {
                    log().warn("Error getting cluster storage configuration. Error: " + ExceptionUtils.getStackTrace(ex));
                    storageAccount = null;
                }
                break;
            case SPARK_INTERACTIVE_SESSION:
                break;
            case ADLS_GEN1:
                String rawRootPath = submitModel.getJobUploadStorageModel().getAdlsRootPath();
                if (StringUtils.isBlank(rawRootPath) || !rawRootPath.matches(SparkBatchJob.AdlsPathPattern)) {
                    throw new ExecutionException("Invalid adls root path input.");
                }

                destinationRootPath = rawRootPath.endsWith("/") ? rawRootPath : rawRootPath + "/";
                // e.g. for adl://john.azuredatalakestore.net/root/path, adlsAccountName is john
                String adlsAccountName =  destinationRootPath.split("\\.")[0].split("//")[1];

                Optional<SubscriptionDetail> subscriptionDetail =  Optional.empty();
                try{
                    subscriptionDetail = AuthMethodManager.getInstance().getAzureManager().getSubscriptionManager()
                            .getSelectedSubscriptionDetails()
                            .stream()
                            .filter((detail) -> detail.getSubscriptionName().equals(subscription))
                            .findFirst();

                }catch (Exception ignore){
                }

                if (!subscriptionDetail.isPresent()) {
                    throw new ExecutionException("Error getting subscription info. Please select correct subscription");
                }
                // get Access Token
                try {
                    accessToken = AzureSparkClusterManager.getInstance().getAccessToken(subscriptionDetail.get().getTenantId());
                } catch (IOException ex) {
                    log().warn("Error getting access token based on the given ADLS root path. " + ExceptionUtils.getStackTrace(ex));
                    throw new ExecutionException("Error getting access token based on the given ADLS root path");
                }

                break;
            case ADLS_GEN2:
                destinationRootPath = submitModel.getJobUploadStorageModel().getUploadPath();
                accessKey = submitModel.getJobUploadStorageModel().getAccessKey();
                try {
                    clusterDetail.getConfigurationInfo();
                    storageAccount = clusterDetail.getStorageAccount();
                } catch (Exception ex) {
                    log().warn("Error getting cluster storage configuration. Error: " + ExceptionUtils.getStackTrace(ex));
                    throw new ExecutionException("Cannot get valid storage account");
                }

                if (StringUtils.isBlank(accessKey)) {
                    throw new ExecutionException("Invalid access key input");
                }

                httpObservable = new SharedKeyHttpObservable(storageAccount.getName(), accessKey);
                jobDeploy = new ADLSGen2Deploy(clusterDetail, httpObservable);
                break;
            case WEBHDFS:
                destinationRootPath = submitModel.getJobUploadStorageModel().getUploadPath();
                if(StringUtils.isBlank(destinationRootPath) || !destinationRootPath.matches(SparkBatchJob.WebHDFSPathPattern)){
                    throw new ExecutionException("Invalid webhdfs root path input");
                }

                //create httpobservable and jobDeploy
                try {
                    if (clusterDetail instanceof ClusterDetail) {
                        httpObservable = new AzureHttpObservable(clusterDetail.getSubscription().getTenantId(), ApiVersion.VERSION);
                        jobDeploy = clusterDetail.getStorageAccount().getAccountType() == StorageAccountTypeEnum.ADLS
                                ? new ADLSGen1HDFSDeploy(clusterDetail, httpObservable)
                                : null;
                    } else if (clusterDetail instanceof SqlBigDataLivyLinkClusterDetail) {
                        httpObservable = new HttpObservable(clusterDetail.getHttpUserName(), clusterDetail.getHttpPassword());
                        jobDeploy = new WebHDFSDeploy(clusterDetail, httpObservable);
                    }
                } catch (HDIException ignore) {
                }

                if (httpObservable == null || jobDeploy == null) {
                    throw new ExecutionException("Error preparing webhdfs uploading info based on the given cluster");
                }

                break;
        }

        //TODO:use httpobservable to replace sparkbathsubmission and deprecate the old constructor.
        if (httpObservable != null) {
            return new SparkBatchJob(clusterDetail, submitModel.getSubmissionParameter(), SparkBatchSubmission.getInstance(), ctrlSubject, storageAccount
                    , storageAcccountType == SparkSubmitStorageType.ADLS_GEN2 ? accessKey : accessToken,
                    destinationRootPath, httpObservable, jobDeploy);
        } else {
            return new SparkBatchJob(clusterDetail, submitModel.getSubmissionParameter(), SparkBatchSubmission.getInstance(), ctrlSubject, storageAccount, accessToken, destinationRootPath);
        }
    }

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state,@NotNull ExecutionEnvironment environment) throws ExecutionException {
        SparkBatchRemoteRunProfileState submissionState = (SparkBatchRemoteRunProfileState) state;

        // Check parameters before starting
        submissionState.checkSubmissionParameter();

        SparkSubmitModel submitModel = submissionState.getSubmitModel();
        Project project = submitModel.getProject();

        // Prepare the run table console view UI
        SparkJobLogConsoleView jobOutputView = new SparkJobLogConsoleView(project);
        PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject = PublishSubject.create();
        SparkBatchJobRemoteProcess remoteProcess = new SparkBatchJobRemoteProcess(
                new IdeaSchedulers(project),
                buildSparkBatchJob(submitModel, ctrlSubject),
                submitModel.getArtifactPath().orElseThrow(() -> new ExecutionException("No artifact selected")),
                submitModel.getSubmissionParameter().getMainClassName(),
                ctrlSubject);
        SparkBatchJobRunProcessHandler processHandler = new SparkBatchJobRunProcessHandler(remoteProcess, "Package and deploy the job to Spark cluster", null);

        // After attaching, the console view can read the process inputStreams and display them
        jobOutputView.attachToProcess(processHandler);

        remoteProcess.start();
        SparkBatchJobDisconnectAction disconnectAction = new SparkBatchJobDisconnectAction(remoteProcess);

        ExecutionResult result = new DefaultExecutionResult(jobOutputView, processHandler, Separator.getInstance(), disconnectAction);
        submissionState.setExecutionResult(result);
        submissionState.setConsoleView(jobOutputView.getSecondaryConsoleView());

        SparkBatchJob sparkBatchJob = remoteProcess.getSparkJob() instanceof SparkBatchJob
                ? (SparkBatchJob) remoteProcess.getSparkJob()
                : null;
        if (sparkBatchJob != null) {
            InternalUrlMapping mapping = sparkBatchJob.getCluster() instanceof InternalUrlMapping
                    ? (InternalUrlMapping) sparkBatchJob.getCluster()
                    : null;
            if (mapping != null) {
                submissionState.getConsoleView().addMessageFilter((line, entireLength) -> {
                    Matcher matcher = Pattern.compile("http://[^\\s]+", Pattern.CASE_INSENSITIVE).matcher(line);
                    List<Filter.ResultItem> items = new ArrayList<>();
                    int textStartOffset = entireLength - line.length();
                    while (matcher.find()) {
                        String mappedUrl = mapping.mapInternalUrlToPublic(matcher.group(0));
                        items.add(new Filter.ResultItem(textStartOffset + matcher.start(), textStartOffset + matcher.end(), new BrowserHyperlinkInfo(mappedUrl)));
                    }
                    return items.size() != 0 ? new Filter.Result(items) : null;
                });
            }
        }
        submissionState.setRemoteProcessCtrlLogHandler(processHandler);

        ctrlSubject.subscribe(
                messageWithType -> {},
                err -> disconnectAction.setEnabled(false),
                () -> disconnectAction.setEnabled(false));

        return super.doExecute(state, environment);
    }

    @Override
    public void setFocus(@NotNull RunConfiguration runConfiguration) {
        if (runConfiguration instanceof LivySparkBatchJobRunConfiguration) {
            LivySparkBatchJobRunConfiguration livyRunConfig = (LivySparkBatchJobRunConfiguration) runConfiguration;
            livyRunConfig.getModel().setFocusedTabIndex(1);
        }
    }
}
