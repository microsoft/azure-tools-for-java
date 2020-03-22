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
package com.microsoft.azure.hdinsight.sdk.common;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosClusterManager;
import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.NameValuePair;
import rx.Observable;

import java.io.IOException;
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

    @Nullable
    @Override
    public List<NameValuePair> getAccountFilter() {
        return null;
    }

    public Observable<SubscriptionDetail> getSubscriptionDetailByStoreAccountName(String storeAccountName) {
        return get()
                .map(clusterManager -> clusterManager.getAccounts())
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
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) {
            throw new AuthException("Not signed in. Can't send out the request.");
        }

        return azureManager.getAccessToken(tenantId, getResourceEndpoint(), PromptBehavior.Auto);
    }

    public boolean isSignedIn() {
        try {
            return AuthMethodManager.getInstance().isSignedIn();
        } catch (Exception ex) {
            log().warn("Exception happens when we try to know if user signed in. " + ExceptionUtils.getStackTrace(ex));
            return false;
        }
    }

    @Nullable
    public String getAzureAccountEmail() {
        if (AuthMethodManager.getInstance() == null) {
            return null;
        } else if (AuthMethodManager.getInstance().getAuthMethodDetails().getAccountEmail() == null) {
            return AuthMethodManager.getInstance().getAuthMethodDetails().getCredFilePath();
        } else {
            return AuthMethodManager.getInstance().getAuthMethodDetails().getAccountEmail();
        }
    }
}
