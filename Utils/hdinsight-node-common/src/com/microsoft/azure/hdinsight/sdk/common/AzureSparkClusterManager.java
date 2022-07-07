/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosClusterManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.NameValuePair;
import rx.Observable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class AzureSparkClusterManager extends AzureSparkCosmosClusterManager implements ILogger {
    // Lazy singleton initialization
    private static class LazyHolder {
        static final AzureSparkClusterManager INSTANCE =
                new AzureSparkClusterManager();
    }

    public static AzureSparkClusterManager getInstance() {
        return AzureSparkClusterManager.LazyHolder.INSTANCE;
    }

    @Override
    public List<NameValuePair> getAccountFilter() {
        return Collections.emptyList();
    }

    public Observable<Subscription> getSubscriptionDetailByStoreAccountName(String storeAccountName) {
        return get()
                .map(AzureSparkCosmosClusterManager::getAccounts)
                .flatMap(Observable::from)
                .filter(account -> account.getDetailResponse() != null &&
                        account.getDetailResponse().dataLakeStoreAccounts() != null &&
                        !account.getDetailResponse().dataLakeStoreAccounts().isEmpty() &&
                        account.getDetailResponse().dataLakeStoreAccounts().stream()
                                .anyMatch(storeAccount -> storeAccount.name().equals(storeAccountName)))
                .map(account -> account.getSubscription())
                .firstOrDefault(null);
    }

    @NotNull
    public String getResourceEndpoint() {
        String endpoint = CommonSettings.getAdEnvironment().resourceManagerEndpoint();

        return endpoint != null ? endpoint : "https://management.azure.com/";
    }

    @NotNull
    public String getAccessToken(String tenantId) throws IOException {
        return IdeAzureAccount.getInstance().getCredentialForTrack1(tenantId).getToken(getResourceEndpoint());
    }

    public boolean isLoggedIn() {
        try {
            return IdeAzureAccount.getInstance().isLoggedIn();
        } catch (Exception ex) {
            log().warn("Exception happens when we try to know if user signed in. " + ExceptionUtils.getStackTrace(ex));
            return false;
        }
    }

    @Nullable
    public String getAzureAccountEmail() {
        final AzureAccount az = Azure.az(AzureAccount.class);
        if (!az.isLoggedIn()) {
            return null;
        } else if (StringUtils.isBlank(az.account().getUsername())) {
            return "Unknown";
        } else {
            return az.account().getUsername();
        }
    }
}
