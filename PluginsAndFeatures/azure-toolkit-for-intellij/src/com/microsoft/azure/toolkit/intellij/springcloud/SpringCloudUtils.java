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

    public static AppResourceInner activeDeployment(AppResourceInner appResourceInner,
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

    public static AppResourceInner createOrUpdateSpringCloudApp(SpringCloudDeployConfiguration configuration) {
        final AppPlatformManager appPlatformManager = getAppPlatformManager(configuration.getSubscriptionId());
        final AppResourceInner appResourceInner = appPlatformManager.apps().inner().get(
                SpringCloudIdHelper.getResourceGroup(configuration.getClusterId()),
                SpringCloudIdHelper.getClusterName(configuration.getClusterId()),
                configuration.getAppName());
        final AppResourceProperties appResourceProperties = updateAppResourceProperties(appResourceInner == null ?
                new AppResourceProperties() : appResourceInner.properties(), configuration);
        // Service didn't support update app with PUT (createOrUpdate)
        return appResourceInner == null ?
               appPlatformManager.apps().inner().createOrUpdate(
                   configuration.getResourceGroup(),
                   configuration.getClusterName(),
                   configuration.getAppName(),
                   new AppResourceInner().withProperties(appResourceProperties)) :
               appPlatformManager.apps().inner().update(
                   configuration.getResourceGroup(),
                   configuration.getClusterName(),
                   configuration.getAppName(),
                   appResourceInner.withProperties(appResourceProperties));
    }

    public static UserSourceInfo deployArtifact(SpringCloudDeployConfiguration configuration, String artifactPath)
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

    public static DeploymentResourceInner createOrUpdateDeployment(SpringCloudDeployConfiguration configuration,
                                                                   UserSourceInfo userSourceInfo)
            throws IOException, AzureExecutionException {
        final DeploymentResourceInner deployment = getActiveDeployment(configuration);
        return deployment == null ? createDeployment(configuration, userSourceInfo) :
                updateDeployment(configuration, deployment, userSourceInfo);
    }

    private static DeploymentResourceInner createDeployment(SpringCloudDeployConfiguration configuration,
                                                            UserSourceInfo userSourceInfo) {
        final AppPlatformManager springManager = getAppPlatformManager(configuration.getSubscriptionId());
        final DeploymentResourceProperties deploymentProperties = new DeploymentResourceProperties();
        final DeploymentSettings deploymentSettings = new DeploymentSettings();
        final RuntimeVersion runtimeVersion = configuration.getRuntimeVersion();
        deploymentSettings.withCpu(configuration.getCpu())
                .withJvmOptions(configuration.getJvmOptions())
                .withMemoryInGB(configuration.getMemoryInGB())
                .withRuntimeVersion(runtimeVersion)
                .withEnvironmentVariables(configuration.getEnvironment());
        deploymentProperties.withSource(userSourceInfo).withDeploymentSettings(deploymentSettings);
        // Create deployment
        final String deploymentName = SpringCloudConstants.DEFAULT_DEPLOYMENT_NAME;

        final SkuInner skuInner = SpringCloudUtils.initDeploymentSku(configuration);
        final DeploymentResourceInner tempDeploymentResource = new DeploymentResourceInner();
        tempDeploymentResource.withSku(skuInner).withProperties(deploymentProperties);

        final DeploymentResourceInner deployment = springManager.deployments().inner().createOrUpdate(
                configuration.getResourceGroup(),
                configuration.getClusterName(),
                configuration.getAppName(),
                deploymentName,
                tempDeploymentResource);
        springManager.deployments().inner().start(
                configuration.getResourceGroup(),
                configuration.getClusterName(),
                configuration.getAppName(),
                deploymentName);
        return deployment;
    }

    private static DeploymentResourceInner updateDeployment(
            SpringCloudDeployConfiguration configuration,
            DeploymentResourceInner deployment,
            UserSourceInfo userSourceInfo) throws AzureExecutionException {
        final String deploymentName = deployment.name();
        final AppPlatformManager springManager = getAppPlatformManager(configuration.getSubscriptionId());
        if (isResourceScaled(configuration, deployment)) {
            Log.info("Scaling deployment...");
            scaleDeployment(deploymentName, configuration);
            Log.info("Scaling deployment done.");
        }
        final DeploymentResourceProperties deploymentProperties = new DeploymentResourceProperties();
        final DeploymentSettings newDeploymentSettings = new DeploymentSettings();
        final RuntimeVersion runtimeVersion = configuration.getRuntimeVersion();
        // Update deployment configuration, scale related parameters should be update in scaleDeployment()
        newDeploymentSettings.withJvmOptions(configuration.getJvmOptions())
                .withRuntimeVersion(runtimeVersion)
                .withEnvironmentVariables(configuration.getEnvironment());
        deploymentProperties.withSource(userSourceInfo).withDeploymentSettings(newDeploymentSettings);
        final DeploymentResourceInner result = springManager.deployments().inner().update(
                configuration.getResourceGroup(),
                configuration.getClusterName(),
                configuration.getAppName(),
                deploymentName,
                deployment);
        springManager.deployments().inner().start(
                configuration.getResourceGroup(),
                configuration.getClusterName(),
                configuration.getAppName(),
                deploymentName);
        return result;
    }

    private static DeploymentResourceInner scaleDeployment(String deploymentName, SpringCloudDeployConfiguration configuration)
            throws AzureExecutionException {
        final AppPlatformManager springManager = getAppPlatformManager(configuration.getSubscriptionId());
        final DeploymentResourceProperties deploymentProperties = new DeploymentResourceProperties();
        final DeploymentSettings deploymentSettings = new DeploymentSettings();
        deploymentSettings.withCpu(configuration.getCpu())
                .withMemoryInGB(configuration.getMemoryInGB());
        deploymentProperties.withDeploymentSettings(deploymentSettings);

        final SkuInner skuInner = SpringCloudUtils.initDeploymentSku(configuration);
        final DeploymentResourceInner tempDeploymentResource = new DeploymentResourceInner();
        tempDeploymentResource.withSku(skuInner).withProperties(deploymentProperties);

        springManager.deployments().inner().update(
                configuration.getResourceGroup(),
                configuration.getClusterName(),
                configuration.getAppName(),
                deploymentName,
                tempDeploymentResource);
        // Wait until deployment scaling done
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<DeploymentResourceInner> future = executor.submit(() -> {
            DeploymentResourceInner result = getActiveDeployment(configuration);
            while (!isStableDeploymentResourceProvisioningState(result.properties().provisioningState())) {
                Thread.sleep(1000);
                result = getDeployment(deploymentName, configuration);
            }
            return result;
        });
        try {
            return future.get(SCALING_TIME_OUT, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new AzureExecutionException(
                    String.format(FAILED_TO_SCALE_DEPLOYMENT, deploymentName, configuration.getAppName()), e);
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

    private static AppResourceProperties updateAppResourceProperties(AppResourceProperties appResourceProperties,
                                                                     SpringCloudDeployConfiguration configuration) {
        // Enable persistent disk with default parameters
        final AppResourceProperties result = appResourceProperties == null ?
                new AppResourceProperties() : appResourceProperties;
        final PersistentDisk previousPersistentDisk = result.persistentDisk();
        final int preStorageSize = (previousPersistentDisk == null || previousPersistentDisk.sizeInGB() == null) ? 0 :
                previousPersistentDisk.sizeInGB();
        if (configuration.isEnablePersistentStorage() && preStorageSize <= 0) {
            result.withPersistentDisk(getDefaultPersistentDisk());
        } else if (!configuration.isEnablePersistentStorage() && preStorageSize > 0) {
            result.withPersistentDisk(getEmptyPersistentDisk());
        }
        // As we can't set public policy to an app without active deployment
        if (StringUtils.isNotEmpty(appResourceProperties.activeDeploymentName())) {
            result.withPublicProperty(configuration.isPublic());
        }
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
    private static DeploymentResourceInner getActiveDeployment(SpringCloudDeployConfiguration configuration) {
        final AppPlatformManager manager = getAppPlatformManager(configuration.getSubscriptionId());
        final AppResourceInner appResourceInner = manager.apps().inner().get(
                configuration.getResourceGroup(),
                configuration.getClusterName(),
                configuration.getAppName());
        final String activeDeploymentName = appResourceInner.properties().activeDeploymentName();
        return StringUtils.isEmpty(activeDeploymentName) ? null : getDeployment(activeDeploymentName, configuration);
    }

    private static DeploymentResourceInner getDeployment(String deploymentName,
                                                         SpringCloudDeployConfiguration configuration) {
        final AppPlatformManager manager = getAppPlatformManager(configuration.getSubscriptionId());
        return manager.deployments().inner().get(
                configuration.getResourceGroup(),
                configuration.getClusterName(),
                configuration.getAppName(),
                deploymentName);
    }
}
