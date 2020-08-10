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

package com.microsoft.intellij.util;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.*;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppPlatformManager;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.ResourceUploadDefinitionInner;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper;
import com.microsoft.intellij.runner.springcloud.SpringCloudConstants;
import com.microsoft.intellij.runner.springcloud.deploy.SpringCloudDeployConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

import static com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel.uploadFileToStorage;

public class SpringCloudUtils {
    private static final int SCALING_TIME_OUT = 60; // Use same timeout as service
    private static final String FAILED_TO_SCALE_DEPLOYMENT = "Failed to scale deployment %s of spring cloud app %s";
    private static final Map<String, AppPlatformManager> SPRING_MANAGER_CACHE = new ConcurrentHashMap<>();

    public static AppResourceInner activeDeployment(AppResourceInner appResourceInner,
                                                    DeploymentResourceInner deploymentResourceInner,
                                                    SpringCloudDeployConfiguration configuration) throws IOException {
        final AppPlatformManager appPlatformManager = getAppPlatformManager(configuration.getSubscriptionId());
        AppResourceProperties appResourceProperties =
                updateAppResourceProperties(appResourceInner.properties(), configuration)
                        .withActiveDeploymentName(deploymentResourceInner.name())
                        .withPublicProperty(configuration.isPublic());
        return appPlatformManager.apps().inner().update(
                configuration.getResourceGroup(),
                configuration.getClusterName(),
                configuration.getAppName(),
                appResourceProperties);
    }

