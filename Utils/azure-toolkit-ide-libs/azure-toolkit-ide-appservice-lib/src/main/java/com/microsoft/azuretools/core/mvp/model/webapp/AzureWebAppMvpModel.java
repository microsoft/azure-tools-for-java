/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.webapp;

import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;
import com.microsoft.azuretools.utils.IProgressIndicator;
import lombok.extern.java.Log;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
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
            name = "webapp.create_app.app|subscription|image",
            params = {
                "model.getWebAppName()",
                "model.getSubscriptionId()",
                "model.getPrivateRegistryImageSetting().getImageNameWithTag()"
            },
            type = AzureOperation.Type.SERVICE
    )
    public WebApp createAzureWebAppWithPrivateRegistryImage(@Nonnull WebAppOnLinuxDeployModel model) {
        final ResourceGroup resourceGroup = getOrCreateResourceGroup(model.getSubscriptionId(), model.getResourceGroupName(), model.getLocationName());
        final AppServicePlanEntity servicePlanEntity = AppServicePlanEntity.builder()
                .id(model.getAppServicePlanId())
                .subscriptionId(model.getSubscriptionId())
                .name(model.getAppServicePlanName())
                .resourceGroup(model.getResourceGroupName())
                .region(model.getLocationName())
                .operatingSystem(com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem.DOCKER)
                .pricingTier(com.microsoft.azure.toolkit.lib.appservice.model.PricingTier.fromString(model.getPricingSkuSize())).build();
        final AppServicePlan appServicePlan = getOrCreateAppServicePlan(servicePlanEntity);
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
                .withResourceGroup(resourceGroup.getName())
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
        name = "docker.update_image.app|image",
        params = {"nameFromResourceId(webAppId)", "imageSetting.getImageNameWithTag()"},
        type = AzureOperation.Type.SERVICE
    )
    public WebApp updateWebAppOnDocker(String webAppId, ImageSetting imageSetting) {
        final WebApp app = com.microsoft.azure.toolkit.lib.Azure.az(AzureAppService.class).webapp(webAppId);
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
            name = "webapp.create_app.app",
            params = {"model.getWebAppName()"},
            type = AzureOperation.Type.SERVICE
    )
    public WebApp createWebAppFromSettingModel(@Nonnull WebAppSettingModel model) {
        final ResourceGroup resourceGroup = getOrCreateResourceGroup(model.getSubscriptionId(), model.getResourceGroup(), model.getRegion());
        final AppServicePlanEntity servicePlanEntity = AppServicePlanEntity.builder()
                .id(model.getAppServicePlanId())
                .subscriptionId(model.getSubscriptionId())
                .name(model.getAppServicePlanName())
                .resourceGroup(model.getResourceGroup())
                .region(model.getRegion())
                .operatingSystem(com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem.fromString(model.getOperatingSystem()))
                .pricingTier(com.microsoft.azure.toolkit.lib.appservice.model.PricingTier.fromString(model.getPricing())).build();
        final AppServicePlan appServicePlan = getOrCreateAppServicePlan(servicePlanEntity);
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
                .withResourceGroup(resourceGroup.getName())
                .withPlan(appServicePlan.id())
                .withRuntime(model.getRuntime())
                .withDiagnosticConfig(diagnosticConfig)
                .commit();
    }

    // todo: Move duplicated codes to azure common library
    private ResourceGroup getOrCreateResourceGroup(String subscriptionId, String resourceGroup, String region) {
        return new CreateResourceGroupTask(subscriptionId, resourceGroup, Region.fromName(region)).execute();
    }

    private AppServicePlan getOrCreateAppServicePlan(AppServicePlanEntity servicePlanEntity) {
        final AzureAppService az = getAzureAppServiceClient(servicePlanEntity.getSubscriptionId());
        final AppServicePlan appServicePlan = az.appServicePlan(servicePlanEntity);
        if (appServicePlan.exists()) {
            return appServicePlan;
        }
        return appServicePlan.create()
                // todo: remove duplicated parameters declared in service plan entity
                .withName(servicePlanEntity.getName())
                .withResourceGroup(servicePlanEntity.getResourceGroup())
                .withRegion(Region.fromName(servicePlanEntity.getRegion()))
                .withPricingTier(servicePlanEntity.getPricingTier())
                .withOperatingSystem(servicePlanEntity.getOperatingSystem())
                .commit();
    }

    /**
     * API to create a new Deployment Slot by setting model.
     */
    @AzureOperation(
            name = "webapp.create_deployment.deployment|app",
            params = {"model.getNewSlotName()", "model.getWebAppName()"},
            type = AzureOperation.Type.SERVICE
    )
    public WebAppDeploymentSlot createDeploymentSlotFromSettingModel(@Nonnull final WebApp webApp, @Nonnull final WebAppSettingModel model) {
        String configurationSource = model.getNewSlotConfigurationSource();
        if (StringUtils.equalsIgnoreCase(configurationSource, webApp.name())) {
            configurationSource = WebAppDeploymentSlot.WebAppDeploymentSlotCreator.CONFIGURATION_SOURCE_PARENT;
        }
        if (StringUtils.equalsIgnoreCase(configurationSource, DO_NOT_CLONE_SLOT_CONFIGURATION)) {
            configurationSource = WebAppDeploymentSlot.WebAppDeploymentSlotCreator.CONFIGURATION_SOURCE_NEW;
        }
        return webApp.deploymentSlot(model.getNewSlotName()).create()
                .withName(model.getNewSlotName())
                .withConfigurationSource(configurationSource).commit();
    }

    public AzureAppService getAzureAppServiceClient(String subscriptionId) {
        return com.microsoft.azure.toolkit.lib.Azure.az(AzureAppService.class).subscription(subscriptionId);
    }

    @AzureOperation(
            name = "webapp.upload_artifact.artifact|app",
            params = {"file.getName()", "deployTarget.name()"},
            type = AzureOperation.Type.SERVICE
    )
    public void deployArtifactsToWebApp(@Nonnull final IWebAppBase deployTarget, @Nonnull final File file,
                                        boolean isDeployToRoot, @Nonnull final IProgressIndicator progressIndicator) {
        final Action<Void> retry = Action.retryFromFailure(() -> deployArtifactsToWebApp(deployTarget, file, isDeployToRoot, progressIndicator));
        if (!(deployTarget instanceof WebApp || deployTarget instanceof WebAppDeploymentSlot)) {
            final String error = "the deployment target is not a valid (deployment slot of) Web App";
            final String action = "select a valid Web App or deployment slot to deploy the artifact";
            throw new AzureToolkitRuntimeException(error, action, retry);
        }
        // stop target app service
        String stopMessage = deployTarget instanceof WebApp ? STOP_WEB_APP : STOP_DEPLOYMENT_SLOT;
        progressIndicator.setText(stopMessage);
        deployTarget.stop();

        final DeployType deployType = getDeployTypeByWebContainer(deployTarget.getRuntime().getWebContainer());
        // java se runtime will always deploy to root
        if (isDeployToRoot ||
                Objects.equals(deployTarget.getRuntime().getWebContainer(), WebContainer.JAVA_SE)) {
            deployTarget.deploy(deployType, file);
        } else {
            final String webappPath = String.format("webapps/%s", FilenameUtils.getBaseName(file.getName()).replaceAll("#", StringUtils.EMPTY));
            deployTarget.deploy(deployType, file, webappPath);
        }

        String successMessage = deployTarget instanceof WebApp ? DEPLOY_SUCCESS_WEB_APP : DEPLOY_SUCCESS_DEPLOYMENT_SLOT;
        progressIndicator.setText(successMessage);
        deployTarget.start();
    }

    // todo: get deploy type with runtime&artifact
    private static DeployType getDeployTypeByWebContainer(WebContainer webContainer) {
        if (Objects.equals(webContainer, WebContainer.JAVA_SE)) {
            return DeployType.JAR;
        }
        if (Objects.equals(webContainer, WebContainer.JBOSS_7)) {
            return DeployType.EAR;
        }
        return DeployType.WAR;
    }

    /**
     * Update app settings of deployment slot.
     * todo: move to app service library
     */
    @AzureOperation(
            name = "webapp.update_deployment_settings.deployment|app",
            params = {"slot.entity().getName()", "slot.entity().getWebappName()"},
            type = AzureOperation.Type.SERVICE
    )
    public void updateDeploymentSlotAppSettings(final WebAppDeploymentSlot slot, final Map<String, String> toUpdate) {
        slot.update().withAppSettings(toUpdate).commit();
    }

    private static final class SingletonHolder {
        private static final AzureWebAppMvpModel INSTANCE = new AzureWebAppMvpModel();
    }
}
