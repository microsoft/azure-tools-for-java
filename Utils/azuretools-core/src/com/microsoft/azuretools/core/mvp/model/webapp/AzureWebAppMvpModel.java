/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.webapp;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.CsmPublishingProfileOptions;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.LogLevel;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.management.appservice.PublishingProfileFormat;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.SkuName;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.management.appservice.WebAppDiagnosticLogs;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.appservice.implementation.GeoRegionInner;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceUtils;
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
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.CacheEvict;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.cache.Preload;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.IProgressIndicator;
import com.microsoft.azuretools.utils.WebAppUtils;
import lombok.extern.java.Log;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

@Deprecated
@Log
public class AzureWebAppMvpModel {

    private static final Logger logger = Logger.getLogger(AzureWebAppMvpModel.class.getName());

    public static final String DO_NOT_CLONE_SLOT_CONFIGURATION = "Don't clone configuration from an existing slot";

    private static final List<WebAppUtils.WebContainerMod> JAVA_8_JAR_CONTAINERS =
        Collections.singletonList(WebAppUtils.WebContainerMod.Java_SE_8);
    private static final List<WebAppUtils.WebContainerMod> JAVA_11_JAR_CONTAINERS = Collections.singletonList(
        WebAppUtils.WebContainerMod.Java_SE_11);
    private static final String STOP_WEB_APP = "Stopping web app...";
    private static final String STOP_DEPLOYMENT_SLOT = "Stopping deployment slot...";
    private static final String DEPLOY_SUCCESS_WEB_APP = "Deploy succeed, restarting web app...";
    private static final String DEPLOY_SUCCESS_DEPLOYMENT_SLOT = "Deploy succeed, restarting deployment slot...";

    private AzureWebAppMvpModel() {
    }
    public static final String CACHE_SUBSCRIPTION_WEBAPPS = "subscription-webapps";

    public static AzureWebAppMvpModel getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * get the web app by ID
     */
    @NotNull
    @AzureOperation(
        name = "webapp.get",
        params = {"nameFromResourceId(id)", "sid"},
        type = AzureOperation.Type.SERVICE
    )
    public WebApp getWebAppById(String sid, String id) throws AzureToolkitRuntimeException {
        final WebApp webapp = this.getNullableWebAppById(sid, id);
        if (Objects.isNull(webapp)) {
            final String error = String.format("cannot find WebApp[%s] in subscription[%s]", ResourceUtils.nameFromResourceId(id), sid);
            final String action = String.format("confirm if the WebApp[id=%s] still exists", ResourceUtils.nameFromResourceId(id));
            throw new AzureToolkitRuntimeException(error, action);
        }
        return webapp;
    }

    /**
     * get the web app by ID.
     */
    @Nullable
    public WebApp getNullableWebAppById(String sid, String id) {
        final Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        return azure.webApps().getById(id);
    }

    @AzureOperation(
        name = "webapp.get",
        params = {"appName", "sid"},
        type = AzureOperation.Type.SERVICE
    )
    public WebApp getWebAppByName(String sid, String resourceGroup, String appName) {
        final Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        return azure.webApps().getByResourceGroup(resourceGroup, appName);
    }

    /**
     * API to create new Web App by setting model.
     */
    @AzureOperation(
        name = "webapp.create_detail",
        params = {"model.getWebAppName()"},
        type = AzureOperation.Type.SERVICE
    )
    public WebApp createWebApp(@NotNull WebAppSettingModel model) {
        switch (model.getOS()) {
            case WINDOWS:
                return createWebAppOnWindows(model);
            case LINUX:
                return createWebAppOnLinux(model);
            default:
                throw new IllegalArgumentException("Invalid operating system setting: " + model.getOS());
        }
    }

    /**
     * API to create a new Deployment Slot by setting model.
     */
    @AzureOperation(
        name = "webapp|deployment.create",
        params = {"model.getNewSlotName()", "model.getWebAppName()"},
        type = AzureOperation.Type.SERVICE
    )
    public DeploymentSlot createDeploymentSlot(@NotNull WebAppSettingModel model) {
        final WebApp app = getWebAppById(model.getSubscriptionId(), model.getWebAppId());
        final String name = model.getNewSlotName();
        final String configurationSource = model.getNewSlotConfigurationSource();
        final DeploymentSlot.DefinitionStages.Blank definedSlot = app.deploymentSlots().define(name);

        if (configurationSource.equals(app.name())) {
            return definedSlot.withConfigurationFromParent().create();
        }

        final DeploymentSlot configurationSourceSlot = app.deploymentSlots()
                                                          .list()
                                                          .stream()
                                                          .filter(s -> configurationSource.equals(s.name()))
                                                          .findAny()
                                                          .orElse(null);

        if (configurationSourceSlot != null) {
            return definedSlot.withConfigurationFromDeploymentSlot(configurationSourceSlot).create();
        } else {
            return definedSlot.withBrandNewConfiguration().create();
        }
    }

