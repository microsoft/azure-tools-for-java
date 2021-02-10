/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.appplatform.v2020_07_01.*;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.*;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper;
import com.microsoft.azure.toolkit.intellij.springcloud.runner.SpringCloudConstants;
import com.microsoft.azure.toolkit.intellij.springcloud.runner.deploy.SpringCloudDeployConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

import static com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel.uploadFileToStorage;

public class SpringCloudUtils {
    private static final int SCALING_TIME_OUT = 60; // Use same timeout as service
    private static final String FAILED_TO_SCALE_DEPLOYMENT = "Failed to scale deployment %s of spring cloud app %s";
    private static final String NO_CLUSTER = "No cluster named %s found in subscription %s";
    private static final Map<String, AppPlatformManager> SPRING_MANAGER_CACHE = new ConcurrentHashMap<>();
    private static final RuntimeVersion DEFAULT_RUNTIME_VERSION = RuntimeVersion.JAVA_8;
    private static final String RUNTIME_VERSION_PATTERN = "(J|j)ava((\\s)?|_)(8|11)$";

    public static AppResourceInner activateDeployment(AppResourceInner appResourceInner,
                                                      DeploymentResourceInner deploymentResourceInner,
                                                      SpringCloudDeployConfiguration configuration) {
        final AppPlatformManager appPlatformManager = getAppPlatformManager(configuration.getSubscriptionId());
        AppResourceProperties appResourceProperties =
            updateAppResourceProperties(appResourceInner.properties(), configuration)
                .withActiveDeploymentName(deploymentResourceInner.name())
                .withPublicProperty(configuration.isPublic());
        return appPlatformManager.apps().inner().update(
            configuration.getResourceGroup(),
            configuration.getClusterName(),
            configuration.getAppName(),
            appResourceInner.withProperties(appResourceProperties));
    }

    public static AppResourceInner createOrUpdateApp(final AppResourceInner app, SpringCloudDeployConfiguration configuration) {
        return Objects.isNull(app) ? createApp(configuration) : updateApp(app, configuration);
    }

    public static AppResourceInner getApp(SpringCloudDeployConfiguration configuration) {
        final AppPlatformManager appPlatformManager = getAppPlatformManager(configuration.getSubscriptionId());
        return appPlatformManager.apps().inner().get(
            SpringCloudIdHelper.getResourceGroup(configuration.getClusterId()),
            SpringCloudIdHelper.getClusterName(configuration.getClusterId()),
            configuration.getAppName(), "true");
    }

    public static AppResourceInner createApp(SpringCloudDeployConfiguration configuration) {
        final AppPlatformManager springManager = getAppPlatformManager(configuration.getSubscriptionId());
        final String resourceGroup = configuration.getResourceGroup();
        final String clusterName = configuration.getClusterName();
        final String appName = configuration.getAppName();

        final AppResourceProperties properties = updateAppResourceProperties(new AppResourceProperties(), configuration);
        final AppResourceInner app = new AppResourceInner().withProperties(properties);
        // Service didn't support update app with PUT (createOrUpdate)
        return springManager.apps().inner().createOrUpdate(resourceGroup, clusterName, appName, app);
    }

    public static AppResourceInner updateApp(AppResourceInner app, SpringCloudDeployConfiguration configuration) {
        final AppPlatformManager appPlatformManager = getAppPlatformManager(configuration.getSubscriptionId());
        final String resourceGroup = configuration.getResourceGroup();
        final String clusterName = configuration.getClusterName();
        final String appName = configuration.getAppName();

        final AppResourceProperties properties = updateAppResourceProperties(app.properties(), configuration);
        app.withProperties(properties);
        // Service didn't support update app with PUT (createOrUpdate)
        return appPlatformManager.apps().inner().update(resourceGroup, clusterName, appName, app);
    }

    public static UserSourceInfo uploadArtifact(SpringCloudDeployConfiguration configuration, String artifactPath)
        throws IOException, URISyntaxException, StorageException {
        // Upload artifact to correspond url
        final AppPlatformManager appPlatformManager = getAppPlatformManager(configuration.getSubscriptionId());
        final ResourceUploadDefinitionInner resourceUploadDefinition =
            appPlatformManager.apps().inner().getResourceUploadUrl(
                configuration.getResourceGroup(), configuration.getClusterName(), configuration.getAppName());
        uploadFileToStorage(new File(artifactPath), resourceUploadDefinition.uploadUrl());
        final UserSourceInfo userSourceInfo = new UserSourceInfo();
        // There are some issues with server side resourceUpload logic
        // Use uploadUrl instead of relativePath
        userSourceInfo.withType(UserSourceType.JAR).withRelativePath(resourceUploadDefinition.relativePath());
        return userSourceInfo;
    }

