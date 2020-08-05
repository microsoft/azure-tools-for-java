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
import com.microsoft.azure.hdinsight.common.AbfsUri;
import com.microsoft.azure.hdinsight.common.AdlUri;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.UriUtil;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.AzureAdAccountDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.*;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.ApiVersion;
import com.microsoft.azure.hdinsight.sdk.storage.ADLSGen2StorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountType;
import com.microsoft.azure.hdinsight.spark.common.*;
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionContentPanel;
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azure.synapsesoc.common.SynapseCosmosSparkPool;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

public class SparkBatchJobDeployFactory implements ILogger {
    private static final SparkBatchJobDeployFactory ourInstance = new SparkBatchJobDeployFactory();

    public static SparkBatchJobDeployFactory getInstance() {
        return ourInstance;
    }

    private SparkBatchJobDeployFactory() {

    }

    public Deployable buildSparkBatchJobDeploy(@NotNull final SparkSubmitModel submitModel,
                                               @NotNull final IClusterDetail clusterDetail) throws ExecutionException {

        // get storage account and access token from submitModel
        final IHDIStorageAccount storageAccount;
        final String accessToken;
        final String accessKey;
        final String destinationRootPath;
        HttpObservable httpObservable = null;
        Deployable jobDeploy = null;
        final String clusterName = submitModel.getSubmissionParameter().getClusterName();

        final SparkSubmitStorageType storageAcccountType =
                submitModel.getJobUploadStorageModel().getStorageAccountType();
        if (storageAcccountType == null) {
            throw new ExecutionException("Job upload storage account type is empty.");
        }

        final String subscription = submitModel.getJobUploadStorageModel().getSelectedSubscription();

        // For HDI Reader cluster, Ambari credential is necessary for job submission.
        if (ClusterManagerEx.getInstance().isHdiReaderCluster(clusterDetail)) {
            try {
                if (clusterDetail.getHttpUserName() == null || clusterDetail.getHttpPassword() == null) {
                    throw new ExecutionException("No Ambari permission to submit job to the selected cluster");
                }
            } catch (final HDIException ex) {
                log().warn("Error getting cluster credential. Cluster Name: " + clusterName);
                log().warn(ExceptionUtils.getStackTrace(ex));
                throw new ExecutionException("Error getting Ambari credential for this cluster");
            }
        }

        switch (storageAcccountType) {
            case BLOB:
                final String storageAccountName = submitModel.getJobUploadStorageModel().getStorageAccount();
                if (StringUtils.isBlank(storageAccountName)) {
                    throw new ExecutionException("Can't get the valid storage account");
                }

                final String fullStorageBlobName = ClusterManagerEx.getInstance().getBlobFullName(storageAccountName);
                final String key = submitModel.getJobUploadStorageModel().getStorageKey();
                final String container = submitModel.getJobUploadStorageModel().getSelectedContainer();
                if (StringUtils.isBlank(key) || StringUtils.isBlank(container)) {
                    throw new ExecutionException("Can't get the valid key or container name");
                }

                storageAccount = new HDStorageAccount(clusterDetail, fullStorageBlobName, key, false, container);
                jobDeploy = new LegacySDKDeploy(storageAccount);
                break;
            case DEFAULT_STORAGE_ACCOUNT:
                try {
                    clusterDetail.getConfigurationInfo();
                    storageAccount = clusterDetail.getStorageAccount();
                    if (storageAccount == null) {
                        final String errorMsg = "Cannot get storage account from cluster";
                        log().warn(String.format("%s. Cluster: %s.", errorMsg, clusterDetail.getName()));
                        throw new ExecutionException(errorMsg);
                    } else if (storageAccount.getAccountType() == StorageAccountType.ADLSGen2) {
                        final String defaultStorageRootPath = clusterDetail.getDefaultStorageRootPath();
                        if (defaultStorageRootPath == null) {
                            throw new ExecutionException(
                                    String.format("cluster %s's default storage root path is empty",
                                                  clusterDetail.getName()));
                        }
                        final URI rawDestinationRootURI =
                                UriUtil.normalizeWithSlashEnding(URI.create(defaultStorageRootPath))
                                       .resolve(SparkSubmissionContentPanel.Constants.submissionFolder + "/");
                        destinationRootPath = AbfsUri.parse(rawDestinationRootURI.toString()).getUrl().toString();

                        if (clusterDetail instanceof AzureAdAccountDetail) {
                            httpObservable =
                                    new ADLSGen2OAuthHttpObservable(
                                            ((AzureAdAccountDetail) clusterDetail).getTenantId());
                        } else {
                            accessKey = ((ADLSGen2StorageAccount) storageAccount).getPrimaryKey();
                            httpObservable = new SharedKeyHttpObservable(storageAccount.getName(), accessKey);
                            if (StringUtils.isBlank(accessKey)) {
                                throw new ExecutionException("Cannot get valid access key for storage account");
                            }
                        }

                        jobDeploy = new ADLSGen2Deploy(httpObservable, destinationRootPath);
                    } else if (storageAccount.getAccountType() == StorageAccountType.BLOB ||
                            storageAccount.getAccountType() == StorageAccountType.ADLS) {
                        if (clusterDetail instanceof SynapseCosmosSparkPool
                                || clusterDetail instanceof AzureSparkCosmosCluster) {
                            final AzureHttpObservable http =
                                    clusterDetail instanceof SynapseCosmosSparkPool
                                    ? ((SynapseCosmosSparkPool) clusterDetail).getHttp()
                                    : ((AzureSparkCosmosCluster) clusterDetail).getHttp();
                            if (http == null) {
                                final String errorMsg = "Error preparing access token for ADLS Gen1 storage account";
                                log().warn(String.format("%s. Cluster: %s. Storage account: %s.",
                                                         errorMsg, clusterDetail.getName(), storageAccount.getName()));
                                throw new ExecutionException(
                                        "Error preparing access token for ADLS Gen1 storage account");
                            }
                            final String defaultStorageRootPath = clusterDetail.getDefaultStorageRootPath();
                            if (StringUtils.isBlank(defaultStorageRootPath)) {
                                final String errorMsg =
                                        "Error getting default storage root path for ADLS Gen1 storage account";
                                log().warn(String.format("%s. Cluster: %s. Storage account: %s.",
                                                         errorMsg, clusterDetail.getName(), storageAccount.getName()));
                                throw new ExecutionException(errorMsg);
                            }
                            jobDeploy = new AdlsDeploy(defaultStorageRootPath, http.getAccessToken());
                        } else {
                            jobDeploy = new LegacySDKDeploy(storageAccount);
                        }
                    }
                } catch (final Exception ex) {
                    log().warn(
                            "Error getting cluster storage configuration. Error: " + ExceptionUtils.getStackTrace(ex));
                }
                break;
            case SPARK_INTERACTIVE_SESSION:
                jobDeploy = new LivySessionDeploy(clusterName);
                break;
            case ADLS_GEN1:
                final String rawRootPath = submitModel.getJobUploadStorageModel().getAdlsRootPath();
                if (StringUtils.isBlank(rawRootPath) || !AdlUri.isType(rawRootPath)) {
                    throw new ExecutionException("Invalid adls root path input");
                }

                destinationRootPath = rawRootPath.endsWith("/") ? rawRootPath : rawRootPath + "/";
                // e.g. for adl://john.azuredatalakestore.net/root/path, adlsAccountName is john
                final String adlsAccountName = destinationRootPath.split("\\.")[0].split("//")[1];

                Optional<SubscriptionDetail> subscriptionDetail = Optional.empty();
                try {
                    subscriptionDetail = AuthMethodManager.getInstance().getAzureManager().getSubscriptionManager()
                                                          .getSelectedSubscriptionDetails()
                                                          .stream()
                                                          .filter((detail) -> detail.getSubscriptionName()
                                                                                    .equals(subscription))
                                                          .findFirst();

                } catch (final Exception ignore) {
                }

                if (!subscriptionDetail.isPresent()) {
                    throw new ExecutionException("Error getting subscription info. Please select correct subscription");
                }
                // get Access Token
                try {
                    accessToken = AzureSparkClusterManager.getInstance().getAccessToken(
                            subscriptionDetail.get().getTenantId());
                } catch (final IOException ex) {
                    log().warn("Error getting access token based on the given ADLS root path. "
                                       + ExceptionUtils.getStackTrace(ex));
                    throw new ExecutionException("Error getting access token based on the given ADLS root path");
                }

                jobDeploy = new AdlsDeploy(destinationRootPath, accessToken);
                break;
            case ADLS_GEN2_FOR_OAUTH:
            case ADLS_GEN2:
                destinationRootPath = submitModel.getJobUploadStorageModel().getUploadPath();
                if (!AbfsUri.isType(destinationRootPath)) {
                    throw new ExecutionException("Invalid ADLS GEN2 root path: " + destinationRootPath);
                }

                final AbfsUri destinationUri = AbfsUri.parse(destinationRootPath);
                if (clusterDetail instanceof AzureAdAccountDetail) {
                    httpObservable =
                            new ADLSGen2OAuthHttpObservable(((AzureAdAccountDetail) clusterDetail).getTenantId());
                } else {
                    accessKey = submitModel.getJobUploadStorageModel().getAccessKey();
                    if (StringUtils.isBlank(accessKey)) {
                        throw new ExecutionException("Invalid access key input");
                    }

                    final String accountName = destinationUri.getAccountName();
                    httpObservable = new SharedKeyHttpObservable(accountName, accessKey);
                }

                jobDeploy = new ADLSGen2Deploy(httpObservable, destinationUri.getUrl().toString());
                break;
            case WEBHDFS:
                destinationRootPath = submitModel.getJobUploadStorageModel().getUploadPath();
                if (StringUtils.isBlank(destinationRootPath)
                        || !destinationRootPath.matches(SparkBatchJob.WebHDFSPathPattern)) {
                    throw new ExecutionException("Invalid webhdfs root path input");
                }

                //create httpobservable and jobDeploy
                try {
                    if (clusterDetail instanceof ClusterDetail) {
                        httpObservable = new AzureHttpObservable(clusterDetail.getSubscription().getTenantId(),
                                                                 ApiVersion.VERSION);
                        jobDeploy =
                                clusterDetail.getStorageAccount() != null &&
                                        clusterDetail.getStorageAccount().getAccountType() == StorageAccountType.ADLS
                                ? new ADLSGen1HDFSDeploy(clusterDetail, httpObservable, destinationRootPath)
                                : null;
                    } else if (clusterDetail instanceof SqlBigDataLivyLinkClusterDetail) {
                        httpObservable = new HttpObservable(clusterDetail.getHttpUserName(),
                                                            clusterDetail.getHttpPassword());
                        jobDeploy = new WebHDFSDeploy(clusterDetail, httpObservable, destinationRootPath);
                    }
                } catch (final HDIException ignore) {
                }

                if (httpObservable == null || jobDeploy == null) {
                    throw new ExecutionException("Error preparing webhdfs uploading info based on the given cluster");
                }

                break;
            default:
                throw new ExecutionException("Unsupported job upload storage type");
        }

        //TODO:use httpobservable to replace sparkbathsubmission and deprecate the old constructor.
        return jobDeploy;
    }
}