    /**
     * API to create Web App on Windows .
     *
     * @param model parameters
     * @return instance of created WebApp
     */
    public WebApp createWebAppOnWindows(@NotNull WebAppSettingModel model) {
        final Azure azure = AuthMethodManager.getInstance().getAzureClient(model.getSubscriptionId());

        WebApp.DefinitionStages.WithCreate withCreate;
        if (model.isCreatingAppServicePlan()) {
            withCreate = withCreateNewWindowsServicePlan(azure, model);
        } else {
            withCreate = withExistingWindowsServicePlan(azure, model);
        }
        withCreate = applyDiagnosticConfig(withCreate, model);
        return withCreate
            .withJavaVersion(model.getJdkVersion())
            .withWebContainer(WebContainer.fromString(model.getWebContainer()))
            .create();
    }

    /**
     * API to create Web App on Linux.
     */
    public WebApp createWebAppOnLinux(@NotNull WebAppSettingModel model) {
        final Azure azure = AuthMethodManager.getInstance().getAzureClient(model.getSubscriptionId());

        final WebApp.DefinitionStages.WithDockerContainerImage withDockerContainerImage;
        if (model.isCreatingAppServicePlan()) {
            withDockerContainerImage = withCreateNewLinuxServicePlan(azure, model);
        } else {
            withDockerContainerImage = withExistingLinuxServicePlan(azure, model);
        }
        final WebApp.DefinitionStages.WithCreate withCreate =
            withDockerContainerImage.withBuiltInImage(model.getLinuxRuntime());
        return applyDiagnosticConfig(withCreate, model).create();
    }

    private WebApp.DefinitionStages.WithCreate applyDiagnosticConfig(WebApp.DefinitionStages.WithCreate withCreate,
                                                                     WebAppSettingModel settingModel) {
        final WebAppDiagnosticLogs.DefinitionStages.Blank diagnosticLogs = withCreate.defineDiagnosticLogsConfiguration();
        WebAppDiagnosticLogs.DefinitionStages.WithAttach withAttach = null;
        if (settingModel.isEnableApplicationLog()) {
            withAttach = diagnosticLogs.withApplicationLogging()
                                       .withLogLevel(LogLevel.fromString(settingModel.getApplicationLogLevel()))
                                       .withApplicationLogsStoredOnFileSystem();
        }
        if (settingModel.isEnableWebServerLogging()) {
            withAttach = diagnosticLogs.withWebServerLogging()
                                       .withWebServerLogsStoredOnFileSystem()
                                       .withWebServerFileSystemQuotaInMB(settingModel.getWebServerLogQuota())
                                       .withLogRetentionDays(settingModel.getWebServerRetentionPeriod())
                                       .withDetailedErrorMessages(settingModel.isEnableDetailedErrorMessage())
                                       .withFailedRequestTracing(settingModel.isEnableFailedRequestTracing());
        }
        return withAttach == null ? withCreate : (WebApp.DefinitionStages.WithCreate) withAttach.attach();
    }

    private AppServicePlan.DefinitionStages.WithCreate prepareWithCreate(@NotNull Azure azure, @NotNull WebAppSettingModel model) {
        final String[] tierSize = model.getPricing().split("_");
        if (tierSize.length != 2) {
            throw new IllegalArgumentException("invalid price tier");
        }
        final com.microsoft.azure.management.appservice.PricingTier pricingTier =
            new com.microsoft.azure.management.appservice.PricingTier(tierSize[0], tierSize[1]);

        final AppServicePlan.DefinitionStages.WithGroup withGroup = azure
            .appServices()
            .appServicePlans()
            .define(model.getAppServicePlanName())
            .withRegion(model.getRegion());

        final AppServicePlan.DefinitionStages.WithPricingTier withPricingTier;
        final String resourceGroup = model.getResourceGroup();
        if (model.isCreatingResGrp()) {
            withPricingTier = withGroup.withNewResourceGroup(resourceGroup);
        } else {
            withPricingTier = withGroup.withExistingResourceGroup(resourceGroup);
        }

        return withPricingTier.withPricingTier(pricingTier).withOperatingSystem(model.getOS());
    }

