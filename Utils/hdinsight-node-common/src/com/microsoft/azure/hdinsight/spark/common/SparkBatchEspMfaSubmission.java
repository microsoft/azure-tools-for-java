/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.util.Arrays;

public class SparkBatchEspMfaSubmission extends SparkBatchSubmission {
    public static final String resource = "https://hib.azurehdinsight.net";
    private String tenantId;

    public SparkBatchEspMfaSubmission(final @NotNull String tenantId, final @NotNull String name) {
        this.tenantId = tenantId;
    }

    @NotNull
    public String getAccessToken() throws IOException {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) {
            throw new AuthException("Not signed in. Can't send out the request.");
        }

        return azureManager.getAccessToken(getTenantId(), resource, PromptBehavior.Auto);
    }

    @NotNull
    @Override
    public CloseableHttpClient getHttpClient() throws IOException {
        return HttpClients.custom()
                .useSystemProperties()
                .setDefaultHeaders(Arrays.asList(
                        new BasicHeader("Authorization", "Bearer " + getAccessToken())))
                .setSSLSocketFactory(getSSLSocketFactory())
                .build();
    }


    public String getTenantId() {
        return tenantId;
    }

    @Override
    @Nullable
    public String getAuthCode() {
        try {
            return "Bearer " + getAccessToken();
        } catch (IOException e) {
            return null;
        }
    }
}
