/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.common.appinsight.AppInsightsHttpRequestInstallIdMapRecord;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.MfaEspCluster;
import com.microsoft.azure.hdinsight.sdk.common.AuthType;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.service.ServiceManager;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.net.util.Base64;
import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class SparkBatchSubmission implements ILogger {
    SparkBatchSubmission() {
    }

    // Singleton Instance
    private static SparkBatchSubmission instance = null;

    public static SparkBatchSubmission getInstance() {
        if (instance == null) {
            synchronized (SparkBatchSubmission.class) {
                if (instance == null) {
                    instance = new SparkBatchSubmission();
                }
            }
        }

        return instance;
    }

    public static SparkBatchSubmission getClusterSubmission(IClusterDetail clusterDetail) {
        if (clusterDetail instanceof MfaEspCluster) {
            String id = ((MfaEspCluster) clusterDetail).getTenantId();
            return new SparkBatchEspMfaSubmission(id, clusterDetail.getName());
        } else {
            final SparkBatchSubmission submission = SparkBatchSubmission.getInstance();
            try {
                submission.setUsernamePasswordCredential(
                        clusterDetail.getHttpUserName(), clusterDetail.getHttpPassword());
            } catch (HDIException ex) {
                LoggerFactory
                    .getLogger(SparkBatchSubmission.class)
                    .warn("Got exception when try to get cluster username and password: " +
                            ex.getMessage() +
                            ", keep no auth.");
            }

            return submission;
        }
    }

    private CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

    private String authCode = null;

    @Nullable
    public String getAuthCode() {
        return authCode;
    }

    @NotNull
    public String getInstallationID() {
        if (HDInsightLoader.getHDInsightHelper() == null) {
            return "";
        }

        return HDInsightLoader.getHDInsightHelper().getInstallationId();
    }

    @Nullable
    protected SSLConnectionSocketFactory getSSLSocketFactory() {
        TrustStrategy ts = ServiceManager.getServiceProvider(TrustStrategy.class);
        SSLConnectionSocketFactory sslSocketFactory = null;

        if (ts != null) {
            try {
                SSLContext sslContext = new SSLContextBuilder()
                        .loadTrustMaterial(ts)
                        .build();

                sslSocketFactory = new SSLConnectionSocketFactory(sslContext,
                        HttpObservable.isSSLCertificateValidationDisabled() ?
                                NoopHostnameVerifier.INSTANCE :
                                new DefaultHostnameVerifier());
            } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                log().error("Prepare SSL Context for HTTPS failure. " + ExceptionUtils.getStackTrace(e));
            }
        }

        return sslSocketFactory;
    }

    public CloseableHttpClient getHttpClientWithoutCredentialAndRedirect() {
        return HttpClients.custom()
                          .useSystemProperties()
                          .setSSLSocketFactory(getSSLSocketFactory())
                          .disableRedirectHandling()
                          .build();
    }

    public CloseableHttpClient getHttpClient() throws IOException {
        return HttpClients.custom()
                          .useSystemProperties()
                          .setSSLSocketFactory(getSSLSocketFactory())
                          .setDefaultCredentialsProvider(credentialsProvider)
                          .build();
    }

    /**
     * Set http request credential using username and password
     * @param username : username
     * @param password : password
     */
    public void setUsernamePasswordCredential(String username, String password) {
        credentialsProvider.setCredentials(new AuthScope(AuthScope.ANY), new UsernamePasswordCredentials(username, password));
        if (username != null && password != null) {
            String auth = username + ":" + password;
            authCode = "Basic " + new String(Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1)));
        }
    }

    public CredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    @Nullable
    public String probeAuthRedirectUrl(String connectUrl) throws IOException {
        HttpResponse resp = negotiateAuthMethodWithResp(connectUrl);
        if (resp.getCode() == 302) {
            String location = resp.findHeader("Location");

            return isAzureADLoginUrl(location) ? location : null;
        }

        return null;
    }

    private boolean isAzureADLoginUrl(String url) {
        if (url.split(",").length > 1) {
            return false;
        }

        try {
            String loginHost = URI.create(url).getHost();

            if (StringUtils.isBlank(loginHost)) {
                return false;
            }

            return Arrays.stream(CommonConst.AZURE_LOGIN_HOSTS)
                    .anyMatch(it -> it.equalsIgnoreCase(loginHost));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public AuthType probeAuthType(String connectUrl) throws IOException {
        HttpResponse resp = negotiateAuthMethodWithResp(connectUrl);

        if (resp.getCode() >= 200 && resp.getCode() < 300) {
            return AuthType.NoAuth;
        }

        if (resp.getCode() == 302) {
            if (Optional.ofNullable(resp.findHeader("Location"))
                    .filter(this::isAzureADLoginUrl)
                    .isPresent()) {
                return AuthType.AADAuth;
            }

            return AuthType.NotSupported;
        }

        if (resp.getCode() == 401) {
            if (Optional.ofNullable(resp.findHeader("www-authenticate"))
                    .filter(authHeader -> StringUtils.startsWithIgnoreCase(authHeader, "Basic"))
                    .isPresent()) {
                return AuthType.BasicAuth;
            }

            return AuthType.NotSupported;
        }

        return AuthType.NotSupported;
    }

    public HttpResponse negotiateAuthMethodWithResp(String connectUrl) throws IOException {
        List<Header> additionHeader = new ArrayList<>();
        additionHeader.add(new BasicHeader("User-Agent", "Mozilla/5"));
        return getHttpResponseViaGet(connectUrl, getHttpClientWithoutCredentialAndRedirect(), additionHeader);
    }

    public HttpResponse getHttpResponseViaGet(String connectUrl, CloseableHttpClient httpclient, List<Header> additionHeaders) throws IOException {
        HttpGet httpGet = new HttpGet(connectUrl);
        httpGet.addHeader("Content-Type", "application/json");
        httpGet.addHeader("User-Agent", getUserAgentPerRequest(false));
        httpGet.addHeader("X-Requested-By", "ambari");

        if (additionHeaders != null) {
            for (Header replace : additionHeaders) {
                httpGet.removeHeaders(replace.getName());
                httpGet.addHeader(replace);
            }
        }

        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            return StreamUtil.getResultFromHttpResponse(response);
        }
    }

    public HttpResponse getHttpResponseViaGet(String connectUrl) throws IOException {
        return getHttpResponseViaGet(connectUrl, getHttpClient(), null);
    }

    public HttpResponse getHttpResponseViaHead(String connectUrl) throws IOException {
        CloseableHttpClient httpclient = getHttpClient();

        HttpHead httpHead = new HttpHead(connectUrl);
        httpHead.addHeader("Content-Type", "application/json");
        httpHead.addHeader("User-Agent", getUserAgentPerRequest(true));
        httpHead.addHeader("X-Requested-By", "ambari");

        // WORKAROUND: https://github.com/Microsoft/azure-tools-for-java/issues/1358
        // The Ambari local account will cause Kerberos authentication initializing infinitely.
        // Set a timer here to cancel the progress.
        httpHead.setConfig(
                RequestConfig
                        .custom()
                        .setSocketTimeout(3 * 1000)
                        .build());

        try (CloseableHttpResponse response = httpclient.execute(httpHead)) {
            return StreamUtil.getResultFromHttpResponse(response);
        }
    }

    /**
     * To generator a User-Agent for HTTP request with a random UUID
     *
     * @param isMapToInstallID true for create the relationship between the UUID and InstallationID
     * @return the unique UA string
     */
    @NotNull
    private String getUserAgentPerRequest(boolean isMapToInstallID) {
        String loadingClass = SparkBatchSubmission.class.getClassLoader().getClass().getName().toLowerCase();
        String userAgentSource = loadingClass.contains("intellij") ? "Azure Toolkit for IntelliJ " :
                (loadingClass.contains("eclipse") ? "Azure Toolkit for Eclipse " : "Azure HDInsight Java SDK ");
        String requestId = AppInsightsClient.getConfigurationSessionId() == null ?
                UUID.randomUUID().toString() :
                AppInsightsClient.getConfigurationSessionId();

        if (isMapToInstallID) {
            new AppInsightsHttpRequestInstallIdMapRecord(requestId, getInstallationID()).post();
        }

        return userAgentSource + requestId;
    }

    /**
     * get all batches spark jobs
     * @param connectUrl : eg http://localhost:8998/batches
     * @return response result
     * @throws IOException
     */
    public HttpResponse getAllBatchesSparkJobs(String connectUrl)throws IOException {
        return getHttpResponseViaGet(connectUrl);
    }

    /**
     * create batch spark job
     * @param connectUrl : eg http://localhost:8998/batches
     * @param submissionParameter : spark submission parameter
     * @return response result
     */
    public HttpResponse createBatchSparkJob(String connectUrl,
                                            SparkSubmissionParameter submissionParameter)
            throws IOException {
        CloseableHttpClient httpclient = getHttpClient();
        HttpPost httpPost = new HttpPost(connectUrl);
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.addHeader("User-Agent", getUserAgentPerRequest(true));
        httpPost.addHeader("X-Requested-By", "ambari");
        StringEntity postingString = new StringEntity(submissionParameter.serializeToJson());
        httpPost.setEntity(postingString);
        try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
            return StreamUtil.getResultFromHttpResponse(response);
        }
    }

    /**
     * get batch spark job status
     * @param connectUrl : eg http://localhost:8998/batches
     * @param batchId : batch Id
     * @return response result
     * @throws IOException
     */
    public HttpResponse getBatchSparkJobStatus(String connectUrl, int batchId) throws IOException {
        return getHttpResponseViaGet(connectUrl + "/" + batchId);
    }

    /**
     * kill batch job
     * @param connectUrl : eg http://localhost:8998/batches
     * @param batchId : batch Id
     * @return response result
     * @throws IOException
     */
    public HttpResponse killBatchJob(String connectUrl, int batchId)throws IOException {
        CloseableHttpClient httpclient = getHttpClient();
        HttpDelete httpDelete = new HttpDelete(connectUrl + "/" + batchId);
        httpDelete.addHeader("User-Agent", getUserAgentPerRequest(true));
        httpDelete.addHeader("Content-Type", "application/json");
        httpDelete.addHeader("X-Requested-By", "ambari");

        try (CloseableHttpResponse response = httpclient.execute(httpDelete)) {
            return StreamUtil.getResultFromHttpResponse(response);
        }
    }

    /**
     * get batch job full log
     * @param connectUrl : eg http://localhost:8998/batches
     * @param batchId : batch Id
     * @return response result
     * @throws IOException
     */
    public HttpResponse getBatchJobFullLog(String connectUrl, int batchId)throws IOException {
        return getHttpResponseViaGet(String.format("%s/%d/log?from=%d&size=%d", connectUrl, batchId, 0, Integer.MAX_VALUE));
    }
}