    private WebApp.DefinitionStages.WithNewAppServicePlan prepareServicePlan(
        @NotNull Azure azure, @NotNull WebAppSettingModel model) {

        final WebApp.DefinitionStages.NewAppServicePlanWithGroup appWithGroup = azure
            .webApps()
            .define(model.getWebAppName())
            .withRegion(model.getRegion());

        final String resourceGroup = model.getResourceGroup();
        if (model.isCreatingResGrp()) {
            return appWithGroup.withNewResourceGroup(resourceGroup);
        }
        return appWithGroup.withExistingResourceGroup(resourceGroup);
    }

    private WebApp.DefinitionStages.WithCreate withCreateNewWindowsServicePlan(
        @NotNull Azure azure, @NotNull WebAppSettingModel model) {
        final AppServicePlan.DefinitionStages.WithCreate withCreate = prepareWithCreate(azure, model);
        final WebApp.DefinitionStages.WithNewAppServicePlan withNewAppServicePlan = prepareServicePlan(azure, model);
        return withNewAppServicePlan.withNewWindowsPlan(withCreate);
    }

    private WebApp.DefinitionStages.WithDockerContainerImage withCreateNewLinuxServicePlan(
        @NotNull Azure azure, @NotNull WebAppSettingModel model) {
        final AppServicePlan.DefinitionStages.WithCreate withCreate = prepareWithCreate(azure, model);
        final WebApp.DefinitionStages.WithNewAppServicePlan withNewAppServicePlan = prepareServicePlan(azure, model);
        return withNewAppServicePlan.withNewLinuxPlan(withCreate);
    }

    private WebApp.DefinitionStages.WithCreate withExistingWindowsServicePlan(
        @NotNull Azure azure, @NotNull WebAppSettingModel model) {
        final AppServicePlan servicePlan = azure.appServices().appServicePlans().getById(model.getAppServicePlanId());
        final WebApp.DefinitionStages.ExistingWindowsPlanWithGroup withGroup = azure
            .webApps()
            .define(model.getWebAppName())
            .withExistingWindowsPlan(servicePlan);

        if (model.isCreatingResGrp()) {
            return withGroup.withNewResourceGroup(model.getResourceGroup());
        }
        return withGroup.withExistingResourceGroup(model.getResourceGroup());
    }

    private WebApp.DefinitionStages.WithDockerContainerImage withExistingLinuxServicePlan(
        @NotNull Azure azure, @NotNull WebAppSettingModel model) {

        final AppServicePlan servicePlan = azure.appServices().appServicePlans().getById(model.getAppServicePlanId());
        final WebApp.DefinitionStages.ExistingLinuxPlanWithGroup withGroup = azure
            .webApps()
            .define(model.getWebAppName())
            .withExistingLinuxPlan(servicePlan);

        if (model.isCreatingResGrp()) {
            return withGroup.withNewResourceGroup(model.getResourceGroup());
        }
        return withGroup.withExistingResourceGroup(model.getResourceGroup());
    }

    @CacheEvict(cacheName = CACHE_SUBSCRIPTION_WEBAPPS, key = "$sid")
    public void deleteWebApp(String sid, String appId) {
        com.microsoft.azure.toolkit.lib.Azure.az(AzureAppService.class).subscription(sid).webapp(appId).delete();
    }

