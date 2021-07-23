/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.webapp;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.utils.IProgressIndicator;
import lombok.extern.java.Log;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Map;
import java.util.Objects;

// todo: Refactor to tasks in app service library
@Deprecated
@Log
public class AzureWebAppMvpModel {

    public static final String DO_NOT_CLONE_SLOT_CONFIGURATION = "Don't clone configuration from an existing slot";

    private static final String STOP_WEB_APP = "Stopping web app...";
    private static final String STOP_DEPLOYMENT_SLOT = "Stopping deployment slot...";
    private static final String DEPLOY_SUCCESS_WEB_APP = "Deploy succeed, restarting web app...";
    private static final String DEPLOY_SUCCESS_DEPLOYMENT_SLOT = "Deploy succeed, restarting deployment slot...";

    private AzureWebAppMvpModel() {
    }

    public static AzureWebAppMvpModel getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * API to create Web App on Docker.
     *
     * @param model parameters
     * @return instance of created WebApp
     */
    @AzureOperation(
            name = "docker.create_from_private_image",
            params = {
                "model.getWebAppName()",
                "model.getSubscriptionId()",
                "model.getPrivateRegistryImageSetting().getImageNameWithTag()"
            },
            type = AzureOperation.Type.SERVICE
    )
    public IWebApp createAzureWebAppWithPrivateRegistryImage(@NotNull WebAppOnLinuxDeployModel model) {
        final ResourceGroup resourceGroup = getOrCreateResourceGroup(model.getSubscriptionId(), model.getResourceGroupName(), model.getLocationName());
        final AppServicePlanEntity servicePlanEntity = AppServicePlanEntity.builder()
                .id(model.getAppServicePlanId())
                .subscriptionId(model.getSubscriptionId())
                .name(model.getAppServicePlanName())
                .resourceGroup(model.getResourceGroupName())
                .region(model.getLocationName())
                .operatingSystem(com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem.DOCKER)
                .pricingTier(com.microsoft.azure.toolkit.lib.appservice.model.PricingTier.fromString(model.getPricingSkuSize())).build();
        final IAppServicePlan appServicePlan = getOrCreateAppServicePlan(servicePlanEntity);
        final PrivateRegistryImageSetting pr = model.getPrivateRegistryImageSetting();
        // todo: support start up file in docker configuration
        final DockerConfiguration dockerConfiguration = DockerConfiguration.builder()
                .image(pr.getImageTagWithServerUrl())
                .registryUrl(pr.getServerUrl())
                .userName(pr.getUsername())
                .password(pr.getPassword())
                .startUpCommand(pr.getStartupFile()).build();
        return getAzureAppServiceClient(model.getSubscriptionId()).webapp(model.getResourceGroupName(), model.getWebAppName()).create()
                .withName(model.getWebAppName())
                .withResourceGroup(resourceGroup.name())
                .withPlan(appServicePlan.id())
                .withRuntime(Runtime.DOCKER)
                .withDockerConfiguration(dockerConfiguration)
                .commit();
    }

    /**
     * Update container settings for existing Web App on Linux.
     *
     * @param webAppId     id of Web App on Linux instance
     * @param imageSetting new container settings
     * @return instance of the updated Web App on Linux
     */
    @AzureOperation(
        name = "docker|image.update",
        params = {"nameFromResourceId(webAppId)", "imageSetting.getImageNameWithTag()"},
        type = AzureOperation.Type.SERVICE
    )
    public IWebApp updateWebAppOnDocker(String webAppId, ImageSetting imageSetting) {
        final IWebApp app = com.microsoft.azure.toolkit.lib.Azure.az(AzureAppService.class).webapp(webAppId);
        // clearTags(app);
        if (imageSetting instanceof PrivateRegistryImageSetting) {
            final PrivateRegistryImageSetting pr = (PrivateRegistryImageSetting) imageSetting;
            final DockerConfiguration dockerConfiguration = DockerConfiguration.builder()
                    .image(pr.getImageTagWithServerUrl())
                    .registryUrl(pr.getServerUrl())
                    .userName(pr.getUsername())
                    .password(pr.getPassword())
                    .startUpCommand(pr.getStartupFile()).build();
            app.update().withDockerConfiguration(dockerConfiguration).commit();
        }
        // status-free restart.
        app.restart();
        return app;
    }

