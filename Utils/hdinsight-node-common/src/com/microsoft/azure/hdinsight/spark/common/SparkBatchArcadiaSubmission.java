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
 */

package com.microsoft.azure.hdinsight.spark.common;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import com.microsoft.azure.hdinsight.common.UriUtil;
import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.AdAuthManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SparkBatchArcadiaSubmission extends SparkBatchSubmission {
    public static final String ARCADIA_RESOURCE_ID = "5d13f7d7-0567-429c-9880-320e9555e5fc";
    public static final Pattern LIVY_URL_NO_WORKSPACE_IN_HOSTNAME_PATTERN = Pattern.compile(
            "https?://arcadia-spark-service-prod\\.(?<region>[^/.]+)(?<suffix>[^/:]+)(:(?<port>[0-9]+))?"
                    + "/versions/(?<apiVersion>[^/]+)/sparkcomputes/(?<compute>[^/]+)/?",
            Pattern.CASE_INSENSITIVE);

    private final @NotNull String workspaceName;
    private final @NotNull String tenantId;
    private final @NotNull URI livyUri;

    public SparkBatchArcadiaSubmission(final @NotNull String tenantId,
                                       final @NotNull String workspaceName,
                                       final @NotNull URI livyUri) {
        this.workspaceName = workspaceName;
        this.tenantId = tenantId;
        this.livyUri = UriUtil.normalizeWithSlashEnding(livyUri);
    }

    @Override
    public void setUsernamePasswordCredential(String username, String password) {
        throw new UnsupportedOperationException("Azure Arcadia does not support UserName/Password credential");
    }

    @NotNull
    private String getResourceEndpoint() {
        return ARCADIA_RESOURCE_ID;
    }

    @NotNull
    protected String getAccessToken() throws IOException {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) {
            throw new AuthException("Not signed in. Can't send out the request.");
        }

        return AdAuthManager.getInstance().getAccessToken(getTenantId(), getResourceEndpoint(), PromptBehavior.Auto);
    }

    @NotNull
    public String getTenantId() {
        return tenantId;
    }

    @NotNull
    public String getWorkspaceName() {
        return workspaceName;
    }

    @NotNull
    @Override
    public CloseableHttpClient getHttpClient() throws IOException {
        return HttpClients.custom()
                .useSystemProperties()
                .setDefaultHeaders(Arrays.asList(
                        new BasicHeader("Authorization", "Bearer " + getAccessToken()),
                        new BasicHeader("x-ms-workspace-name", getWorkspaceName())))
                .setSSLSocketFactory(getSSLSocketFactory())
                .build();
    }

    @NotNull
    public URI getLivyUri() {
        return livyUri;
    }

    @NotNull
    public URL getHistoryServerUrl(int livyId) {
        Matcher matcher = LIVY_URL_NO_WORKSPACE_IN_HOSTNAME_PATTERN.matcher(getLivyUri().toString());

        if (matcher.matches()) {
            try {
                return new URL(String.format("https://arcadia-spark-history-server-prod-%s.azurewebsites.net/" +
                        "workspaces/%s/sparkcomputes/%s/livyid/%d/summary",
                        matcher.group("region"),
                        workspaceName,
                        matcher.group("compute"),
                        livyId));
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Bad Arcadia history server URL", e);
            }
        }

        throw new IllegalArgumentException("Bad Arcadia Livy URL: " + getLivyUri());
    }
}