    /**
     * API to create Web App on Docker.
     *
     * @param model parameters
     * @return instance of created WebApp
     * @throws IOException IOExceptions
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
    public WebApp createWebAppWithPrivateRegistryImage(@NotNull WebAppOnLinuxDeployModel model) {
        final PrivateRegistryImageSetting pr = model.getPrivateRegistryImageSetting();
        final WebApp app;
        final Azure azure = AuthMethodManager.getInstance().getAzureClient(model.getSubscriptionId());
        final com.microsoft.azure.management.appservice.PricingTier pricingTier =
            new com.microsoft.azure.management.appservice.PricingTier(model.getPricingSkuTier(), model.getPricingSkuSize());

        final WebApp.DefinitionStages.Blank webAppDefinition = azure.webApps().define(model.getWebAppName());
        if (model.isCreatingNewAppServicePlan()) {
            // new asp
            final AppServicePlan.DefinitionStages.WithCreate asp;
            final com.microsoft.azure.management.resources.fluentcore.arm.Region region =
                com.microsoft.azure.management.resources.fluentcore.arm.Region.findByLabelOrName(model.getLocationName());
            if (model.isCreatingNewResourceGroup()) {
                // new rg
                asp = azure.appServices().appServicePlans()
                        .define(model.getAppServicePlanName())
                        .withRegion(region)
                        .withNewResourceGroup(model.getResourceGroupName())
                        .withPricingTier(pricingTier)
                        .withOperatingSystem(OperatingSystem.LINUX);
                app = webAppDefinition
                        .withRegion(region)
                        .withNewResourceGroup(model.getResourceGroupName())
                        .withNewLinuxPlan(asp)
                        .withPrivateRegistryImage(pr.getImageTagWithServerUrl(), pr.getServerUrl())
                        .withCredentials(pr.getUsername(), pr.getPassword())
                        .withStartUpCommand(pr.getStartupFile()).create();
            } else {
                // old rg
                asp = azure.appServices().appServicePlans()
                        .define(model.getAppServicePlanName())
                        .withRegion(region)
                        .withExistingResourceGroup(model.getResourceGroupName())
                        .withPricingTier(pricingTier)
                        .withOperatingSystem(OperatingSystem.LINUX);
                app = webAppDefinition
                        .withRegion(region)
                        .withExistingResourceGroup(model.getResourceGroupName())
                        .withNewLinuxPlan(asp)
                        .withPrivateRegistryImage(pr.getImageTagWithServerUrl(), pr.getServerUrl())
                        .withCredentials(pr.getUsername(), pr.getPassword())
                        .withStartUpCommand(pr.getStartupFile()).create();
            }
        } else {
            // old asp
            final AppServicePlan asp = azure.appServices().appServicePlans().getById(model.getAppServicePlanId());
            if (model.isCreatingNewResourceGroup()) {
                // new rg
                app = webAppDefinition
                        .withExistingLinuxPlan(asp)
                        .withNewResourceGroup(model.getResourceGroupName())
                        .withPrivateRegistryImage(pr.getImageTagWithServerUrl(), pr.getServerUrl())
                        .withCredentials(pr.getUsername(), pr.getPassword())
                        .withStartUpCommand(pr.getStartupFile()).create();
            } else {
                // old rg
                app = webAppDefinition
                        .withExistingLinuxPlan(asp)
                        .withExistingResourceGroup(model.getResourceGroupName())
                        .withPrivateRegistryImage(pr.getImageTagWithServerUrl(), pr.getServerUrl())
                        .withCredentials(pr.getUsername(), pr.getPassword())
                        .withStartUpCommand(pr.getStartupFile()).create();
            }
        }
        return app;
        // TODO: update cache
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
     * @param sid          Subscription id
     * @param webAppId     id of Web App on Linux instance
     * @param imageSetting new container settings
     * @return instance of the updated Web App on Linux
     */
    @AzureOperation(
            name = "docker|image.update",
            params = {"nameFromResourceId(webAppId)", "imageSetting.getImageNameWithTag()"},
            type = AzureOperation.Type.SERVICE
    )
    public WebApp updateWebAppOnDocker(String sid, String webAppId, ImageSetting imageSetting) {
        final WebApp app = getWebAppById(sid, webAppId);
        clearTags(app);
        if (imageSetting instanceof PrivateRegistryImageSetting) {
            final PrivateRegistryImageSetting pr = (PrivateRegistryImageSetting) imageSetting;
            app.update().withPrivateRegistryImage(pr.getImageTagWithServerUrl(), pr.getServerUrl())
                    .withCredentials(pr.getUsername(), pr.getPassword())
                    .withStartUpCommand(pr.getStartupFile()).apply();
        } else {
            // TODO: other types of ImageSetting, e.g. Docker Hub
        }
        // status-free restart.
        stopWebApp(sid, webAppId);
        startWebApp(sid, webAppId);
        return app;
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
     * Update app settings of webapp.
     *
     * @param sid      subscription id
     * @param webAppId webapp id
     * @param toUpdate entries to add/modify
     * @param toRemove entries to remove
     */
    @AzureOperation(
        name = "webapp.update_settings",
        params = {"nameFromResourceId(webAppId)"},
        type = AzureOperation.Type.SERVICE
    )
    public void updateWebAppSettings(String sid, String webAppId, Map<String, String> toUpdate, Set<String> toRemove) {
        final WebApp app = getWebAppById(sid, webAppId);
        clearTags(app);
        com.microsoft.azure.management.appservice.WebAppBase.Update<WebApp> update = app.update()
                                                                                        .withAppSettings(toUpdate);
        for (final String key : toRemove) {
            update = update.withoutAppSetting(key);
        }
        update.apply();
    }

    /**
     * Update app settings of deployment slot.
     */
    @AzureOperation(
        name = "webapp|deployment.update_settings",
        params = {"slotName", "nameFromResourceId(webAppId)"},
        type = AzureOperation.Type.SERVICE
    )
    public void updateDeploymentSlotAppSettings(final String subsciptionId, final String webAppId,
                                                final String slotName, final Map<String, String> toUpdate,
                                                final Set<String> toRemove) {
        final DeploymentSlot slot = getWebAppById(subsciptionId, webAppId).deploymentSlots().getByName(slotName);
        clearTags(slot);
        com.microsoft.azure.management.appservice.WebAppBase.Update<DeploymentSlot> update = slot.update().withAppSettings(toUpdate);
        for (final String key : toRemove) {
            update = update.withoutAppSetting(key);
        }
        update.apply();
    }

    public void deleteWebAppOnLinux(String sid, String appid) {
        deleteWebApp(sid, appid);
    }

    public void restartWebApp(String sid, String appid) {
        AuthMethodManager.getInstance().getAzureClient(sid).webApps().getById(appid).restart();
    }

    public void startWebApp(String sid, String appid) {
        AuthMethodManager.getInstance().getAzureClient(sid).webApps().getById(appid).start();
    }

    public void stopWebApp(String sid, String appid) {
        AuthMethodManager.getInstance().getAzureClient(sid).webApps().getById(appid).stop();
    }

    @AzureOperation(
        name = "webapp|deployment.start",
        params = {"slotName", "nameFromResourceId(appId)"},
        type = AzureOperation.Type.SERVICE
    )
    public void startDeploymentSlot(final String subscriptionId, final String appId,
                                    final String slotName) {
        final WebApp app = AuthMethodManager.getInstance().getAzureClient(subscriptionId).webApps().getById(appId);
        app.deploymentSlots().getByName(slotName).start();
    }

    @AzureOperation(
        name = "webapp|deployment.stop",
        params = {"slotName", "nameFromResourceId(appId)"},
        type = AzureOperation.Type.SERVICE
    )
    public void stopDeploymentSlot(final String subscriptionId, final String appId,
                                   final String slotName) {
        final WebApp app = AuthMethodManager.getInstance().getAzureClient(subscriptionId).webApps().getById(appId);
        app.deploymentSlots().getByName(slotName).stop();
    }

    @AzureOperation(
        name = "webapp|deployment.restart",
        params = {"slotName", "nameFromResourceId(appId)"},
        type = AzureOperation.Type.SERVICE
    )
    public void restartDeploymentSlot(final String subscriptionId, final String appId,
                                      final String slotName) {
        final WebApp app = AuthMethodManager.getInstance().getAzureClient(subscriptionId).webApps().getById(appId);
        app.deploymentSlots().getByName(slotName).restart();
    }

    @AzureOperation(
        name = "webapp|deployment.swap",
        params = {"slotName", "nameFromResourceId(appId)"},
        type = AzureOperation.Type.SERVICE
    )
    public void swapSlotWithProduction(final String subscriptionId, final String appId,
                                       final String slotName) {
        final WebApp app = AuthMethodManager.getInstance().getAzureClient(subscriptionId).webApps().getById(appId);
        final DeploymentSlot slot = app.deploymentSlots().getByName(slotName);
        slot.swap("production");
    }

    @AzureOperation(name = "webapp|deployment.delete", params = {"slotName", "nameFromResourceId(appId)"}, type = AzureOperation.Type.SERVICE)
    public void deleteDeploymentSlotNode(final String subscriptionId, final String appId,
                                         final String slotName) {
        final WebApp app = AuthMethodManager.getInstance().getAzureClient(subscriptionId).webApps().getById(appId);
        app.deploymentSlots().deleteByName(slotName);
    }

    /**
     * Get all the deployment slots of a web app by the subscription id and web app id.
     */
    @AzureOperation(name = "webapp|deployment.list", params = {"nameFromResourceId(appId)"}, type = AzureOperation.Type.SERVICE)
    public @Nullable List<DeploymentSlot> getDeploymentSlots(final String subscriptionId, final String appId) {
        final AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        if (azureManager == null) {
            return null;
        }
        final WebApp webApp = AuthMethodManager.getInstance().getAzureClient(subscriptionId).webApps().getById(appId);
        if (webApp == null) {
            return null;
        }
        return new ArrayList<>(webApp.deploymentSlots().list());
    }

    /**
     * List app service plan by subscription id and resource group name.
     */
    @AzureOperation(
        name = "appservice|plan.list.subscription|rg",
        params = {"group", "sid"},
        type = AzureOperation.Type.SERVICE
    )
    public List<AppServicePlan> listAppServicePlanBySubscriptionIdAndResourceGroupName(String sid, String group) {
        final Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        return new ArrayList<>(azure.appServices().appServicePlans().listByResourceGroup(group));
    }

    /**
     * List app service plan by subscription id.
     */
    @AzureOperation(
        name = "appservice|plan.list.subscription",
        params = {"sid"},
        type = AzureOperation.Type.SERVICE
    )
    public List<AppServicePlan> listAppServicePlanBySubscriptionId(String sid) {
        return AuthMethodManager.getInstance().getAzureClient(sid).appServices().appServicePlans().list(true);
    }

    /**
     * List Web Apps by Subscription ID.
     */
    @Deprecated
    public List<ResourceEx<WebApp>> listWebAppsOnWindowsBySubscriptionId(final String sid, final boolean force) {
        return this.listWebAppsOnWindows(sid, force);
    }

    /**
     * List all the Web Apps on Windows in selected subscriptions.
     */
    @AzureOperation(
        name = "webapp.list.windows|subscription|selected",
        type = AzureOperation.Type.SERVICE
    )
    public List<ResourceEx<WebApp>> listAllWebAppsOnWindows(final boolean force) {
        final List<ResourceEx<WebApp>> webApps = new ArrayList<>();
        for (final Subscription sub : az(AzureAccount.class).account().getSelectedSubscriptions()) {
            final String sid = sub.getId();
            webApps.addAll(listWebAppsOnWindows(sid, force));
        }
        return webApps;
    }

    /**
     * List Web App on Linux by Subscription ID.
     *
     * @param sid   subscription Id
     * @param force flag indicating whether force to fetch most updated data from server
     * @return list of Web App on Linux
     */
    @Deprecated
    public List<ResourceEx<WebApp>> listWebAppsOnLinuxBySubscriptionId(final String sid, final boolean force) {
        return this.listWebAppsOnLinux(sid, force);
    }

    /**
     * List all the Web Apps in selected subscriptions.
     *
     * @param force flag indicating whether force to fetch most updated data from server
     * @return list of Web App
     */
    @AzureOperation(
        name = "webapp.list.subscription|selected",
        type = AzureOperation.Type.SERVICE
    )
    public List<ResourceEx<WebApp>> listAllWebApps(final boolean... force) {
        return az(AzureAccount.class).account().getSelectedSubscriptions().parallelStream()
            .flatMap((sd) -> listWebApps(sd.getId(), force).stream())
            .collect(Collectors.toList());
    }

    @NotNull
    @AzureOperation(
        name = "webapp.list.java|subscription|selected",
        type = AzureOperation.Type.SERVICE
    )
    @Preload
    public List<ResourceEx<WebApp>> listJavaWebApps(final boolean... force) {
        return this.listAllWebApps(force).parallelStream()
            .filter(app -> WebAppUtils.isJavaWebApp(app.getResource()))
            .collect(Collectors.toList());
    }

    /**
     * List web apps on linux by subscription id.
     */
    @AzureOperation(
        name = "webapp.list.linux|subscription",
        params = {"subscriptionId"},
        type = AzureOperation.Type.SERVICE
    )
    public List<ResourceEx<WebApp>> listWebAppsOnLinux(@NotNull final String subscriptionId, final boolean force) {
        return listWebApps(subscriptionId, force)
            .stream()
            .filter(resourceEx -> OperatingSystem.LINUX == resourceEx.getResource().operatingSystem())
            .collect(Collectors.toList());
    }

    /**
     * List web apps on windows by subscription id.
     */
    @AzureOperation(
        name = "webapp.list.windows|subscription",
        params = {"subscriptionId"},
        type = AzureOperation.Type.SERVICE
    )
    public List<ResourceEx<WebApp>> listWebAppsOnWindows(@NotNull final String subscriptionId, final boolean force) {
        return listWebApps(subscriptionId, force)
            .stream()
            .filter(resourceEx -> OperatingSystem.WINDOWS == (resourceEx.getResource().operatingSystem()))
            .collect(Collectors.toList());
    }

    /**
     * List all web apps by subscription id.
     */
    @NotNull
    @AzureOperation(
        name = "webapp.list.subscription",
        params = {"subscriptionId"},
        type = AzureOperation.Type.SERVICE
    )
    @Cacheable(cacheName = CACHE_SUBSCRIPTION_WEBAPPS, key = "$subscriptionId", condition = "!(force&&force[0])")
    public List<ResourceEx<WebApp>> listWebApps(final String subscriptionId, final boolean... force) {
        final Azure azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId);
        final Predicate<SiteInner> filter = inner -> inner.kind() == null || !Arrays.asList(inner.kind().split(",")).contains("functionapp");
        return azure.appServices().webApps().inner().list().stream().parallel().filter(filter)
                  .map(inner -> new WebAppWrapper(subscriptionId, inner))
                  .map(app -> new ResourceEx<WebApp>(app, subscriptionId))
                  .collect(Collectors.toList());
    }