    public static DeploymentResourceInner createDeployment(SpringCloudDeployConfiguration configuration, UserSourceInfo userSourceInfo) {
        final AppPlatformManager springManager = getAppPlatformManager(configuration.getSubscriptionId());
        final String resourceGroup = configuration.getResourceGroup();
        final String clusterName = configuration.getClusterName();
        final String appName = configuration.getAppName();
        final String deploymentName = SpringCloudConstants.DEFAULT_DEPLOYMENT_NAME;

        final DeploymentResourceProperties deploymentProperties = new DeploymentResourceProperties();
        final SkuInner skuInner = SpringCloudUtils.initDeploymentSku(configuration);
        final DeploymentSettings deploymentSettings = new DeploymentSettings();
        final RuntimeVersion runtimeVersion = configuration.getRuntimeVersion();
        deploymentSettings.withCpu(configuration.getCpu())
                          .withJvmOptions(configuration.getJvmOptions())
                          .withMemoryInGB(configuration.getMemoryInGB())
                          .withRuntimeVersion(runtimeVersion)
                          .withEnvironmentVariables(configuration.getEnvironment());
        deploymentProperties.withSource(userSourceInfo).withDeploymentSettings(deploymentSettings);

        final DeploymentResourceInner tempDeploymentResource = new DeploymentResourceInner();
        tempDeploymentResource.withSku(skuInner).withProperties(deploymentProperties);

        // Create deployment
        final DeploymentsInner inner = springManager.deployments().inner();
        final DeploymentResourceInner deployment = inner.createOrUpdate(resourceGroup, clusterName, appName, deploymentName, tempDeploymentResource);
        inner.start(resourceGroup, clusterName, appName, deploymentName);
        return deployment;
    }

    public static DeploymentResourceInner updateDeployment(DeploymentResourceInner deployment, SpringCloudDeployConfiguration configuration,
                                                           UserSourceInfo userSourceInfo) throws AzureExecutionException {
        final AppPlatformManager springManager = getAppPlatformManager(configuration.getSubscriptionId());
        final String resourceGroup = configuration.getResourceGroup();
        final String clusterName = configuration.getClusterName();
        final String appName = configuration.getAppName();
        final String deploymentName = SpringCloudConstants.DEFAULT_DEPLOYMENT_NAME;

        if (isResourceScaled(configuration, deployment)) {
            Log.info("Scaling deployment...");
            scaleDeployment(deploymentName, configuration);
            Log.info("Scaling deployment done.");
        }
        final RuntimeVersion runtimeVersion = configuration.getRuntimeVersion();
        // Update deployment configuration, scale related parameters should be update in scaleDeployment()
        final DeploymentSettings newDeploymentSettings = new DeploymentSettings()
            .withJvmOptions(configuration.getJvmOptions())
            .withRuntimeVersion(runtimeVersion)
            .withEnvironmentVariables(configuration.getEnvironment());
        final DeploymentResourceProperties deploymentProperties = new DeploymentResourceProperties()
            .withSource(userSourceInfo)
            .withDeploymentSettings(newDeploymentSettings);
        deployment
            .withProperties(deploymentProperties)
            .withSku(null); // server cannot update and scale deployment at the same time
        final DeploymentsInner inner = springManager.deployments().inner();
        final DeploymentResourceInner result = inner.update(resourceGroup, clusterName, appName, deploymentName, deployment);
        inner.start(resourceGroup, clusterName, appName, deploymentName);
        return result;
    }