    /**
     * API to create new Web App by setting model.
     */
    @AzureOperation(
            name = "webapp.create_detail",
            params = {"model.getWebAppName()"},
            type = AzureOperation.Type.SERVICE
    )
    public IWebApp createWebAppFromSettingModel(@NotNull WebAppSettingModel model) {
        final ResourceGroup resourceGroup = getOrCreateResourceGroup(model.getSubscriptionId(), model.getResourceGroup(), model.getRegion());
        final AppServicePlanEntity servicePlanEntity = AppServicePlanEntity.builder()
                .id(model.getAppServicePlanId())
                .subscriptionId(model.getSubscriptionId())
                .name(model.getAppServicePlanName())
                .resourceGroup(model.getResourceGroup())
                .region(model.getRegion())
                .operatingSystem(com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem.fromString(model.getOperatingSystem()))
                .pricingTier(com.microsoft.azure.toolkit.lib.appservice.model.PricingTier.fromString(model.getPricing())).build();
        final IAppServicePlan appServicePlan = getOrCreateAppServicePlan(servicePlanEntity);
        final DiagnosticConfig diagnosticConfig = DiagnosticConfig.builder()
                .enableApplicationLog(model.isEnableApplicationLog())
                .applicationLogLevel(com.microsoft.azure.toolkit.lib.appservice.model.LogLevel.fromString(model.getApplicationLogLevel()))
                .enableWebServerLogging(model.isEnableWebServerLogging())
                .webServerLogQuota(model.getWebServerLogQuota())
                .webServerRetentionPeriod(model.getWebServerRetentionPeriod())
                .enableDetailedErrorMessage(model.isEnableDetailedErrorMessage())
                .enableFailedRequestTracing(model.isEnableFailedRequestTracing()).build();
        return getAzureAppServiceClient(model.getSubscriptionId()).webapp(model.getResourceGroup(), model.getWebAppName()).create()
                .withName(model.getWebAppName())
                .withResourceGroup(resourceGroup.name())
                .withPlan(appServicePlan.id())
                .withRuntime(model.getRuntime())
                .withDiagnosticConfig(diagnosticConfig)
                .commit();
    }

    // todo: Move duplicated codes to azure common library
    private ResourceGroup getOrCreateResourceGroup(String subscriptionId, String resourceGroup, String region) {
        final AzureResourceManager az =
                com.microsoft.azure.toolkit.lib.Azure.az(AzureAppService.class).getAzureResourceManager(subscriptionId);
        try {
            return az.resourceGroups().getByName(resourceGroup);
        } catch (ManagementException e) {
            return az.resourceGroups().define(resourceGroup).withRegion(region).create();
        }
    }

    private IAppServicePlan getOrCreateAppServicePlan(AppServicePlanEntity servicePlanEntity) {
        final AzureAppService az = getAzureAppServiceClient(servicePlanEntity.getSubscriptionId());
        final IAppServicePlan appServicePlan = az.appServicePlan(servicePlanEntity);
        if (appServicePlan.exists()) {
            return appServicePlan;
        }
        return appServicePlan.create()
                // todo: remove duplicated parameters declared in service plan entity
                .withName(servicePlanEntity.getName())
                .withResourceGroup(servicePlanEntity.getResourceGroup())
                .withRegion(com.microsoft.azure.toolkit.lib.common.model.Region.fromName(servicePlanEntity.getRegion()))
                .withPricingTier(servicePlanEntity.getPricingTier())
                .withOperatingSystem(servicePlanEntity.getOperatingSystem())
                .commit();
    }

    /**
     * API to create a new Deployment Slot by setting model.
     */
    @AzureOperation(
            name = "webapp|deployment.create",
            params = {"model.getNewSlotName()", "model.getWebAppName()"},
            type = AzureOperation.Type.SERVICE
    )
    public IWebAppDeploymentSlot createDeploymentSlotFromSettingModel(@NotNull final IWebApp webApp, @NotNull final WebAppSettingModel model) {
        String configurationSource = model.getNewSlotConfigurationSource();
        if (StringUtils.equalsIgnoreCase(configurationSource, webApp.name())) {
            configurationSource = WebAppDeploymentSlot.WebAppDeploymentSlotCreator.CONFIGURATION_SOURCE_PARENT;
        }
        if (StringUtils.equalsIgnoreCase(configurationSource, DO_NOT_CLONE_SLOT_CONFIGURATION)) {
            configurationSource = WebAppDeploymentSlot.WebAppDeploymentSlotCreator.CONFIGURATION_SOURCE_NEW;
        }
        return webApp.deploymentSlot(model.getSlotName()).create()
                .withName(model.getNewSlotName())
                .withConfigurationSource(configurationSource).commit();
    }