    /**
     * List available web containers for jar files.
     */
    public static List<WebAppUtils.WebContainerMod> listWebContainersForJarFile(JdkModel jdkModel) {
        if (jdkModel == null || jdkModel.getJavaVersion() == null) {
            return Collections.emptyList();
        }
        final String javaVersion = jdkModel.getJavaVersion().toString();
        if (javaVersion.startsWith("1.8")) {
            return JAVA_8_JAR_CONTAINERS;
        } else if (javaVersion.startsWith("11")) {
            return JAVA_11_JAR_CONTAINERS;
        }
        return Collections.emptyList();
    }

    /**
     * List available web containers for war files.
     */
    public static List<WebAppUtils.WebContainerMod> listWebContainersForWarFile() {
        return Arrays.asList(
            WebAppUtils.WebContainerMod.Newest_Jetty_91,
            WebAppUtils.WebContainerMod.Newest_Jetty_93,
            WebAppUtils.WebContainerMod.Newest_Tomcat_70,
            WebAppUtils.WebContainerMod.Newest_Tomcat_80,
            WebAppUtils.WebContainerMod.Newest_Tomcat_85,
            WebAppUtils.WebContainerMod.Newest_Tomcat_90
                            );
    }

    @AzureOperation(
        name = "webapp.list_containers",
        type = AzureOperation.Type.TASK
    )
    public List<WebAppUtils.WebContainerMod> listWebContainers() {
        final List<WebAppUtils.WebContainerMod> webContainers = new ArrayList<>();
        Collections.addAll(webContainers, WebAppUtils.WebContainerMod.values());
        return webContainers;
    }

