/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class AzureHttpObservable extends OAuthTokenHttpObservable {
    @NotNull
    private String tenantId;
    @NotNull
    private final String apiVersion;
    @NotNull
    private final List<NameValuePair> azureDefaultParameters;


    public AzureHttpObservable(@NotNull Subscription subscription, @NotNull String apiVersion) {
        this(subscription.getTenantId(), apiVersion);
    }

    public AzureHttpObservable(@NotNull String apiVersion) {
        this("common", apiVersion);
    }

    public AzureHttpObservable(@NotNull String tenantId, @NotNull String apiVersion) {
        super();

        this.tenantId = tenantId;
        this.apiVersion = apiVersion;

        setHttpClient(HttpClients.custom()
                .useSystemProperties()
                .setDefaultCookieStore(getCookieStore())
                .setDefaultRequestConfig(getDefaultRequestConfig())
                .build());

        azureDefaultParameters = super.getDefaultParameters();

        azureDefaultParameters.removeIf(nameValuePair -> nameValuePair.getName().toLowerCase().equals(ApiVersionParam.NAME));
        azureDefaultParameters.add(new ApiVersionParam(getApiVersion()));
    }

    @NotNull
    public AzureHttpObservable setTenantId(@NotNull String tenantId) {
        this.tenantId = tenantId;

        return this;
    }

    @NotNull
    public String getTenantId() {
        return tenantId;
    }

    @NotNull
    @Override
    public String getAccessToken() throws IOException {
        return IdeAzureAccount.getInstance().getCredentialForTrack1(getTenantId()).getToken(getResourceEndpoint());
    }

    @NotNull
    public String getApiVersion() {
        return apiVersion;
    }

    @NotNull
    @Override
    public List<NameValuePair> getDefaultParameters() {
        return azureDefaultParameters;
    }

    @NotNull
    public AzureHttpObservable withUuidUserAgent() {
        String originUa = getUserAgentPrefix();
        String requestId = AppInsightsClient.getConfigurationSessionId() == null ?
                UUID.randomUUID().toString() :
                AppInsightsClient.getConfigurationSessionId();

        setUserAgent(String.format("%s %s", originUa.trim(), requestId));

        return this;
    }

    @NotNull
    private String getInstallationID() {
        if (HDInsightLoader.getHDInsightHelper() == null) {
            return "";
        }

        return HDInsightLoader.getHDInsightHelper().getInstallationId();
    }

    @NotNull
    public String getResourceEndpoint() {
        String endpoint = CommonSettings.getAdEnvironment().resourceManagerEndpoint();

        return endpoint != null ? endpoint : "https://management.azure.com/";
    }
}
