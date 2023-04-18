/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.appservice.model.MonitorConfig;
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.intellij.container.model.DockerHost;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.container.model.DockerPushConfiguration;
import com.microsoft.azure.toolkit.intellij.containerregistry.buildimage.IDockerConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunConfigurationBase;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.LogLevel;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig;
import com.microsoft.azuretools.core.mvp.model.container.pojo.DockerHostRunSetting;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppOnLinuxDeployModel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Optional;

public class WebAppOnLinuxDeployConfiguration extends AzureRunConfigurationBase<WebAppOnLinuxDeployModel> implements IDockerConfiguration {

    private static final String MISSING_SERVER_URL = "Please specify a valid Server URL.";
    private static final String MISSING_USERNAME = "Please specify Username.";
    private static final String MISSING_PASSWORD = "Please specify Password.";
    private static final String MISSING_IMAGE_WITH_TAG = "Please specify Image and Tag.";
    private static final String MISSING_WEB_APP = "Please specify Web App for Containers.";
    private static final String MISSING_SUBSCRIPTION = "Please specify Subscription.";
    private static final String MISSING_RESOURCE_GROUP = "Please specify Resource Group.";
    private static final String MISSING_APP_SERVICE_PLAN = "Please specify App Service Plan.";
    private static final String INVALID_IMAGE_WITH_TAG = "Image and Tag name is invalid";
    private static final String INVALID_DOCKER_FILE = "Please specify a valid docker file.";

    // TODO: move to util
    private static final String MISSING_ARTIFACT = "A web archive (.war|.jar) artifact has not been configured.";
    private static final String INVALID_WAR_FILE = "The artifact name %s is invalid. "
            + "An artifact name may contain only the ASCII letters 'a' through 'z' (case-insensitive), "
            + "and the digits '0' through '9', '.', '-' and '_'.";
    private static final String CANNOT_END_WITH_COLON = "Image and tag name cannot end with ':'";
    private static final String REPO_LENGTH_INVALID = "The length of repository name must be at least one character "
            + "and less than 256 characters";
    private static final String CANNOT_END_WITH_SLASH = "The repository name should not end with '/'";
    private static final String REPO_COMPONENT_INVALID = "Invalid repository component: %s, should follow: %s";
    private static final String TAG_LENGTH_INVALID = "The length of tag name must be no more than 128 characters";
    private static final String TAG_INVALID = "Invalid tag: %s, should follow: %s";
    private static final String ARTIFACT_NAME_REGEX = "^[.A-Za-z0-9_-]+\\.(war|jar)$";
    private static final String DOMAIN_NAME_REGEX = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$";
    private static final String REPO_COMPONENTS_REGEX = "[a-z0-9]+(?:[._-][a-z0-9]+)*";
    private static final String TAG_REGEX = "^[\\w]+[\\w.-]*$";
    private static final int TAG_LENGTH = 128;
    private static final int REPO_LENGTH = 255;

    private final WebAppOnLinuxDeployModel deployModel;

    protected WebAppOnLinuxDeployConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String
            name) {
        super(project, factory, name);
        deployModel = new WebAppOnLinuxDeployModel();
    }

    @Override
    public WebAppOnLinuxDeployModel getModel() {
        return this.deployModel;
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new WebAppOnLinuxDeploySettingsEditor(this.getProject(), this);
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) {
        return new WebAppOnLinuxDeployState(getProject(), this);
    }

    /**
     * Configuration value Validation.
     */
    @Override
    public void validate() throws ConfigurationException {
        checkAzurePreconditions();
        // web app
        if (deployModel.isCreatingNewWebAppOnLinux()) {
            if (StringUtils.isEmpty(deployModel.getWebAppName())) {
                throw new ConfigurationException(MISSING_WEB_APP);
            }
            if (StringUtils.isEmpty(deployModel.getSubscriptionId())) {
                throw new ConfigurationException(MISSING_SUBSCRIPTION);
            }
            if (StringUtils.isEmpty(deployModel.getResourceGroupName())) {
                throw new ConfigurationException(MISSING_RESOURCE_GROUP);
            }
            if (StringUtils.isEmpty(deployModel.getAppServicePlanName())) {
                throw new ConfigurationException(MISSING_APP_SERVICE_PLAN);
            }
        } else {
            if (StringUtils.isEmpty(deployModel.getWebAppId())) {
                throw new ConfigurationException(MISSING_WEB_APP);
            }
        }
    }

    public String getAppName() {
        return deployModel.getWebAppName();
    }

    public void setAppName(String appName) {
        deployModel.setWebAppName(appName);
    }

    @Override
    public String getSubscriptionId() {
        return deployModel.getSubscriptionId();
    }

    public void setSubscriptionId(String subscriptionId) {
        deployModel.setSubscriptionId(subscriptionId);
    }

    public boolean isCreatingNewResourceGroup() {
        return deployModel.isCreatingNewResourceGroup();
    }

    public void setCreatingNewResourceGroup(boolean creatingNewResourceGroup) {
        deployModel.setCreatingNewResourceGroup(creatingNewResourceGroup);
    }

    public String getResourceGroupName() {
        return deployModel.getResourceGroupName();
    }

    public void setResourceGroupName(String resourceGroupName) {
        deployModel.setResourceGroupName(resourceGroupName);
    }

    public String getLocationName() {
        return deployModel.getLocationName();
    }

    public void setLocationName(String locationName) {
        deployModel.setLocationName(locationName);
    }

    public String getPricingSkuTier() {
        return deployModel.getPricingSkuTier();
    }

    public void setPricingSkuTier(String pricingSkuTier) {
        deployModel.setPricingSkuTier(pricingSkuTier);
    }

    public String getPricingSkuSize() {
        return deployModel.getPricingSkuSize();
    }

    public void setPricingSkuSize(String pricingSkuSize) {
        deployModel.setPricingSkuSize(pricingSkuSize);
    }

    public PrivateRegistryImageSetting getPrivateRegistryImageSetting() {
        return deployModel.getPrivateRegistryImageSetting();
    }

    public void setPrivateRegistryImageSetting(PrivateRegistryImageSetting privateRegistryImageSetting) {
        deployModel.setPrivateRegistryImageSetting(privateRegistryImageSetting);
    }

    public String getWebAppId() {
        return deployModel.getWebAppId();
    }

    public void setWebAppId(String webAppId) {
        deployModel.setWebAppId(webAppId);
    }

    public boolean isCreatingNewWebAppOnLinux() {
        return deployModel.isCreatingNewWebAppOnLinux();
    }

    public void setCreatingNewWebAppOnLinux(boolean creatingNewWebAppOnLinux) {
        deployModel.setCreatingNewWebAppOnLinux(creatingNewWebAppOnLinux);
    }

    public boolean isCreatingNewAppServicePlan() {
        return deployModel.isCreatingNewAppServicePlan();
    }

    public void setCreatingNewAppServicePlan(boolean creatingNewAppServicePlan) {
        deployModel.setCreatingNewAppServicePlan(creatingNewAppServicePlan);
    }

    public String getAppServicePlanName() {
        return deployModel.getAppServicePlanName();
    }

    public void setAppServicePlanName(String appServicePlanName) {
        deployModel.setAppServicePlanName(appServicePlanName);
    }

    public void setAppServicePlanResourceGroupName(String rgName) {
        deployModel.setAppServicePlanResourceGroupName(rgName);
    }

    public String getAppServicePlanResourceGroupName() {
        return deployModel.getAppServicePlanResourceGroupName();
    }

    @Override
    public String getTargetPath() {
        return deployModel.getTargetPath();
    }

    public void setTargetPath(String targetPath) {
        deployModel.setTargetPath(targetPath);
    }

    @Override
    public String getTargetName() {
        return deployModel.getTargetName();
    }

    public void setTargetName(String targetName) {
        deployModel.setTargetName(targetName);
    }

    public String getDockerFilePath() {
        return deployModel.getDockerFilePath();
    }

    public void setDockerFilePath(String dockerFilePath) {
        deployModel.setDockerFilePath(dockerFilePath);
    }

    public void setDockerImage(@Nullable DockerImage image) {
        final DockerHostRunSetting dockerHostRunSetting = Optional.ofNullable(getDockerHostRunSetting()).orElseGet(DockerHostRunSetting::new);
        dockerHostRunSetting.setImageName(Optional.ofNullable(image).map(DockerImage::getRepositoryName).orElse(null));
        dockerHostRunSetting.setTagName(Optional.ofNullable(image).map(DockerImage::getTagName).orElse(null));
        dockerHostRunSetting.setDockerFilePath(Optional.ofNullable(image).map(DockerImage::getDockerFile).map(File::getAbsolutePath).orElse(null));
        this.getModel().setDockerHostRunSetting(dockerHostRunSetting);
    }

    public void setHost(@Nullable DockerHost host) {
        final DockerHostRunSetting dockerHostRunSetting = Optional.ofNullable(getDockerHostRunSetting()).orElseGet(DockerHostRunSetting::new);
        dockerHostRunSetting.setDockerHost(Optional.ofNullable(host).map(DockerHost::getDockerHost).orElse(null));
        dockerHostRunSetting.setDockerCertPath(Optional.ofNullable(host).map(DockerHost::getDockerCertPath).orElse(null));
        dockerHostRunSetting.setTlsEnabled(Optional.ofNullable(host).map(DockerHost::isTlsEnabled).orElse(false));
        this.getModel().setDockerHostRunSetting(dockerHostRunSetting);
    }

    public DockerImage getDockerImageConfiguration() {
        final DockerImage image = new DockerImage();
        final DockerHostRunSetting dockerHostRunSetting = getDockerHostRunSetting();
        if (dockerHostRunSetting == null || StringUtils.isAllBlank(dockerHostRunSetting.getImageName(), dockerHostRunSetting.getDockerFilePath())) {
            return null;
        }
        image.setRepositoryName(dockerHostRunSetting.getImageName());
        image.setTagName(dockerHostRunSetting.getTagName());
        image.setDockerFile(Optional.ofNullable(dockerHostRunSetting.getDockerFilePath()).map(File::new).orElse(null));
        image.setDraft(StringUtils.isNoneBlank(dockerHostRunSetting.getDockerFilePath()));
        return image;
    }

    @javax.annotation.Nullable
    @Override
    public DockerHost getDockerHostConfiguration() {
        final DockerHostRunSetting dockerHostRunSetting = getDockerHostRunSetting();
        if (dockerHostRunSetting == null || StringUtils.isEmpty(dockerHostRunSetting.getDockerHost())) {
            return null;
        }
        return new DockerHost(dockerHostRunSetting.getDockerHost(), dockerHostRunSetting.getDockerCertPath());
    }

    @Nullable
    public DockerHostRunSetting getDockerHostRunSetting() {
        return getModel().getDockerHostRunSetting();
    }

    public void setContainerRegistry(@Nullable final ContainerRegistry containerRegistry) {
        getModel().setContainerRegistryId(Optional.ofNullable(containerRegistry).map(ContainerRegistry::getId).orElse(null));
    }

    public String getContainerRegistryId() {
        return getModel().getContainerRegistryId();
    }

    public WebAppConfig getWebAppConfig() {
        final Subscription subscription = new Subscription(this.getSubscriptionId());
        final Region region = StringUtils.isEmpty(this.getLocationName()) ? null : Region.fromName(this.getLocationName());
        final String rgName = this.getResourceGroupName();
        final ResourceGroupConfig resourceGroup = ResourceGroupConfig.builder().subscriptionId(subscription.getId()).name(rgName).region(region).build();
        final PricingTier pricingTier = StringUtils.isAnyEmpty(this.getPricingSkuTier(), this.getPricingSkuSize()) ? null :
                PricingTier.fromString(this.getPricingSkuTier(), this.getPricingSkuSize());
        final AppServicePlanConfig plan = AppServicePlanConfig.builder().subscriptionId(subscription.getId())
                .name(this.getAppServicePlanName()).resourceGroupName(rgName).region(region).os(OperatingSystem.LINUX).pricingTier(pricingTier).build();
        final DiagnosticConfig diagnosticConfig = DiagnosticConfig.builder()
                .enableApplicationLog(this.getModel().isEnableApplicationLog())
                .applicationLogLevel(LogLevel.fromString(this.getModel().getApplicationLogLevel()))
                .enableDetailedErrorMessage(this.getModel().isEnableDetailedErrorMessage())
                .enableFailedRequestTracing(this.getModel().isEnableFailedRequestTracing())
                .enableWebServerLogging(this.getModel().isEnableWebServerLogging())
                .webServerRetentionPeriod(this.getModel().getWebServerRetentionPeriod())
                .webServerLogQuota(this.getModel().getWebServerLogQuota()).build();
        final MonitorConfig monitorConfig = MonitorConfig.builder().diagnosticConfig(diagnosticConfig).build();
        final WebAppConfig.WebAppConfigBuilder<?, ?> configBuilder = WebAppConfig.builder().name(this.getAppName())
                .resourceId(this.getWebAppId())
                .subscription(subscription)
                .resourceGroup(resourceGroup)
                .runtime(Runtime.DOCKER)
                .servicePlan(plan);
        return !this.isCreatingNewWebAppOnLinux() ? configBuilder.build() : configBuilder.region(region).pricingTier(pricingTier)
                .monitorConfig(monitorConfig).build();
    }

    public void setWebAppConfig(@Nonnull final WebAppConfig webAppConfig) {
        this.setWebAppId(webAppConfig.getResourceId());
        this.setSubscriptionId(webAppConfig.getSubscriptionId());
        this.setResourceGroupName(webAppConfig.getResourceGroupName());
        this.setAppName(webAppConfig.getName());
        this.setCreatingNewWebAppOnLinux(StringUtils.isEmpty(webAppConfig.getResourceId()));
        if (this.isCreatingNewWebAppOnLinux()) {
            this.setLocationName(webAppConfig.getRegion().getName());
            this.setCreatingNewAppServicePlan(webAppConfig.getServicePlan().toResource().isDraftForCreating());
            this.setPricingSkuTier(Optional.ofNullable(webAppConfig.getServicePlan())
                    .map(AppServicePlanConfig::getPricingTier).map(PricingTier::getTier).orElse(null));
            this.setPricingSkuSize(Optional.ofNullable(webAppConfig.getServicePlan())
                    .map(AppServicePlanConfig::getPricingTier).map(PricingTier::getSize).orElse(null));
            this.setAppServicePlanName(webAppConfig.getServicePlan().getName());
            this.setAppServicePlanResourceGroupName(webAppConfig.getServicePlan().getResourceGroupName());
            Optional.ofNullable(webAppConfig.getMonitorConfig()).map(MonitorConfig::getDiagnosticConfig).ifPresent(diagnosticConfig -> {
                this.getModel().setEnableApplicationLog(diagnosticConfig.isEnableApplicationLog());
                this.getModel().setApplicationLogLevel(diagnosticConfig.getApplicationLogLevel().getValue());
                this.getModel().setEnableWebServerLogging(diagnosticConfig.isEnableWebServerLogging());
                this.getModel().setWebServerLogQuota(diagnosticConfig.getWebServerLogQuota());
                this.getModel().setWebServerRetentionPeriod(diagnosticConfig.getWebServerRetentionPeriod());
                this.getModel().setEnableDetailedErrorMessage(diagnosticConfig.isEnableDetailedErrorMessage());
                this.getModel().setEnableFailedRequestTracing(diagnosticConfig.isEnableFailedRequestTracing());
            });
        } else {
            this.setCreatingNewAppServicePlan(false);
            this.setAppServicePlanName(Optional.ofNullable(webAppConfig.getServicePlan())
                    .map(AppServicePlanConfig::getName).orElse(null));
            this.setAppServicePlanResourceGroupName(Optional.ofNullable(webAppConfig.getServicePlan())
                    .map(AppServicePlanConfig::getResourceGroupName).orElse(null));
        }
    }

    public DockerPushConfiguration getDockerPushConfiguration() {
        final DockerPushConfiguration dockerPushConfiguration = new DockerPushConfiguration();
        dockerPushConfiguration.setContainerRegistryId(this.getContainerRegistryId());
        dockerPushConfiguration.setDockerHost(this.getDockerHostConfiguration());
        dockerPushConfiguration.setDockerImage(this.getDockerImageConfiguration());
        return dockerPushConfiguration;
    }

    public void setDockerPushConfiguration(@Nonnull final DockerPushConfiguration configuration) {
        this.setHost(configuration.getDockerHost());
        this.setDockerImage(configuration.getDockerImage());
        this.getModel().setContainerRegistryId(configuration.getContainerRegistryId());
    }
}