    public AzureAppService getAzureAppServiceClient(String subscriptionId) {
        return com.microsoft.azure.toolkit.lib.Azure.az(AzureAppService.class).subscription(subscriptionId);
    }

    @AzureOperation(
            name = "webapp|artifact.upload",
            params = {"file.getName()", "deployTarget.name()"},
            type = AzureOperation.Type.SERVICE
    )
    public void deployArtifactsToWebApp(@NotNull final IWebAppBase deployTarget, @NotNull final File file,
                                        boolean isDeployToRoot, @NotNull final IProgressIndicator progressIndicator) {
        if (!(deployTarget instanceof IWebApp || deployTarget instanceof IWebAppDeploymentSlot)) {
            final String error = "the deployment target is not a valid (deployment slot of) Web App";
            final String action = "select a valid Web App or deployment slot to deploy the artifact";
            throw new AzureToolkitRuntimeException(error, action);
        }
        // stop target app service
        String stopMessage = deployTarget instanceof IWebApp ? STOP_WEB_APP : STOP_DEPLOYMENT_SLOT;
        progressIndicator.setText(stopMessage);
        deployTarget.stop();

        final DeployType deployType = getDeployTypeByWebContainer(deployTarget.getRuntime().getWebContainer());
        // java se runtime will always deploy to root
        if (isDeployToRoot ||
                Objects.equals(deployTarget.getRuntime().getWebContainer(), com.microsoft.azure.toolkit.lib.appservice.model.WebContainer.JAVA_SE)) {
            deployTarget.deploy(deployType, file);
        } else {
            final String webappPath = String.format("webapps/%s", FilenameUtils.getBaseName(file.getName()).replaceAll("#", StringUtils.EMPTY));
            deployTarget.deploy(deployType, file, webappPath);
        }

        String successMessage = deployTarget instanceof IWebApp ? DEPLOY_SUCCESS_WEB_APP : DEPLOY_SUCCESS_DEPLOYMENT_SLOT;
        progressIndicator.setText(successMessage);
        deployTarget.start();
    }

    // todo: get deploy type with runtime&artifact
    private static DeployType getDeployTypeByWebContainer(com.microsoft.azure.toolkit.lib.appservice.model.WebContainer webContainer) {
        if (Objects.equals(webContainer, com.microsoft.azure.toolkit.lib.appservice.model.WebContainer.JAVA_SE)) {
            return DeployType.JAR;
        }
        if (Objects.equals(webContainer, com.microsoft.azure.toolkit.lib.appservice.model.WebContainer.JBOSS_72)) {
            return DeployType.EAR;
        }
        return DeployType.WAR;
    }

    /**
     * Update app settings of deployment slot.
     * todo: move to app service library
     */
    @AzureOperation(
            name = "webapp|deployment.update_settings",
            params = {"slot.entity().getName()", "slot.entity().getWebappName()"},
            type = AzureOperation.Type.SERVICE
    )
    public void updateDeploymentSlotAppSettings(final IWebAppDeploymentSlot slot, final Map<String, String> toUpdate) {
        final AzureResourceManager azureResourceManager =
                com.microsoft.azure.toolkit.lib.Azure.az(AzureAppService.class).getAzureResourceManager(slot.entity().getSubscriptionId());
        final com.azure.resourcemanager.appservice.models.DeploymentSlot slotClient =
                azureResourceManager.webApps().getById(slot.webApp().id()).deploymentSlots().getById(slot.id());
        slotClient.update().withAppSettings(toUpdate).apply();
    }

    private static final class SingletonHolder {
        private static final AzureWebAppMvpModel INSTANCE = new AzureWebAppMvpModel();
    }
}
