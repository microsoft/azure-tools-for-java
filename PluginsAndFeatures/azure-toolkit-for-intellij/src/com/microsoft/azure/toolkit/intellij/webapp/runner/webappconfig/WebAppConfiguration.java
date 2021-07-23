/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactType;
import com.microsoft.azure.toolkit.intellij.common.AzureRunConfigurationBase;
import com.microsoft.azure.toolkit.intellij.webapp.WebAppComboBoxModel;
import com.microsoft.azure.toolkit.intellij.webapp.runner.Constants;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class WebAppConfiguration extends AzureRunConfigurationBase<IntelliJWebAppSettingModel> {

    // const string
    private static final String SLOT_NAME_REGEX = "[a-zA-Z0-9-]{1,60}";
    private static final String TOMCAT = "tomcat";
    private static final String JAVA = "java";
    private static final String JBOSS = "jboss";
    public static final String JAVA_VERSION = "javaVersion";
    private final IntelliJWebAppSettingModel webAppSettingModel;
    @Getter
    @Setter
    private Map<String, String> applicationSettings;

    public WebAppConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        webAppSettingModel = new IntelliJWebAppSettingModel();
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
            if (Utils.isEmptyString(webAppSettingModel.getWebAppName())) {
                throw new ConfigurationException(message("webapp.deploy.validate.noWebAppName"));
            }
            if (Utils.isEmptyString(webAppSettingModel.getSubscriptionId())) {
                throw new ConfigurationException(message("webapp.deploy.validate.noSubscription"));
            }
            if (Utils.isEmptyString(webAppSettingModel.getResourceGroup())) {
                throw new ConfigurationException(message("webapp.deploy.validate.noResourceGroup"));
            }
            if (webAppSettingModel.isCreatingAppServicePlan()) {
                if (Utils.isEmptyString(webAppSettingModel.getRegion())) {
                    throw new ConfigurationException(message("webapp.deploy.validate.noLocation"));
                }
                if (Utils.isEmptyString(webAppSettingModel.getPricing())) {
                    throw new ConfigurationException(message("webapp.deploy.validate.noPricingTier"));
                }
                if (Utils.isEmptyString(webAppSettingModel.getAppServicePlanName())) {
                    throw new ConfigurationException(message("webapp.deploy.validate.noAppServicePlan"));
                }
            } else {
                if (Utils.isEmptyString(webAppSettingModel.getAppServicePlanId())) {
                    throw new ConfigurationException(message("webapp.deploy.validate.noAppServicePlan"));
                }
            }
        } else {
            if (Utils.isEmptyString(webAppSettingModel.getWebAppId())) {
                throw new ConfigurationException(message("webapp.deploy.validate.noWebApp"));
            }
            if (webAppSettingModel.isDeployToSlot()) {
                if (webAppSettingModel.getSlotName().equals(Constants.CREATE_NEW_SLOT)) {
                    if (Utils.isEmptyString(webAppSettingModel.getNewSlotName())) {
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
        if (runtime == null) {
            throw new ConfigurationException(message("webapp.deploy.validate.invalidRuntime"));
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

    public void setAppServicePlanName(String name) {
        webAppSettingModel.setAppServicePlanName(name);
    }

    public String getAppServicePlanId() {
        return webAppSettingModel.getAppServicePlanId();
    }

    public void setAppServicePlanId(String id) {
        webAppSettingModel.setAppServicePlanId(id);
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

    public void saveModel(final WebAppComboBoxModel webAppComboBoxModel) {
        setWebAppId(webAppComboBoxModel.getResourceId());
        setWebAppName(webAppComboBoxModel.getAppName());
        setResourceGroup(webAppComboBoxModel.getResourceGroup());
        setSubscriptionId(webAppComboBoxModel.getSubscriptionId());
        if (webAppComboBoxModel.isNewCreateResource()) {
            setCreatingNew(true);
            final WebAppSettingModel settingModel = webAppComboBoxModel.getWebAppSettingModel();
            setCreatingResGrp(settingModel.isCreatingResGrp());
            setCreatingAppServicePlan(settingModel.isCreatingAppServicePlan());
            setAppServicePlanName(settingModel.getAppServicePlanName());
            setRegion(settingModel.getRegion());
            setPricing(settingModel.getPricing());
            setAppServicePlanId(settingModel.getAppServicePlanId());
            saveRuntime(settingModel.getRuntime());
            setCreatingResGrp(settingModel.isCreatingResGrp());
            setCreatingAppServicePlan(settingModel.isCreatingAppServicePlan());
            webAppSettingModel.setEnableApplicationLog(settingModel.isEnableApplicationLog());
            webAppSettingModel.setApplicationLogLevel(settingModel.getApplicationLogLevel());
            webAppSettingModel.setEnableWebServerLogging(settingModel.isEnableWebServerLogging());
            webAppSettingModel.setWebServerLogQuota(settingModel.getWebServerLogQuota());
            webAppSettingModel.setWebServerRetentionPeriod(settingModel.getWebServerRetentionPeriod());
            webAppSettingModel.setEnableDetailedErrorMessage(settingModel.isEnableDetailedErrorMessage());
            webAppSettingModel.setEnableFailedRequestTracing(settingModel.isEnableFailedRequestTracing());
        } else {
            setCreatingNew(false);
            final IWebApp webApp = webAppComboBoxModel.getResource();
            if (webApp != null) {
                saveRuntime(webApp.getRuntime());
                setAppServicePlanId(webApp.entity().getAppServicePlanId());
                setRegion(webApp.entity().getRegion().getName());
            }
        }
    }
}