    /**
     * List available Third Party JDKs.
     */
    @AzureOperation(
        name = "webapp.list_jdks",
        type = AzureOperation.Type.TASK
    )
    public List<JdkModel> listJdks() {
        final List<JdkModel> jdkModels = new ArrayList<>();
        Collections.addAll(jdkModels, JdkModel.values());
        return jdkModels;
    }

    /**
     * List all available Java linux RuntimeStacks.
     * todo: For those unchanged list, like jdk versions, web containers,
     * linux runtimes, do we really need to get the values from Mvp model every time?
     */
    @AzureOperation(
        name = "webapp.list_linux_runtime",
        type = AzureOperation.Type.TASK
    )
    public List<RuntimeStack> getLinuxRuntimes() {
        return WebAppUtils.getAllJavaLinuxRuntimeStacks();
    }

    /**
     * List all the Web Apps on Linux in selected subscriptions.
     *
     * @param force flag indicating whether force to fetch most updated data from server
     * @return list of Web App on Linux
     */
    @AzureOperation(
        name = "webapp.list.linux|subscription|selected",
        type = AzureOperation.Type.SERVICE
    )
    public List<ResourceEx<WebApp>> listAllWebAppsOnLinux(final boolean force) {
        final List<ResourceEx<WebApp>> webApps = new ArrayList<>();
        for (final Subscription sub : az(AzureAccount.class).account().getSelectedSubscriptions()) {
            final String sid = sub.getId();
            webApps.addAll(listWebAppsOnLinux(sid, force));
        }
        return webApps;
    }

