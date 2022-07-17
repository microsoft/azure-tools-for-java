/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.common.UriUtil;
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkComputeManager;
import com.microsoft.azure.projectarcadia.common.ArcadiaWorkSpace;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SparkBatchArcadiaSubmission extends SparkBatchSubmission {
    public static final String ARCADIA_RESOURCE_ID = "https://dev.azuresynapse.net";
    public static final Pattern LIVY_URL_NO_WORKSPACE_IN_HOSTNAME_PATTERN = Pattern.compile(
            "https?://[^.]+.(?<env>[^.]+).[^/]+/livyApi/versions/(?<apiVersion>[^/]+)/sparkPools/"
                    + "(?<pool>[^/]+)/?",
            Pattern.CASE_INSENSITIVE);
    public static final String SYNAPSE_STUDIO_WEB_ROOT_URL = "https://web.azuresynapse.net";

    private final @NotNull String workspaceName;
    private final @NotNull String tenantId;
    private final @NotNull URI livyUri;
    private final @NotNull String jobName;
    private final @Nullable String webUrl;

    public SparkBatchArcadiaSubmission(final @NotNull String tenantId,
                                       final @NotNull String workspaceName,
                                       final @NotNull URI livyUri,
                                       final @NotNull String jobName,
                                       final @Nullable String webUrl) {
        this.workspaceName = workspaceName;
        this.tenantId = tenantId;
        this.livyUri = UriUtil.normalizeWithSlashEnding(livyUri);
        this.jobName = jobName;
        this.webUrl = webUrl;
    }

    @Override
    public void setUsernamePasswordCredential(String username, String password) {
        throw new UnsupportedOperationException("Azure Synapse does not support UserName/Password credential");
    }

    @NotNull
    private String getResourceEndpoint() {
        return ARCADIA_RESOURCE_ID;
    }

    @NotNull
    protected String getAccessToken() throws IOException {
        return IdeAzureAccount.getInstance().getCredentialForTrack1(getTenantId()).getToken(getResourceEndpoint());
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
                        new BasicHeader("Authorization", "Bearer " + getAccessToken())))
                .setSSLSocketFactory(getSSLSocketFactory())
                .build();
    }

    @NotNull
    public URI getLivyUri() {
        return livyUri;
    }

    @Nullable
    public URL getHistoryServerUrl(int livyId) {
        Matcher matcher = LIVY_URL_NO_WORKSPACE_IN_HOSTNAME_PATTERN.matcher(getLivyUri().toString());

        if (matcher.matches()) {
            try {
                return new URL(String.format("%s/sparkui/%s_%s.azuresynapse.net"
                                                     + "/workspaces/%s/sparkpools/%s/livyid/%d/summary",
                        SYNAPSE_STUDIO_WEB_ROOT_URL,
                        getTenantId(),
                        matcher.group("env"),
                        getWorkspaceName(),
                        matcher.group("pool"),
                        livyId));
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Bad Spark history server URL", e);
            }
        }

        throw new IllegalArgumentException("Bad Synapse Livy URL: " + getLivyUri());
    }

    @NotNull
    public URL getJobDetailsWebUrl(int livyId) {
        Matcher matcher = LIVY_URL_NO_WORKSPACE_IN_HOSTNAME_PATTERN.matcher(getLivyUri().toString());
        if (matcher.matches()) {
            try {
                ArcadiaWorkSpace workSpace =
                        ArcadiaSparkComputeManager.getInstance()
                                .findWorkspace(getTenantId(), getWorkspaceName())
                                .toBlocking()
                                .first();
                // Currently we just concatenate the string and show it as the Spark job detail page URL.
                // We don't check if the URL is valid or not because we have never met any exceptions when clicking the
                // link during test. If there are errors reported by user that URL is invalid in the future, we will
                // add more validation code here at that time.
                URI rootUri = URI.create(this.webUrl == null ? SYNAPSE_STUDIO_WEB_ROOT_URL : this.webUrl).resolve("/");
                return new URIBuilder(rootUri)
                        .setPath("/monitoring/sparkapplication/" + jobName)
                        .setParameters(Arrays.asList(
                                new BasicNameValuePair("workspace", workSpace.getId()),
                                new BasicNameValuePair("livyId", String.valueOf(livyId)),
                                new BasicNameValuePair("sparkPoolName", matcher.group("pool"))
                        )).build().toURL();
            } catch (NoSuchElementException ex) {
                log().warn(String.format("Can't find workspace %s under tenant %s", getWorkspaceName(), getTenantId()), ex);
            } catch (MalformedURLException | URISyntaxException ex) {
                log().warn("Build Spark job detail web URL failed with error " + ex.getMessage(), ex);
                throw new IllegalArgumentException("Bad Spark job detail URL", ex);
            }
        }

        throw new IllegalArgumentException("Bad Synapse Livy URL: " + getLivyUri());
    }
}
