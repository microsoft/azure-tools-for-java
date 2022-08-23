/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactType;
import com.microsoft.azure.toolkit.intellij.common.runconfig.IWebAppRunConfiguration;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.IConnectionAware;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunConfigurationBase;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.Constants;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class WebAppConfiguration extends AzureRunConfigurationBase<IntelliJWebAppSettingModel>
        implements IWebAppRunConfiguration, IConnectionAware {

    // const string
    private static final String SLOT_NAME_REGEX = "[a-zA-Z0-9-]{1,60}";
    private static final String TOMCAT = "tomcat";
    private static final String JAVA = "java";
    private static final String JBOSS = "jboss";
    public static final String JAVA_VERSION = "javaVersion";
    private final IntelliJWebAppSettingModel webAppSettingModel;
    @Getter
    @Setter
    private Connection<?, ?> connection;

    private String appSettingsKey;

    public WebAppConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        this.webAppSettingModel = new IntelliJWebAppSettingModel();
    }

    @Override
    public IntelliJWebAppSettingModel getModel() {
        return this.webAppSettingModel;
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new WebAppSettingEditor(getProject(), this);
    }

    @Override
    public void setApplicationSettings(Map<String, String> env) {
        webAppSettingModel.setAppSettings(env);
    }

    @Override
    public Map<String, String> getApplicationSettings() {
        return webAppSettingModel.getAppSettings();
    }

    public String getAppSettingsKey() {
        return webAppSettingModel.getAppSettingsKey();
    }

    public void setAppSettingKey(final String appSettingsKey) {
        this.webAppSettingModel.setAppSettingsKey(appSettingsKey);
    }

    public Module getModule() {
        final AzureArtifact azureArtifact = AzureArtifactManager.getInstance(this.getProject())
                .getAzureArtifactById(this.getAzureArtifactType(), this.getArtifactIdentifier());
        return AzureArtifactManager.getInstance(this.getProject()).getModuleFromAzureArtifact(azureArtifact);
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment)
            throws ExecutionException {
        return new WebAppRunState(getProject(), this);
    }

    @Override
    public void readExternal(final Element element) throws InvalidDataException {
        super.readExternal(element);
        Optional.ofNullable(element.getChild(JAVA_VERSION))
                .map(javaVersionElement -> javaVersionElement.getAttributeValue(JAVA_VERSION))
                .ifPresent(webAppSettingModel::setWebAppJavaVersion);
    }

    @Override
    public void validate() throws ConfigurationException {
        if (webAppSettingModel.isCreatingNew()) {
            if (StringUtils.isEmpty(webAppSettingModel.getWebAppName())) {
                throw new ConfigurationException(message("webapp.deploy.validate.noWebAppName"));
            }
            if (StringUtils.isEmpty(webAppSettingModel.getSubscriptionId())) {
                throw new ConfigurationException(message("webapp.deploy.validate.noSubscription"));
            }
            if (StringUtils.isEmpty(webAppSettingModel.getResourceGroup())) {
                throw new ConfigurationException(message("webapp.deploy.validate.noResourceGroup"));
            }
            if (webAppSettingModel.isCreatingAppServicePlan()) {
                if (StringUtils.isEmpty(webAppSettingModel.getRegion())) {
                    throw new ConfigurationException(message("webapp.deploy.validate.noLocation"));
                }
                if (StringUtils.isEmpty(webAppSettingModel.getPricing())) {
                    throw new ConfigurationException(message("webapp.deploy.validate.noPricingTier"));
                }
            }
            if (StringUtils.isEmpty(webAppSettingModel.getAppServicePlanName())) {
                throw new ConfigurationException(message("webapp.deploy.validate.noAppServicePlan"));
            }
        } else {
            if (StringUtils.isEmpty(webAppSettingModel.getWebAppId())) {
                throw new ConfigurationException(message("webapp.deploy.validate.noWebApp"));
            }
            if (StringUtils.isEmpty(webAppSettingModel.getAppServicePlanName())) {
                // Service plan could be null as lazy loading, throw exception in this case
                throw new ConfigurationException(message("webapp.validate_deploy_configuration.loading"));
            }
            if (webAppSettingModel.isDeployToSlot()) {
                if (Constants.CREATE_NEW_SLOT.equals(webAppSettingModel.getSlotName())) {
                    if (StringUtils.isEmpty(webAppSettingModel.getNewSlotName())) {
                        throw new ConfigurationException(message("webapp.deploy.validate.noSlotName"));
                    }
                    if (!webAppSettingModel.getNewSlotName().matches(SLOT_NAME_REGEX)) {
                        throw new ConfigurationException(message("webapp.deploy.validate.invalidSlotName"));
                    }
                } else if (StringUtils.isEmpty(webAppSettingModel.getSlotName())) {
                    throw new ConfigurationException(message("webapp.deploy.validate.noSlotName"));
                }
            }
        }
        // validate runtime with artifact
        final Runtime runtime = webAppSettingModel.getRuntime();
        final OperatingSystem operatingSystem = Optional.ofNullable(runtime).map(Runtime::getOperatingSystem).orElse(null);
        final JavaVersion javaVersion = Optional.ofNullable(runtime).map(Runtime::getJavaVersion).orElse(null);
        if (operatingSystem == OperatingSystem.DOCKER) {
            throw new ConfigurationException(message("webapp.validate_deploy_configuration.dockerRuntime"));
        }
        if (javaVersion == null || Objects.equals(javaVersion, JavaVersion.OFF)) {
            throw new ConfigurationException(message("webapp.validate_deploy_configuration.invalidRuntime"));
        }
        final String webContainer = runtime.getWebContainer().getValue();
        final String artifactPackage = webAppSettingModel.getPackaging();
        if (StringUtils.containsIgnoreCase(webContainer, TOMCAT) && !StringUtils.equalsAnyIgnoreCase(artifactPackage, "war")) {
            throw new ConfigurationException(message("webapp.deploy.validate.invalidTomcatArtifact"));
        } else if (StringUtils.containsIgnoreCase(webContainer, JBOSS) && !StringUtils.equalsAnyIgnoreCase(artifactPackage, "war", "ear")) {
            throw new ConfigurationException(message("webapp.deploy.validate.invalidJbossArtifact"));
        } else if (Objects.equals(runtime.getWebContainer(), WebContainer.JAVA_SE) && !StringUtils.equalsAnyIgnoreCase(artifactPackage, "jar")) {
            throw new ConfigurationException(message("webapp.deploy.validate.invalidJavaSeArtifact"));
        }
        if (StringUtils.isEmpty(webAppSettingModel.getArtifactIdentifier())) {
            throw new ConfigurationException(message("webapp.deploy.validate.missingArtifact"));
        }
    }

    public String getWebAppId() {
        return webAppSettingModel.getWebAppId();
    }

    public void setWebAppId(String id) {
        webAppSettingModel.setWebAppId(id);
    }

    @Override
    public String getSubscriptionId() {
        return webAppSettingModel.getSubscriptionId();
    }

    public void setSubscriptionId(String sid) {
        webAppSettingModel.setSubscriptionId(sid);
    }

    public boolean isDeployToRoot() {
        return webAppSettingModel.isDeployToRoot();
    }

    public void setDeployToRoot(boolean toRoot) {
        webAppSettingModel.setDeployToRoot(toRoot);
    }

    public boolean isDeployToSlot() {
        return webAppSettingModel.isDeployToSlot();
    }

    public void setDeployToSlot(final boolean deployToSlot) {
        webAppSettingModel.setDeployToSlot(deployToSlot);
    }

    public String getSlotName() {
        return webAppSettingModel.getSlotName();
    }

    public void setSlotName(final String slotName) {
        webAppSettingModel.setSlotName(slotName);
    }

    public String getNewSlotName() {
        return webAppSettingModel.getNewSlotName();
    }

    public void setNewSlotName(final String newSlotName) {
        webAppSettingModel.setNewSlotName(newSlotName);
    }

    public String getNewSlotConfigurationSource() {
        return webAppSettingModel.getNewSlotConfigurationSource();
    }

    public void setNewSlotConfigurationSource(final String newSlotConfigurationSource) {
        webAppSettingModel.setNewSlotConfigurationSource(newSlotConfigurationSource);
    }

    public boolean isCreatingNew() {
        return webAppSettingModel.isCreatingNew();
    }

    public void setCreatingNew(boolean isCreating) {
        webAppSettingModel.setCreatingNew(isCreating);
    }

    public String getWebAppName() {
        return webAppSettingModel.getWebAppName();
    }

    public void setWebAppName(String name) {
        webAppSettingModel.setWebAppName(name);
    }

    public boolean isCreatingResGrp() {
        return webAppSettingModel.isCreatingResGrp();
    }

    public void setCreatingResGrp(boolean isCreating) {
        webAppSettingModel.setCreatingResGrp(isCreating);
    }

    public String getResourceGroup() {
        return webAppSettingModel.getResourceGroup();
    }

    public void setResourceGroup(String name) {
        webAppSettingModel.setResourceGroup(name);
    }

    public boolean isCreatingAppServicePlan() {
        return webAppSettingModel.isCreatingAppServicePlan();
    }

    public void setCreatingAppServicePlan(boolean isCreating) {
        webAppSettingModel.setCreatingAppServicePlan(isCreating);
    }

    public String getAppServicePlanName() {
        return webAppSettingModel.getAppServicePlanName();
    }

    public String getAppServicePlanResourceGroupName() {
        return webAppSettingModel.getAppServicePlanResourceGroupName();
    }

    public void setAppServicePlanName(String name) {
        webAppSettingModel.setAppServicePlanName(name);
    }

    public void setAppServicePlanResourceGroupName(String name) {
        webAppSettingModel.setAppServicePlanResourceGroupName(name);
    }

    public String getRegion() {
        return webAppSettingModel.getRegion();
    }

    public void setRegion(String region) {
        webAppSettingModel.setRegion(region);
    }

    public String getPricing() {
        return webAppSettingModel.getPricing();
    }

    public void setPricing(String price) {
        webAppSettingModel.setPricing(price);
    }

    @Override
    public String getTargetPath() {
        return webAppSettingModel.getTargetPath();
    }

    public void setTargetPath(String path) {
        webAppSettingModel.setTargetPath(path);
    }

    public void setTargetName(String name) {
        webAppSettingModel.setTargetName(name);
    }

    @Override
    public String getTargetName() {
        return webAppSettingModel.getTargetName();
    }

    public boolean isOpenBrowserAfterDeployment() {
        return webAppSettingModel.isOpenBrowserAfterDeployment();
    }

    public void setOpenBrowserAfterDeployment(boolean openBrowserAfterDeployment) {
        webAppSettingModel.setOpenBrowserAfterDeployment(openBrowserAfterDeployment);
    }

    public boolean isSlotPanelVisible() {
        return webAppSettingModel.isSlotPanelVisible();
    }

    public void setSlotPanelVisible(boolean slotPanelVisible) {
        webAppSettingModel.setSlotPanelVisible(slotPanelVisible);
    }

    public AzureArtifactType getAzureArtifactType() {
        return webAppSettingModel.getAzureArtifactType();
    }

    public void setAzureArtifactType(final AzureArtifactType azureArtifactType) {
        webAppSettingModel.setAzureArtifactType(azureArtifactType);
    }

    public String getArtifactIdentifier() {
        return webAppSettingModel.getArtifactIdentifier();
    }

    public void setArtifactIdentifier(final String artifactIdentifier) {
        webAppSettingModel.setArtifactIdentifier(artifactIdentifier);
    }

    public void saveArtifact(AzureArtifact azureArtifact) {
        final AzureArtifactManager azureArtifactManager = AzureArtifactManager.getInstance(getProject());
        webAppSettingModel.setArtifactIdentifier(azureArtifact == null ? null : azureArtifactManager.getArtifactIdentifier(azureArtifact));
        webAppSettingModel.setAzureArtifactType(azureArtifact == null ? null : azureArtifact.getType());
        webAppSettingModel.setPackaging(azureArtifact == null ? null : azureArtifactManager.getPackaging(azureArtifact));
    }

    public void saveRuntime(final Runtime runtime) {
        webAppSettingModel.saveRuntime(runtime);
    }

    public Set<String> getAppSettingsToRemove() {
        return webAppSettingModel.getAppSettingsToRemove();
    }

    public void setAppSettingsToRemove(@Nonnull Set<String> appSettingsToRemove) {
        webAppSettingModel.setAppSettingsToRemove(appSettingsToRemove);
    }

    public void setWebApp(@Nonnull WebApp webApp) {
        this.setWebAppId(webApp.getId());
        this.setWebAppName(webApp.getName());
        this.setSubscriptionId(webApp.getSubscriptionId());
        this.setResourceGroup(webApp.getResourceGroupName());
        Optional.ofNullable(webApp.getAppServicePlan()).ifPresent(plan -> {
            this.setAppServicePlanName(plan.getName());
            this.setAppServicePlanResourceGroupName(plan.getResourceGroupName());
        });
        Optional.ofNullable(webApp.getRegion()).ifPresent(r -> this.setRegion(r.getName()));
        this.setApplicationSettings(webApp.getAppSettings());
    }
}
