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
package com.microsoft.azure.hdinsight.spark.run;

import com.intellij.execution.ExecutionException;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.*;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.ApiVersion;
import com.microsoft.azure.hdinsight.sdk.storage.ADLSGen2StorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountTypeEnum;
import com.microsoft.azure.hdinsight.spark.common.*;
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.scalameta.logger;
import rx.Observer;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SparkBatchJobDeployFactory implements ILogger {
    private static SparkBatchJobDeployFactory ourInstance = new SparkBatchJobDeployFactory();

    public static SparkBatchJobDeployFactory getInstance() {
        return ourInstance;
    }

    private SparkBatchJobDeployFactory() {

    }

    public Deployable buildSparkBatchJobDeploy(@NotNull SparkSubmitModel submitModel,
                                               @NotNull Observer<AbstractMap.SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject) throws ExecutionException {

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

        // For HDI Reader cluster, Ambari credential is necessary for job submission.
        if (ClusterManagerEx.getInstance().isHdiReaderCluster(clusterDetail)) {
            try {
                if (clusterDetail.getHttpUserName() == null || clusterDetail.getHttpPassword() == null) {
                    throw new ExecutionException("You have Ready-only permission for this cluster. Please link the cluster first.");
                }
            } catch (HDIException ex) {
                log().warn("Error getting cluster credential. Cluster Name: " + clusterName);
                log().warn(ExceptionUtils.getStackTrace(ex));
                throw new ExecutionException("Error getting Ambari credential for this cluster.");
            }
        }

        switch (storageAcccountType) {
            case BLOB:
                String storageAccountName = submitModel.getJobUploadStorageModel().getStorageAccount();
                if (StringUtils.isBlank(storageAccountName)) {
                    throw new ExecutionException("Can't get the valid storage account.");
                }

                String fullStorageBlobName = ClusterManagerEx.getInstance().getBlobFullName(storageAccountName);
                String key = submitModel.getJobUploadStorageModel().getStorageKey();
                String container = submitModel.getJobUploadStorageModel().getSelectedContainer();
                if (StringUtils.isBlank(key) || StringUtils.isBlank(container)) {
                    throw new ExecutionException("Can't get the valid key or container name.");
                }

                storageAccount = new HDStorageAccount(clusterDetail, fullStorageBlobName, key, false, container);
                jobDeploy = new LegacySDKDeploy(storageAccount, ctrlSubject);
                break;
            case DEFAULT_STORAGE_ACCOUNT:
                try {
                    clusterDetail.getConfigurationInfo();
                    storageAccount = clusterDetail.getStorageAccount();

                    if (storageAccount.getAccountType() == StorageAccountTypeEnum.ADLSGen2) {
                        destinationRootPath = submitModel.getJobUploadStorageModel().getUploadPath();
                        accessKey = ((ADLSGen2StorageAccount) storageAccount).getPrimaryKey();
                        if (StringUtils.isBlank(accessKey)) {
                            throw new ExecutionException("Cannot get valid access key for storage account");
                        }

                        httpObservable = new SharedKeyHttpObservable(storageAccount.getName(), accessKey);
                        jobDeploy = new ADLSGen2Deploy(httpObservable, destinationRootPath);
                    } else if (storageAccount.getAccountType() == StorageAccountTypeEnum.BLOB ||
                            storageAccount.getAccountType() == StorageAccountTypeEnum.ADLS) {
                        jobDeploy = new LegacySDKDeploy(storageAccount, ctrlSubject);
                    }
                } catch (Exception ex) {
                    log().warn("Error getting cluster storage configuration. Error: " + ExceptionUtils.getStackTrace(ex));
                    storageAccount = null;
                }
                break;
            case SPARK_INTERACTIVE_SESSION:
                jobDeploy = new LivySessionDeploy(clusterName, ctrlSubject);
                break;
            case ADLS_GEN1:
                String rawRootPath = submitModel.getJobUploadStorageModel().getAdlsRootPath();
                if (StringUtils.isBlank(rawRootPath) || !rawRootPath.matches(SparkBatchJob.AdlsPathPattern)) {
                    throw new ExecutionException("Invalid adls root path input.");
                }

                destinationRootPath = rawRootPath.endsWith("/") ? rawRootPath : rawRootPath + "/";
                // e.g. for adl://john.azuredatalakestore.net/root/path, adlsAccountName is john
                String adlsAccountName = destinationRootPath.split("\\.")[0].split("//")[1];

                Optional<SubscriptionDetail> subscriptionDetail = Optional.empty();
                try {
                    subscriptionDetail = AuthMethodManager.getInstance().getAzureManager().getSubscriptionManager()
                            .getSelectedSubscriptionDetails()
                            .stream()
                            .filter((detail) -> detail.getSubscriptionName().equals(subscription))
                            .findFirst();

                } catch (Exception ignore) {
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

                jobDeploy = new AdlsDeploy(destinationRootPath, accessToken);
                break;
            case ADLS_GEN2:
                destinationRootPath = submitModel.getJobUploadStorageModel().getUploadPath();
                String gen2StorageAccount = "";
                Matcher m = Pattern.compile(SparkBatchJob.AdlsGen2RestfulPathPattern).matcher(destinationRootPath);
                if (m.find()) {
                    gen2StorageAccount = m.group("accountName");
                }

                if (StringUtils.isBlank(gen2StorageAccount)) {
                    throw new ExecutionException("Invalid ADLS GEN2 root path.");
                }

                accessKey = submitModel.getJobUploadStorageModel().getAccessKey();
                if (StringUtils.isBlank(accessKey)) {
                    throw new ExecutionException("Invalid access key input");
                }

                httpObservable = new SharedKeyHttpObservable(gen2StorageAccount, accessKey);
                jobDeploy = new ADLSGen2Deploy(httpObservable, destinationRootPath);
                break;
            case WEBHDFS:
                destinationRootPath = submitModel.getJobUploadStorageModel().getUploadPath();
                if (StringUtils.isBlank(destinationRootPath) || !destinationRootPath.matches(SparkBatchJob.WebHDFSPathPattern)) {
                    throw new ExecutionException("Invalid webhdfs root path input");
                }

                //create httpobservable and jobDeploy
                try {
                    if (clusterDetail instanceof ClusterDetail) {
                        httpObservable = new AzureHttpObservable(clusterDetail.getSubscription().getTenantId(), ApiVersion.VERSION);
                        jobDeploy = clusterDetail.getStorageAccount().getAccountType() == StorageAccountTypeEnum.ADLS
                                ? new ADLSGen1HDFSDeploy(clusterDetail, httpObservable, destinationRootPath)
                                : null;
                    } else if (clusterDetail instanceof SqlBigDataLivyLinkClusterDetail) {
                        httpObservable = new HttpObservable(clusterDetail.getHttpUserName(), clusterDetail.getHttpPassword());
                        jobDeploy = new WebHDFSDeploy(clusterDetail, httpObservable, destinationRootPath);
                    }
                } catch (HDIException ignore) {
                }

                if (httpObservable == null || jobDeploy == null) {
                    throw new ExecutionException("Error preparing webhdfs uploading info based on the given cluster");
                }

                break;
        }

        //TODO:use httpobservable to replace sparkbathsubmission and deprecate the old constructor.
        return jobDeploy;
    }
}
