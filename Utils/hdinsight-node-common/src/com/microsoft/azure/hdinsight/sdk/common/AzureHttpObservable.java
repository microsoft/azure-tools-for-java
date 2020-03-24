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

import org.apache.http.NameValuePair;
import org.apache.http.impl.client.HttpClients;

import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.AppInsightsClient;

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


    public AzureHttpObservable(@NotNull SubscriptionDetail subscription, @NotNull String apiVersion) {
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
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) {
            throw new AuthException("Not signed in. Can't send out the request.");
        }

        return azureManager.getAccessToken(getTenantId(), getResourceEndpoint(), PromptBehavior.Auto);
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
