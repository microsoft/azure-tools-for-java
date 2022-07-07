/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.synapsesoc.common;

import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.AzureManagementHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.ApiVersion;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.DataLakeAnalyticsAccount;
import com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models.BigDataPoolResourceInfo;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkCompute;
import com.microsoft.azure.projectarcadia.common.ArcadiaWorkSpace;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SynapseCosmosSparkPool extends ArcadiaSparkCompute {
    public static final Pattern ADLA_RESOURCE_ID_PATTERN = Pattern.compile("^/subscriptions/(?<sid>[0-9a-z-]+)/resourceGroups/(.*)$");
    private boolean isConfigInfoAvailable = false;
    private final String adlaResourceId;
    @Nullable
    private IHDIStorageAccount storageAccount;
    @Nullable
    private AzureHttpObservable http;

    public SynapseCosmosSparkPool(ArcadiaWorkSpace workSpace, BigDataPoolResourceInfo sparkComputeResponse, String adlaResourceId) {
        super(workSpace, sparkComputeResponse);
        this.adlaResourceId = adlaResourceId;
    }

    public String getAdlaResourceId() {
        return adlaResourceId;
    }

    public boolean isConfigInfoAvailable() {
        return isConfigInfoAvailable;
    }

    @Override
    public void getConfigurationInfo() throws IOException {
        if (!isConfigInfoAvailable()) {
            // Extract Subscription ID from ADLA resource ID
            // Sample adlaResouceId: /subscriptions/a00b00a0-00a0-0000-b000-a000b0a00000/resourceGroups/testRG/providers/Microsoft.DataLakeAnalytics/accounts/testAccount
            Matcher matcher = ADLA_RESOURCE_ID_PATTERN.matcher(getAdlaResourceId());
            if (!matcher.matches()) {
                String errorMsg = String.format(
                        "ADLA resource ID doesn't match with pattern. AdlaResourceId: %s. Pattern: %s",
                        getAdlaResourceId(),
                        ADLA_RESOURCE_ID_PATTERN);
                throw new IOException(errorMsg);
            }
            String subscriptionId = matcher.group("sid");

            // Get Subscription from subscription ID
            final Account account = Azure.az(AzureAccount.class).account();
            Subscription subscription = account.getSubscription(subscriptionId);
            if (subscription == null) {
                throw new IOException("User has no permission to access subscription " + subscriptionId + ".");
            }

            // Get ADLA account details through Azure REST API
            AzureHttpObservable managementHttp =
                    new AzureManagementHttpObservable(subscription, ApiVersion.VERSION);
            String resourceManagerEndpoint = CommonSettings.getAdEnvironment().resourceManagerEndpoint();
            URI accountDetailUri = URI.create(resourceManagerEndpoint).resolve(getAdlaResourceId());
            AzureSparkServerlessAccount azureSparkServerlessAccount =
                    managementHttp
                            .withUuidUserAgent()
                            .get(accountDetailUri.toString(), null, null, DataLakeAnalyticsAccount.class)
                            .doOnError(err ->
                                    log().warn("Error getting ADLA account details with error: " + err.getMessage()))
                            .map(dataLakeAnalyticsAccount ->
                                    new AzureSparkServerlessAccount(
                                            subscription,
                                            URI.create("https://" + dataLakeAnalyticsAccount.endpoint()),
                                            dataLakeAnalyticsAccount.name())
                                            .setDetailResponse(dataLakeAnalyticsAccount))
                            .subscribeOn(Schedulers.io())
                            .toBlocking()
                            .singleOrDefault(null);

            // Get default storage account info from ADLA account
            String storageRootPath = azureSparkServerlessAccount.getStorageRootPath();
            IHDIStorageAccount adlsGen1StorageAccount =
                    storageRootPath == null ? null : new AzureSparkCosmosCluster.StorageAccount(
                            azureSparkServerlessAccount.getDetailResponse().defaultDataLakeStoreAccount(),
                            storageRootPath,
                            azureSparkServerlessAccount.getSubscription().getId());

            synchronized (this) {
                if (!isConfigInfoAvailable()) {
                    this.http = managementHttp;
                    this.storageAccount = adlsGen1StorageAccount;
                    isConfigInfoAvailable = true;
                }
            }
        }
    }

    @Nullable
    public AzureHttpObservable getHttp() {
        return this.http;
    }

    @Nullable
    @Override
    public IHDIStorageAccount getStorageAccount() {
        try {
            getConfigurationInfo();
        } catch (Exception ex) {
            String errorMessage = String.format(
                    "Error getting storage account info from SoC spark pool. AdlaResourceId: %s. Workspace: %s. Spark pool: %s.",
                    getAdlaResourceId(),
                    getWorkSpace().getName(),
                    getName());
            log().warn(errorMessage);
            log().warn(ExceptionUtils.getStackTrace(ex));
        } finally {
            return this.storageAccount;
        }
    }

    @Nullable
    @Override
    public String getDefaultStorageRootPath() {
        IHDIStorageAccount storageAccount = getStorageAccount();
        if (storageAccount == null) {
            return null;
        }

        return storageAccount.getDefaultContainerOrRootPath();
    }

    public SparkSubmitStorageTypeOptionsForCluster getStorageOptionsType() {
        return SparkSubmitStorageTypeOptionsForCluster.SynapseCosmosSparkCluster;
    }

    @Override
    public SparkSubmitStorageType getDefaultStorageType() {
        return SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT;
    }
}
