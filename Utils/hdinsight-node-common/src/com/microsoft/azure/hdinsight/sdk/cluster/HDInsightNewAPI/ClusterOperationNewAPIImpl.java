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
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterOperationImpl;
import com.microsoft.azure.hdinsight.sdk.common.AzureManagementHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.errorresponse.ForbiddenHttpErrorStatus;
import com.microsoft.azure.hdinsight.sdk.common.errorresponse.HttpErrorStatus;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.entity.StringEntity;
import rx.Observable;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ClusterOperationNewAPIImpl extends ClusterOperationImpl implements ILogger {
    private static final String VERSION = "2015-03-01-preview";
    private HDInsightUserRoleType roleType;

    private Observable<ClusterConfiguration> getClusterConfigurationRequest(
            @NotNull final SubscriptionDetail subscription,
            @NotNull final String clusterId) throws IOException {
        String managementURI = AuthMethodManager.getInstance().getAzureManager().getManagementURI();
        AzureManagementHttpObservable httpObservable = new AzureManagementHttpObservable(subscription, VERSION);
        String url = URI.create(managementURI)
                .resolve(clusterId.replaceAll("/+$", "") + "/configurations").toString();
        StringEntity entity = new StringEntity("", StandardCharsets.UTF_8);
        entity.setContentType("application/json");
        return httpObservable
                .withUuidUserAgent()
                .post(url, entity, null, null, ClusterConfiguration.class);
    }

    public Observable<Boolean> isProbeGetConfigurationSucceed(
            final SubscriptionDetail subscription,
            final String clusterId) throws IOException {
        return getClusterConfigurationRequest(subscription, clusterId)
                .map(clusterConfiguration -> {
                    if (clusterConfiguration != null
                            && clusterConfiguration.getConfigurations() != null
                            && clusterConfiguration.getConfigurations().getGateway() != null
                            && clusterConfiguration.getConfigurations().getGateway().getUsername() != null
                            && clusterConfiguration.getConfigurations().getGateway().getPassword() != null) {
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
                            log().error("Error getting cluster configurations with NEW HDInsight API. " + clusterId, ex);
                            log().warn(((HttpErrorStatus) err).getErrorDetails());

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
                    return getClusterConfigurationRequest(subscription, clusterId)
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
