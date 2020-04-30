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

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.common.UriUtil;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.sdk.rest.ObjectConvertUtils;
import com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models.GetSparkHistoryEndpointResponse;
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkComputeManager;
import com.microsoft.azure.projectarcadia.common.ArcadiaWorkSpace;
import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

public class SparkBatchArcadiaSubmission extends SparkBatchSubmission {
    public static final String ARCADIA_RESOURCE_ID = "https://dev.azuresynapse.net";
    public static final Pattern LIVY_URL_NO_WORKSPACE_IN_HOSTNAME_PATTERN = Pattern.compile(
            "(?<baseUrl>https?://[^/]+)/livyApi/versions/(?<apiVersion>[^/]+)/sparkPools/(?<compute>[^/]+)/?",
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
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) {
            throw new AuthException("Not signed in. Can't send out the request.");
        }

        return azureManager.getAccessToken(getTenantId(), getResourceEndpoint(), PromptBehavior.Auto);
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
    protected String getSparkHistoryEndpoint(@NotNull String baseUrl) {
        final int MAX_RETRY_COUNT = 3;
        final int DELAY_SECONDS = 10;
        int retries = 0;

        while (retries < MAX_RETRY_COUNT) {
            try {
                String requestUrl = baseUrl + "/sparkhistory/api/v1/historyServerProperties";
                HttpResponse httpResponse = getHttpResponseViaGet(requestUrl);
                if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                    Optional<GetSparkHistoryEndpointResponse> historyEndpointResponse =
                            ObjectConvertUtils.convertJsonToObject(httpResponse.getMessage(), GetSparkHistoryEndpointResponse.class);
                    return historyEndpointResponse
                            .orElseThrow(() -> new UnknownServiceException(
                                    String.format("Bad response when getting from %s, response: %s",
                                            requestUrl, httpResponse.getMessage())))
                            .getWebProxyEndpoint();
                }
            } catch (IOException ex) {
                log().warn("Got exception when getting Spark history endpoint, will retry.", ex);
            }

            try {
                // Retry interval
                sleep(TimeUnit.SECONDS.toMillis(DELAY_SECONDS));
            } catch (InterruptedException ex) {
                log().warn("Interrupted in retry attempting", ex);
            }

            retries++;
        }

        log().warn("Failed to get Spark history endpoint after retry " + MAX_RETRY_COUNT + " times.");
        return null;
    }

    @Nullable
    public URL getHistoryServerUrl(int livyId) {
        Matcher matcher = LIVY_URL_NO_WORKSPACE_IN_HOSTNAME_PATTERN.matcher(getLivyUri().toString());

        if (matcher.matches()) {
            String sparkHistoryEndpoint = getSparkHistoryEndpoint(matcher.group("baseUrl"));
            if (sparkHistoryEndpoint == null) {
                return null;
            }

            try {
                return new URL(String.format("%s/workspaces/%s/sparkcomputes/%s/livyid/%d/summary",
                        sparkHistoryEndpoint,
                        getWorkspaceName(),
                        matcher.group("compute"),
                        livyId));
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Bad Synapse history server URL", e);
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
                                new BasicNameValuePair("sparkPoolName", matcher.group("compute"))
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
