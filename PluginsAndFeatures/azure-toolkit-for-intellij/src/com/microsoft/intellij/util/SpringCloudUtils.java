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

import static com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel.uploadFileToStorage;

public class SpringCloudUtils {
    public static AppResourceInner activeDeployment(AppResourceInner appResourceInner,
                                                    DeploymentResourceInner deploymentResourceInner,
                                                    SpringCloudDeployConfiguration configuration) throws IOException {
        final AppPlatformManager appPlatformManager = getAppPlatformManager(configuration.getSubscriptionId());
        AppResourceProperties appResourceProperties = updateAppResourceProperties(appResourceInner.properties(),
                                                                                  configuration);
        appResourceProperties = appResourceProperties
                .withActiveDeploymentName(deploymentResourceInner.name())
                .withPublicProperty(configuration.isPublic());
        return appPlatformManager.apps().inner().update(SpringCloudIdHelper.getResourceGroup(configuration.getClusterId()),
                                                        SpringCloudIdHelper.getClusterName(configuration.getClusterId()),
                                                        configuration.getAppName(), appResourceProperties);
    }

    public static DeploymentResourceInner createOrUpdateDeployment(SpringCloudDeployConfiguration configuration,
                                                                   UserSourceInfo userSourceInfo) throws IOException {
        final AppPlatformManager appPlatformManager = getAppPlatformManager(configuration.getSubscriptionId());
        final AppResourceInner appResourceInner =
                appPlatformManager.apps().inner()
                                  .get(SpringCloudIdHelper.getResourceGroup(configuration.getClusterId()),
                                       SpringCloudIdHelper.getClusterName(configuration.getClusterId()),
                                       configuration.getAppName());
        // get or create default deployment
        // Get existing deployment properties or new one
        final String targetDeployment;
        DeploymentResourceProperties deploymentResourceProperties;
        if (StringUtils.isEmpty(appResourceInner.properties().activeDeploymentName())) {
            // Create deployment properties
            targetDeployment = SpringCloudConstants.DEFAULT_DEPLOYMENT_NAME;
            deploymentResourceProperties = new DeploymentResourceProperties();
        } else {
            // Get existing deployment properties
            targetDeployment = appResourceInner.properties().activeDeploymentName();
            deploymentResourceProperties = appPlatformManager.deployments().inner().get(SpringCloudIdHelper.getResourceGroup(configuration.getClusterId()),
                                                                                        SpringCloudIdHelper.getClusterName(configuration.getClusterId()),
                                                                                        configuration.getAppName(), targetDeployment).properties();
        }
        deploymentResourceProperties = updateDeploymentProperties(deploymentResourceProperties, configuration).withSource(userSourceInfo);

        return appPlatformManager.deployments().inner().createOrUpdate(SpringCloudIdHelper.getResourceGroup(configuration.getClusterId()),
                                                                       SpringCloudIdHelper.getClusterName(configuration.getClusterId()),
                configuration.getAppName(), targetDeployment, deploymentResourceProperties);
    }

    public static AppResourceInner createOrUpdateSpringCloudApp(SpringCloudDeployConfiguration configuration) throws IOException {
        final AppPlatformManager appPlatformManager = getAppPlatformManager(configuration.getSubscriptionId());
        final AppResourceInner appResourceInner = appPlatformManager.apps().inner().get(SpringCloudIdHelper.getResourceGroup(configuration.getClusterId()),
                                                                                        SpringCloudIdHelper.getClusterName(configuration.getClusterId()),
                                                                                        configuration.getAppName());
        final AppResourceProperties appResourceProperties = updateAppResourceProperties(appResourceInner == null ?
                new AppResourceProperties() : appResourceInner.properties(), configuration);
        // Service didn't support update app with PUT (createOrUpdate)
        return appResourceInner == null ? appPlatformManager.apps().inner().createOrUpdate(SpringCloudIdHelper.getResourceGroup(configuration.getClusterId()),
                                                                                           SpringCloudIdHelper.getClusterName(configuration.getClusterId()),
                                                                                           configuration.getAppName(), appResourceProperties) :
                appPlatformManager.apps().inner().update(SpringCloudIdHelper.getResourceGroup(configuration.getClusterId()),
                                                         SpringCloudIdHelper.getClusterName(configuration.getClusterId()),
                        configuration.getAppName(), appResourceProperties);
    }

    public static UserSourceInfo deployArtifact(SpringCloudDeployConfiguration configuration, String artifactPath)
            throws IOException, URISyntaxException, StorageException {
        // Upload artifact to correspond url
        final AppPlatformManager appPlatformManager = getAppPlatformManager(configuration.getSubscriptionId());
        final ResourceUploadDefinitionInner resourceUploadDefinition = appPlatformManager.apps().inner()
                .getResourceUploadUrl(SpringCloudIdHelper.getResourceGroup(configuration.getClusterId()),
                                      SpringCloudIdHelper.getClusterName(configuration.getClusterId()), configuration.getAppName());
        uploadFileToStorage(new File(artifactPath), resourceUploadDefinition.uploadUrl());
        final UserSourceInfo userSourceInfo = new UserSourceInfo();
        // There are some issues with server side resourceUpload logic
        // Use uploadUrl instead of relativePath
        userSourceInfo.withType(UserSourceType.JAR).withRelativePath(resourceUploadDefinition.relativePath());
        return userSourceInfo;
    }

    private static DeploymentResourceProperties updateDeploymentProperties(DeploymentResourceProperties deploymentProperties,
                                                                           SpringCloudDeployConfiguration configuration) {
        DeploymentSettings deploymentSettings = deploymentProperties.deploymentSettings() == null ?
                new DeploymentSettings() : deploymentProperties.deploymentSettings();
        deploymentSettings = deploymentSettings.withJvmOptions(configuration.getJvmOptions())
                .withEnvironmentVariables(configuration.getEnvironment())
                .withCpu(configuration.getCpu())
                .withInstanceCount(configuration.getInstanceCount())
                .withMemoryInGB(configuration.getMemoryInGB())
                .withRuntimeVersion(configuration.getRuntimeVersion());
        return deploymentProperties.withDeploymentSettings(deploymentSettings);
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
        return AuthMethodManager.getInstance().getAzureSpringCloudClient(subscriptionId);
    }

}
