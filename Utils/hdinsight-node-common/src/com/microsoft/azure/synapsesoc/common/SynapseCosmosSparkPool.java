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

package com.microsoft.azure.synapsesoc.common;

import com.microsoft.azure.hdinsight.sdk.common.AzureManagementHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosClusterManager;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.ApiVersion;
import com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models.BigDataPoolResourceInfo;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageTypeOptionsForCluster;
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkCompute;
import com.microsoft.azure.projectarcadia.common.ArcadiaWorkSpace;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
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
    private IHDIStorageAccount storageAccount;

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
            synchronized (this) {
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

                    // Get SubscriptionDetail from subscription ID
                    AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                    SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
                    SubscriptionDetail subscription = subscriptionManager.getSubscriptionIdToSubscriptionDetailsMap().getOrDefault(subscriptionId, null);
                    if (subscription == null) {
                        throw new IOException("User has no permission to access subscription " + subscriptionId + ".");
                    }

                    // Get ADLA account details through Azure REST API
                    AzureManagementHttpObservable http = new AzureManagementHttpObservable(subscription, ApiVersion.VERSION);
                    AzureSparkServerlessAccount azureSparkServerlessAccount =
                            AzureSparkCosmosClusterManager.getInstance()
                                    .getAzureDataLakeAccountDetail(http, getAdlaResourceId())
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
                    this.storageAccount = storageRootPath == null ? null : new AzureSparkCosmosCluster.StorageAccount(
                            azureSparkServerlessAccount.getDetailResponse().defaultDataLakeStoreAccount(),
                            storageRootPath,
                            azureSparkServerlessAccount.getSubscription().getSubscriptionId());
                }
            }
        }
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
        if (getStorageAccount() == null) {
            return null;
        }

        return getStorageAccount().getDefaultContainerOrRootPath();
    }

    public SparkSubmitStorageTypeOptionsForCluster getStorageOptionsType() {
        return SparkSubmitStorageTypeOptionsForCluster.SynapseCosmosSparkCluster;
    }

    @Override
    public SparkSubmitStorageType getDefaultStorageType() {
        return SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT;
    }
}