    /**
     * Download publish profile of web app.
     *
     * @param sid      subscription id
     * @param webAppId webapp id
     * @param filePath file path to save publish profile
     * @return status indicating whether it is successful or not
     */
    @AzureOperation(
        name = "webapp.get_publishing_profile",
        params = {"nameFromResourceId(webAppId)"},
        type = AzureOperation.Type.SERVICE
    )
    public boolean getPublishingProfileXmlWithSecrets(String sid, String webAppId, String filePath) {
        final WebApp app = getWebAppById(sid, webAppId);
        return AppServiceUtils.getPublishingProfileXmlWithSecrets(app, filePath);
    }

    /**
     * Download publish profile of deployment slot.
     */
    @AzureOperation(
        name = "webapp|deployment.get_publishing_profile",
        params = {"slotName", "nameFromResourceId(webAppId)"},
        type = AzureOperation.Type.SERVICE
    )
    public boolean getSlotPublishingProfileXmlWithSecrets(final String sid,
                                                          final String webAppId,
                                                          final String slotName,
                                                          final String filePath) {
        final WebApp app = getWebAppById(sid, webAppId);
        final DeploymentSlot slot = app.deploymentSlots().getByName(slotName);
        final String fileName = slotName + "_" + System.currentTimeMillis() + ".PublishSettings";
        final Path path = Paths.get(filePath, fileName);
        final File file = new File(path.toString());
        try {
            file.createNewFile();
        } catch (final IOException e) {
            log.warning("failed to create publishing profile xml file");
            return false;
        }
        try (final InputStream inputStream = slot.manager().inner().webApps()
                                                 .listPublishingProfileXmlWithSecretsSlot(slot.resourceGroupName(),
                                                                                          app.name(),
                                                                                          slotName,
                                                                                          new CsmPublishingProfileOptions()
                                                                                              .withFormat(
                                                                                                  PublishingProfileFormat.FTP));
             final OutputStream outputStream = new FileOutputStream(file)
        ) {
            IOUtils.copy(inputStream, outputStream);
            return true;
        } catch (final IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        }
    }