    private static DeploymentResourceInner scaleDeployment(String deploymentName, SpringCloudDeployConfiguration configuration)
        throws AzureExecutionException {
        final AppPlatformManager springManager = getAppPlatformManager(configuration.getSubscriptionId());
        final String resourceGroup = configuration.getResourceGroup();
        final String clusterName = configuration.getClusterName();
        final String appName = configuration.getAppName();

        final SkuInner skuInner = SpringCloudUtils.initDeploymentSku(configuration);
        final DeploymentSettings deploymentSettings = new DeploymentSettings()
            .withCpu(configuration.getCpu())
            .withMemoryInGB(configuration.getMemoryInGB());
        final DeploymentResourceProperties deploymentProperties = new DeploymentResourceProperties()
            .withDeploymentSettings(deploymentSettings);
        final DeploymentResourceInner tempDeploymentResource = new DeploymentResourceInner()
            .withSku(skuInner)
            .withProperties(deploymentProperties);

        springManager.deployments().inner().update(resourceGroup, clusterName, appName, deploymentName, tempDeploymentResource);
        // Wait until deployment scaling done
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<DeploymentResourceInner> future = executor.submit(() -> {
            DeploymentResourceInner result = springManager.deployments().inner().get(resourceGroup, clusterName, appName, deploymentName);
            while (!isStableDeploymentResourceProvisioningState(result.properties().provisioningState())) {
                Thread.sleep(1000);
                result = springManager.deployments().inner().get(resourceGroup, clusterName, appName, deploymentName);
            }
            return result;
        });
        try {
            return future.get(SCALING_TIME_OUT, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new AzureExecutionException(String.format(FAILED_TO_SCALE_DEPLOYMENT, deploymentName, appName), e);
        }
    }

    private static SkuInner initDeploymentSku(SpringCloudDeployConfiguration configuration) {
        final String clusterName = configuration.getClusterName();
        final String subscriptionId = configuration.getSubscriptionId();
        final ServiceResourceInner cluster = SpringCloudUtils.getCluster(subscriptionId, clusterName);
        final SkuInner clusterSku = cluster.sku();
        return new SkuInner().withName(clusterSku.name())
                             .withTier(clusterSku.tier())
                             .withCapacity(configuration.getInstanceCount());
    }

    private static ServiceResourceInner getCluster(String subscriptionId, String clusterName) {
        final AppPlatformManager springManager = getAppPlatformManager(subscriptionId);
        final PagedList<ServiceResourceInner> clusterList = springManager.inner().services().list();
        clusterList.loadAll();
        return clusterList.stream().filter(appClusterResourceInner -> appClusterResourceInner.name().equals(clusterName))
                          .findFirst()
                          .orElseThrow(() -> new InvalidParameterException(String.format(NO_CLUSTER, clusterName, subscriptionId)));
    }

    private static boolean isResourceScaled(SpringCloudDeployConfiguration deploymentConfiguration, DeploymentResourceInner deployment) {
        final DeploymentSettings deploymentSettings = deployment.properties().deploymentSettings();
        return !(Objects.equals(deploymentConfiguration.getCpu(), deploymentSettings.cpu()) &&
            Objects.equals(deploymentConfiguration.getMemoryInGB(), deploymentSettings.memoryInGB()) &&
            Objects.nonNull(deployment.sku()) &&
            Objects.equals(deploymentConfiguration.getInstanceCount(), deployment.sku().capacity()));
    }

    private static boolean isStableDeploymentResourceProvisioningState(DeploymentResourceProvisioningState state) {
        return state == DeploymentResourceProvisioningState.SUCCEEDED || state == DeploymentResourceProvisioningState.FAILED;
    }

    private static AppResourceProperties updateAppResourceProperties(AppResourceProperties properties,
                                                                     SpringCloudDeployConfiguration configuration) {
        // Enable persistent disk with default parameters
        final AppResourceProperties result = properties == null ? new AppResourceProperties() : properties;
        final PersistentDisk previousPersistentDisk = result.persistentDisk();
        final int preStorageSize = (previousPersistentDisk == null || previousPersistentDisk.sizeInGB() == null) ? 0 :
                                   previousPersistentDisk.sizeInGB();
        if (configuration.isEnablePersistentStorage() && preStorageSize <= 0) {
            result.withPersistentDisk(getDefaultPersistentDisk());
        } else if (!configuration.isEnablePersistentStorage() && preStorageSize > 0) {
            result.withPersistentDisk(getEmptyPersistentDisk());
        }
        // As we can't set public policy to an app without active deployment
        if (StringUtils.isNotEmpty(properties.activeDeploymentName())) {
            result.withActiveDeploymentName(properties.activeDeploymentName());
        } else {
            result.withActiveDeploymentName(SpringCloudConstants.DEFAULT_DEPLOYMENT_NAME);
        }
        result.withPublicProperty(configuration.isPublic());
        return result;
    }

    private static PersistentDisk getDefaultPersistentDisk() {
        final PersistentDisk persistentDisk = new PersistentDisk();
        persistentDisk.withMountPath(SpringCloudConstants.DEFAULT_PERSISTENT_DISK_MOUNT_PATH)
                      .withSizeInGB(SpringCloudConstants.DEFAULT_PERSISTENT_DISK_SIZE);
        return persistentDisk;
    }

    private static PersistentDisk getEmptyPersistentDisk() {
        final PersistentDisk persistentDisk = new PersistentDisk();
        persistentDisk.withMountPath(null)
                      .withSizeInGB(0);
        return persistentDisk;
    }

    private static AppPlatformManager getAppPlatformManager(String subscriptionId) {
        if (SPRING_MANAGER_CACHE.containsKey(subscriptionId)) {
            return SPRING_MANAGER_CACHE.get(subscriptionId);
        }
        final AppPlatformManager result = AuthMethodManager.getInstance().getAzureSpringCloudClient(subscriptionId);
        SPRING_MANAGER_CACHE.put(subscriptionId, result);
        return result;
    }

    @Nullable
    public static DeploymentResourceInner getActiveDeployment(final AppResourceInner app, SpringCloudDeployConfiguration configuration) {
        final String activeDeploymentName = getActiveDeploymentName(app);
        final String deploymentName = StringUtils.isEmpty(activeDeploymentName) ? SpringCloudConstants.DEFAULT_DEPLOYMENT_NAME : activeDeploymentName;
        return getDeployment(deploymentName, configuration);
    }

    public static String getActiveDeploymentName(final AppResourceInner appResourceInner) {
        return appResourceInner == null ? null : appResourceInner.properties().activeDeploymentName();
    }

    private static DeploymentResourceInner getDeployment(String deploymentName, SpringCloudDeployConfiguration configuration) {
        final AppPlatformManager manager = getAppPlatformManager(configuration.getSubscriptionId());
        return manager.deployments().inner().get(
            configuration.getResourceGroup(),
            configuration.getClusterName(),
            configuration.getAppName(),
            deploymentName);
    }
}