    public static AppResourceInner createOrUpdateSpringCloudApp(SpringCloudDeployConfiguration configuration) throws IOException {
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
                       appResourceProperties) :
               appPlatformManager.apps().inner().update(
                       configuration.getResourceGroup(),
                       configuration.getClusterName(),
                       configuration.getAppName(),
                       appResourceProperties);
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
                                                            UserSourceInfo userSourceInfo) throws IOException {
        final AppPlatformManager springManager = getAppPlatformManager(configuration.getSubscriptionId());
        final DeploymentResourceProperties deploymentProperties = new DeploymentResourceProperties();
        final DeploymentSettings deploymentSettings = new DeploymentSettings();
        final RuntimeVersion runtimeVersion = configuration.getRuntimeVersion();
        deploymentSettings.withCpu(configuration.getCpu())
                          .withInstanceCount(configuration.getInstanceCount())
                          .withJvmOptions(configuration.getJvmOptions())
                          .withMemoryInGB(configuration.getMemoryInGB())
                          .withRuntimeVersion(runtimeVersion)
                          .withEnvironmentVariables(configuration.getEnvironment());
        deploymentProperties.withSource(userSourceInfo).withDeploymentSettings(deploymentSettings);
        // Create deployment
        final String deploymentName = SpringCloudConstants.DEFAULT_DEPLOYMENT_NAME;
        final DeploymentResourceInner deployment = springManager.deployments().inner().createOrUpdate(
                configuration.getResourceGroup(),
                configuration.getClusterName(),
                configuration.getAppName(),
                deploymentName,
                deploymentProperties);
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
            UserSourceInfo userSourceInfo) throws AzureExecutionException, IOException {
        final String deploymentName = deployment.name();
        final AppPlatformManager springManager = getAppPlatformManager(configuration.getSubscriptionId());
        final DeploymentSettings previousDeploymentSettings = deployment.properties().deploymentSettings();
        if (isResourceScaled(configuration, previousDeploymentSettings)) {
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
                deploymentProperties);
        springManager.deployments().inner().start(
                configuration.getResourceGroup(),
                configuration.getClusterName(),
                configuration.getAppName(),
                deploymentName);
        return result;
    }

    private static DeploymentResourceInner scaleDeployment(String deploymentName,
                                                           SpringCloudDeployConfiguration configuration)
            throws AzureExecutionException, IOException {
        final AppPlatformManager springManager = getAppPlatformManager(configuration.getSubscriptionId());
        final DeploymentResourceProperties deploymentProperties = new DeploymentResourceProperties();
        final DeploymentSettings deploymentSettings = new DeploymentSettings();
        deploymentSettings.withCpu(configuration.getCpu())
                          .withInstanceCount(configuration.getInstanceCount())
                          .withMemoryInGB(configuration.getMemoryInGB());
        deploymentProperties.withDeploymentSettings(deploymentSettings);
        springManager.deployments().inner().update(
                configuration.getResourceGroup(),
                configuration.getClusterName(),
                configuration.getAppName(),
                deploymentName,
                deploymentProperties);
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

    private static boolean isResourceScaled(SpringCloudDeployConfiguration deploymentConfiguration, DeploymentSettings deploymentSettings) {
        return !(Objects.equals(deploymentConfiguration.getCpu(), deploymentSettings.cpu()) &&
                Objects.equals(deploymentConfiguration.getMemoryInGB(), deploymentSettings.memoryInGB()) &&
                Objects.equals(deploymentConfiguration.getInstanceCount(), deploymentSettings.instanceCount()));
    }

    private static boolean isStableDeploymentResourceProvisioningState(DeploymentResourceProvisioningState state) {
        return state == DeploymentResourceProvisioningState.SUCCEEDED || state == DeploymentResourceProvisioningState.FAILED;
    }

    private static AppResourceProperties updateAppResourceProperties(AppResourceProperties appResourceProperties,
                                                                     SpringCloudDeployConfiguration configuration) {
        // Enable persistent disk with default parameters
        appResourceProperties = appResourceProperties == null ? new AppResourceProperties() : appResourceProperties;
        final PersistentDisk previousPersistentDisk = appResourceProperties.persistentDisk();
        final int preStorageSize = (previousPersistentDisk == null || previousPersistentDisk.sizeInGB() == null) ? 0 :
                previousPersistentDisk.sizeInGB();
        if (configuration.isEnablePersistentStorage() && preStorageSize <= 0) {
            appResourceProperties = appResourceProperties.withPersistentDisk(getDefaultPersistentDisk());
        } else if (!configuration.isEnablePersistentStorage() && preStorageSize > 0) {
            appResourceProperties = appResourceProperties.withPersistentDisk(getEmptyPersistentDisk());
        }
        // As we can't set public policy to an app without active deployment
        if (StringUtils.isNotEmpty(appResourceProperties.activeDeploymentName())) {
            appResourceProperties = appResourceProperties.withPublicProperty(configuration.isPublic());
        }
        return appResourceProperties;
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

    private static AppPlatformManager getAppPlatformManager(String subscriptionId) throws IOException {
        if (SPRING_MANAGER_CACHE.containsKey(subscriptionId)) {
            return SPRING_MANAGER_CACHE.get(subscriptionId);
        }
        final AppPlatformManager result = AuthMethodManager.getInstance().getAzureSpringCloudClient(subscriptionId);
        SPRING_MANAGER_CACHE.put(subscriptionId, result);
        return result;
    }

    private static DeploymentResourceInner getActiveDeployment(SpringCloudDeployConfiguration configuration)
            throws IOException {
        final AppPlatformManager manager = getAppPlatformManager(configuration.getSubscriptionId());
        final AppResourceInner appResourceInner = manager.apps().inner().get(
                configuration.getResourceGroup(),
                configuration.getClusterName(),
                configuration.getAppName());
        final String activeDeploymentName = appResourceInner.properties().activeDeploymentName();
        return StringUtils.isEmpty(activeDeploymentName) ? null : getDeployment(activeDeploymentName, configuration);
    }

    private static DeploymentResourceInner getDeployment(String deploymentName,
                                                         SpringCloudDeployConfiguration configuration)
            throws IOException {
        final AppPlatformManager manager = getAppPlatformManager(configuration.getSubscriptionId());
        return manager.deployments().inner().get(
                configuration.getResourceGroup(),
                configuration.getClusterName(),
                configuration.getAppName(),
                deploymentName);
    }
}
