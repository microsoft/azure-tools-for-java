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

package com.microsoft.azure.hdinsight.sdk.cluster.HDInsightNewAPI;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterManager;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterOperationImpl;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterRawInfo;
import com.microsoft.azure.hdinsight.sdk.common.AzureManagementHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.errorresponse.ForbiddenHttpErrorStatus;
import com.microsoft.azure.hdinsight.sdk.common.errorresponse.GatewayTimeoutErrorStatus;
import com.microsoft.azure.hdinsight.sdk.common.errorresponse.HttpErrorStatus;
import com.microsoft.azure.hdinsight.sdk.common.errorresponse.NotFoundHttpErrorStatus;
import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.entity.StringEntity;
import rx.Observable;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ClusterOperationNewAPIImpl extends ClusterOperationImpl implements ILogger {
    private static final String VERSION = "2015-03-01-preview";
    private HDInsightUserRoleType roleType;
    @NotNull
    private final SubscriptionDetail subscription;
    @NotNull
    private final AzureManagementHttpObservable http;

    public ClusterOperationNewAPIImpl(@NotNull SubscriptionDetail subscription) {
        this.subscription = subscription;
        this.http = new AzureManagementHttpObservable(subscription, VERSION);
    }

    private AzureManagementHttpObservable getHttp() {
        return this.http;
    }

    private AzureManager getAzureManager() throws IOException {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();

        if (azureManager == null) {
            throw new AuthException("Not signed in. Can't send out the request.");
        }

        return azureManager;
    }

    public Observable<Map> getClusterCoreSiteRequest(@NotNull final String clusterId) throws IOException {
        String managementURI = getAzureManager().getManagementURI();
        String url = URI.create(managementURI)
                .resolve(clusterId.replaceAll("/+$", "") + "/configurations/core-site").toString();
        return getHttp()
                .withUuidUserAgent()
                .get(url, null, null, Map.class);
    }

    private Observable<ClusterConfiguration> getClusterConfigurationRequest(
            @NotNull final String clusterId) {
        try {
            String managementURI = getAzureManager().getManagementURI();
            String url = URI.create(managementURI)
                    .resolve(clusterId.replaceAll("/+$", "") + "/configurations").toString();
            StringEntity entity = new StringEntity("", StandardCharsets.UTF_8);
            entity.setContentType("application/json");
            return getHttp()
                    .withUuidUserAgent()
                    .post(url, entity, null, null, ClusterConfiguration.class);
        } catch (IOException ex) {
            log().info("Cluster ID: " + clusterId);
            log().warn("Error getting Azure Manager when probe new HDInsight API. " + ExceptionUtils.getStackTrace(ex));
            return Observable.empty();
        }
    }

    public Observable<Boolean> isProbeGetConfigurationSucceed(final ClusterRawInfo clusterRawInfo) {
        String clusterId = clusterRawInfo.getId();

        return getClusterConfigurationRequest(clusterId)
                .map(clusterConfiguration -> {
                    if (isClusterConfigurationValid(clusterRawInfo, clusterConfiguration)) {
                        setRoleType(HDInsightUserRoleType.OWNER);
                        return true;
                    } else {
                        final Map<String, String> properties = new HashMap<>();
                        properties.put("ClusterID", clusterId);
                        properties.put("StatusCode", "200");
                        properties.put("ErrorDetails", "Cluster credential is incomplete.");
                        AppInsightsClient.createByType(AppInsightsClient.EventType.Telemetry, this.getClass().getSimpleName(), null, properties);

                        log().error("Cluster credential is incomplete even if successfully get cluster configuration.");
                        return false;
                    }
                })
                .onErrorResumeNext(err -> {
                    if (err instanceof ForbiddenHttpErrorStatus) {
                        setRoleType(HDInsightUserRoleType.READER);
                        log().info("HDInsight user role type is READER. Request cluster ID: " + clusterId);

                        // Send telemetry when cluster role type is READER
                        final Map<String, String> properties = new HashMap<>();
                        properties.put("ClusterID", clusterId);
                        properties.put("RoleType", "READER");
                        properties.put("StatusCode", String.valueOf(((HttpErrorStatus) err).getStatusCode()));
                        properties.put("ErrorDetails", ((HttpErrorStatus) err).getErrorDetails());
                        AppInsightsClient.createByType(AppInsightsClient.EventType.Telemetry, this.getClass().getSimpleName(), null, properties);
                        return Observable.just(true);
                    } else {
                        if (err instanceof HttpErrorStatus) {
                            HDInsightNewApiUnavailableException ex = new HDInsightNewApiUnavailableException(err);
                            if (!(err instanceof NotFoundHttpErrorStatus
                                    || err instanceof GatewayTimeoutErrorStatus)) {
                                log().error(String.format(
                                        "Error getting cluster configurations with NEW HDInsight API: %s, %s",
                                        clusterId,
                                        ((HttpErrorStatus) err).getErrorDetails()), ex);
                            }

                            final Map<String, String> properties = new HashMap<>();
                            properties.put("ClusterID", clusterId);
                            properties.put("StackTrace", ExceptionUtils.getStackTrace(err));
                            properties.put("StatusCode", String.valueOf(((HttpErrorStatus) err).getStatusCode()));
                            properties.put("ErrorDetails", ((HttpErrorStatus) err).getErrorDetails());
                            AppInsightsClient.createByType(AppInsightsClient.EventType.Telemetry, this.getClass().getSimpleName(), null, properties);
                        }

                        log().warn("Error getting cluster configurations with NEW HDInsight API. " + ExceptionUtils.getStackTrace(err));
                        return Observable.just(false);
                    }
                });
    }

    private boolean isClusterConfigurationValid(ClusterRawInfo clusterRawInfo, @Nullable ClusterConfiguration clusterConfiguration) {
        if (clusterConfiguration == null
                || clusterConfiguration.getConfigurations() == null
                || clusterConfiguration.getConfigurations().getGateway() == null) {
            return false;
        }

        if (ClusterManager.getInstance().isMfaEspCluster(clusterRawInfo)) {
            return true;
        }

        Gateway gw = clusterConfiguration.getConfigurations().getGateway();
        if (Boolean.parseBoolean(gw.getIsEnabled())) {
            return true;
        }

        return gw.getUsername() != null || gw.getPassword() != null;
    }

    /**
     * get cluster configuration including http username, password, storage and additional storage account
     *
     * @param subscription
     * @param clusterId
     * @return cluster configuration info
     * @throws AzureCmdException
     */
    @Nullable
    @Override
    public com.microsoft.azure.hdinsight.sdk.cluster.ClusterConfiguration getClusterConfiguration(
            final SubscriptionDetail subscription,
            final String clusterId) throws AzureCmdException {
        assert roleType != null : "isProbeGetConfigurationSucceed() should be called first to determine role type";

        try {
            switch (roleType) {
                case OWNER:
                    return getClusterConfigurationRequest(clusterId)
                            // As you can see, the response class is
                            // com.microsoft.azure.hdinsight.sdk.cluster.HDInsightNewAPI.ClusterConfiguration.
                            // However, if we want to override method getClusterConfiguration, the method return type should be
                            // com.microsoft.azure.hdinsight.sdk.cluster.ClusterConfiguration.
                            // Therefore, we need to convert the new API response to old API.
                            .map(ClusterOperationNewAPIImpl::convertConfigurationToOldAPI)
                            .toBlocking()
                            .singleOrDefault(null);
                case READER:
                    // Do nothing if roleType is HDInsightUserRoleType.READER
                    return null;
                default:
                    return null;
            }
        } catch (Exception ex) {
            log().warn(ExceptionUtils.getStackTrace(ex));
            throw new AzureCmdException("Error getting cluster configuration", ex);
        }
    }

    @Nullable
    private static com.microsoft.azure.hdinsight.sdk.cluster.ClusterConfiguration convertConfigurationToOldAPI(
            @Nullable ClusterConfiguration srcClusterConfig) {
        return Optional.ofNullable(srcClusterConfig)
                .map(ClusterConfiguration::getConfigurations)
                .map(srcConfigurations -> {
                    com.microsoft.azure.hdinsight.sdk.cluster.Configurations dstConfigurations =
                            new com.microsoft.azure.hdinsight.sdk.cluster.Configurations();
                    Optional.ofNullable(srcConfigurations.getClusterIdentity())
                            .ifPresent(srcIdentity -> {
                                com.microsoft.azure.hdinsight.sdk.cluster.ClusterIdentity dstIdentity =
                                        new com.microsoft.azure.hdinsight.sdk.cluster.ClusterIdentity();
                                dstIdentity.setClusterIdentityresourceUri(srcIdentity.getClusterIdentityresourceUri());
                                dstIdentity.setClusterIdentitycertificatePassword(srcIdentity.getClusterIdentitycertificatePassword());
                                dstIdentity.setClusterIdentitycertificate(srcIdentity.getClusterIdentitycertificate());
                                dstIdentity.setClusterIdentityapplicationId(srcIdentity.getClusterIdentityapplicationId());
                                dstIdentity.setClusterIdentityaadTenantId(srcIdentity.getClusterIdentityaadTenantId());
                                dstConfigurations.setClusterIdentity(dstIdentity);
                            });
                    Optional.ofNullable((srcConfigurations.getGateway()))
                            .ifPresent(srcGateway -> {
                                com.microsoft.azure.hdinsight.sdk.cluster.Gateway dstGateway =
                                        new com.microsoft.azure.hdinsight.sdk.cluster.Gateway();
                                dstGateway.setIsEnabled(srcGateway.getIsEnabled());
                                dstGateway.setUsername(srcGateway.getUsername());
                                dstGateway.setPassword(srcGateway.getPassword());
                                dstConfigurations.setGateway(dstGateway);
                            });
                    Optional.ofNullable(srcConfigurations.getCoresite())
                            .ifPresent(srcCoresite -> dstConfigurations.setCoresite(srcCoresite));

                    com.microsoft.azure.hdinsight.sdk.cluster.ClusterConfiguration dstClusterConfig =
                            new com.microsoft.azure.hdinsight.sdk.cluster.ClusterConfiguration();
                    dstClusterConfig.setConfigurations(dstConfigurations);
                    return dstClusterConfig;
                })
                .orElse(null);
    }

    public void setRoleType(@NotNull HDInsightUserRoleType roleType) {
        this.roleType = roleType;
    }

    @NotNull
    public HDInsightUserRoleType getRoleType() {
        assert roleType != null : "isProbeGetConfigurationSucceed() should be called first to determine role type";

        return roleType;
    }
}