    // Refers https://github.com/microsoft/vscode-azureappservice/blob/v0.16.5/src/explorer/SiteTreeItem.ts#L133
    public static boolean isHttpLogEnabled(WebAppBase webAppBase) {
        final WebAppDiagnosticLogs config = webAppBase.diagnosticLogsConfig();
        return config != null && config.inner() != null && config.inner().httpLogs() != null &&
            config.inner().httpLogs().fileSystem() != null && config.inner().httpLogs().fileSystem().enabled();
    }

    public static void enableHttpLog(WebAppBase.Update webApp) {
        webApp.withContainerLoggingEnabled().apply();
    }

    @AzureOperation(
        name = "common|region.list.subscription|tier",
        params = {"pricingTier", "subscriptionId"},
        type = AzureOperation.Type.SERVICE
    )
    public List<Region> getAvailableRegions(String subscriptionId, PricingTier pricingTier) {
        if (StringUtils.isEmpty(subscriptionId) || pricingTier == null) {
            return Collections.emptyList();
        }
        final SkuName skuName = SkuName.fromString(pricingTier.getTier());
        final List<GeoRegionInner> geoRegionInnerList = AuthMethodManager.getInstance()
                                                                         .getAzureClient(subscriptionId)
                                                                         .appServices()
                                                                         .inner()
                                                                         .listGeoRegions(skuName, false, false, false);
        return geoRegionInnerList.stream()
                                 .map(regionInner -> Region.fromName(regionInner.displayName()))
                                 .collect(Collectors.toList());
    }

    /**
     * List all the Web Apps in selected subscriptions.
     * todo: move to app service library
     */
    @AzureOperation(
            name = "webapp.list.subscription|selected",
            type = AzureOperation.Type.SERVICE
    )
    public List<IWebApp> listAzureWebApps(final boolean force) {
        return az(AzureAccount.class).account().getSelectedSubscriptions().stream()
                .flatMap(sub -> listAzureWebAppsBySubscription(sub.getId(), force).stream())
                .collect(Collectors.toList());
    }

    @NotNull
    @AzureOperation(
            name = "webapp.list.subscription",
            params = {"subscriptionId"},
            type = AzureOperation.Type.SERVICE
    )
    @Cacheable(cacheName = "subscription-webapps-track2", key = "$subscriptionId", condition = "!(force&&force[0])")
    public List<IWebApp> listAzureWebAppsBySubscription(final String subscriptionId, final boolean... force) {
        return com.microsoft.azure.toolkit.lib.Azure.az(AzureAppService.class).subscription(subscriptionId).webapps();
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
        if (isDeployToRoot || deployTarget.getRuntime().getWebContainer() == com.microsoft.azure.toolkit.lib.appservice.model.WebContainer.JAVA_SE) {
            deployTarget.deploy(deployType, file);
        } else {
            final String webappPath = String.format("webapps/%s", FilenameUtils.getBaseName(file.getName()).replaceAll("#", StringUtils.EMPTY));
            deployTarget.deploy(deployType, file, webappPath);
        }

        String successMessage = deployTarget instanceof IWebApp ? DEPLOY_SUCCESS_WEB_APP : DEPLOY_SUCCESS_DEPLOYMENT_SLOT;
        progressIndicator.setText(successMessage);
        deployTarget.start();
    }

    private static DeployType getDeployTypeByWebContainer(com.microsoft.azure.toolkit.lib.appservice.model.WebContainer webContainer) {
        if (webContainer == com.microsoft.azure.toolkit.lib.appservice.model.WebContainer.JAVA_SE) {
            return DeployType.JAR;
        }
        if (webContainer == com.microsoft.azure.toolkit.lib.appservice.model.WebContainer.JBOSS_72) {
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

    /**
     * Work Around:
     * When a web app is created from Azure Portal, there are hidden tags associated with the app.
     * It will be messed up when calling "update" API.
     * An issue is logged at https://github.com/Azure/azure-libraries-for-java/issues/508 .
     * Remove all tags here to make it work.
     */
    private void clearTags(@NotNull final WebAppBase app) {
        app.inner().withTags(null);
    }

    private static final class SingletonHolder {
        private static final AzureWebAppMvpModel INSTANCE = new AzureWebAppMvpModel();
    }
}
