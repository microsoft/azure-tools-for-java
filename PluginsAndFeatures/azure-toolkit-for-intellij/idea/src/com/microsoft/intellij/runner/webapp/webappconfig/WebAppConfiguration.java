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

package com.microsoft.intellij.runner.webapp.webappconfig;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.toolkit.intellij.webapp.WebAppComboBoxModel;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.intellij.runner.AzureRunConfigurationBase;
import com.microsoft.intellij.runner.webapp.Constants;
import com.microsoft.intellij.ui.components.AzureArtifact;
import com.microsoft.intellij.ui.components.AzureArtifactManager;
import com.microsoft.intellij.ui.components.AzureArtifactType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WebAppConfiguration extends AzureRunConfigurationBase<IntelliJWebAppSettingModel> {

    // const string
    private static final String NEED_CHOOSE_WEB_APP = "Choose a web app to deploy.";
    private static final String MISSING_WEB_APP_NAME = "Web App name not provided.";
    private static final String MISSING_SLOT_NAME = "The deployment slot name is not provided.";
    private static final String MISSING_SUBSCRIPTION = "Subscription not provided.";
    private static final String MISSING_WEB_CONTAINER = "Web Container not provided.";
    private static final String MISSING_RESOURCE_GROUP = "Resource Group not provided.";
    private static final String MISSING_APP_SERVICE_PLAN = "App Service Plan not provided.";
    private static final String MISSING_LOCATION = "Location not provided.";
    private static final String MISSING_PRICING_TIER = "Pricing Tier not provided.";
    private static final String MISSING_ARTIFACT = "A web archive (.war|.jar|.ear) artifact has not been configured.";
    private static final String SLOT_NAME_REGEX = "[a-zA-Z0-9-]{1,60}";
    private static final String INVALID_SLOT_NAME =
        "The slot name is invalid, it needs to match the pattern " + SLOT_NAME_REGEX;
    private static final String INVALID_RUNTIME = "Invalid target, please select a web app with Java runtime";
    private static final String INVALID_TOMCAT_ARTIFACT = "Invalid artifact, tomcat app service only supports .war artifact";
    private static final String INVALID_JBOSS_ARTIFACT = "Invalid artifact, jboss app service only supports .war or .ear artifact";
    private static final String INVALID_JAVA_SE_ARTIFACT = "Invalid artifact, java se app service only supports .jar artifact";
    private static final String TOMCAT = "tomcat";
    private static final String JAVA = "java";
    private static final String JBOSS = "jboss";
    private final IntelliJWebAppSettingModel webAppSettingModel;

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
    public void validate() throws ConfigurationException {
        checkAzurePreconditions();
        if (webAppSettingModel.isCreatingNew()) {
            if (Utils.isEmptyString(webAppSettingModel.getWebAppName())) {
                throw new ConfigurationException(MISSING_WEB_APP_NAME);
            }
            if (webAppSettingModel.getOS() == OperatingSystem.WINDOWS && Utils.isEmptyString(webAppSettingModel.getWebContainer())) {
                throw new ConfigurationException(MISSING_WEB_CONTAINER);
            }
            if (Utils.isEmptyString(webAppSettingModel.getSubscriptionId())) {
                throw new ConfigurationException(MISSING_SUBSCRIPTION);
            }
            if (Utils.isEmptyString(webAppSettingModel.getResourceGroup())) {
                throw new ConfigurationException(MISSING_RESOURCE_GROUP);
            }
            if (webAppSettingModel.isCreatingAppServicePlan()) {
                if (Utils.isEmptyString(webAppSettingModel.getRegion())) {
                    throw new ConfigurationException(MISSING_LOCATION);
                }
                if (Utils.isEmptyString(webAppSettingModel.getPricing())) {
                    throw new ConfigurationException(MISSING_PRICING_TIER);
                }
                if (Utils.isEmptyString(webAppSettingModel.getAppServicePlanName())) {
                    throw new ConfigurationException(MISSING_APP_SERVICE_PLAN);
                }
            } else {
                if (Utils.isEmptyString(webAppSettingModel.getAppServicePlanId())) {
                    throw new ConfigurationException(MISSING_APP_SERVICE_PLAN);
                }
            }
        } else {
            if (Utils.isEmptyString(webAppSettingModel.getWebAppId())) {
                throw new ConfigurationException(NEED_CHOOSE_WEB_APP);
            }
            if (webAppSettingModel.isDeployToSlot()) {
                if (webAppSettingModel.getSlotName().equals(Constants.CREATE_NEW_SLOT)) {
                    if (Utils.isEmptyString(webAppSettingModel.getNewSlotName())) {
                        throw new ConfigurationException(MISSING_SLOT_NAME);
                    }
                    if (!webAppSettingModel.getNewSlotName().matches(SLOT_NAME_REGEX)) {
                        throw new ConfigurationException(INVALID_SLOT_NAME);
                    }
                } else if (StringUtils.isEmpty(webAppSettingModel.getSlotName())) {
                    throw new ConfigurationException(MISSING_SLOT_NAME);
                }
            }
        }
        // validate runtime with artifact
        final String artifactPackage = webAppSettingModel.getPackaging();
        final String runtime = StringUtils.lowerCase(getRuntime());
        if (StringUtils.isEmpty(runtime)) {
            throw new ConfigurationException(INVALID_RUNTIME);
        } else if (StringUtils.contains(runtime, TOMCAT) && !StringUtils.equalsAnyIgnoreCase(artifactPackage, "war")) {
            throw new ConfigurationException(INVALID_TOMCAT_ARTIFACT);
        } else if (StringUtils.contains(runtime, JBOSS) && !StringUtils.equalsAnyIgnoreCase(artifactPackage, "war", "ear")) {
            throw new ConfigurationException(INVALID_JBOSS_ARTIFACT);
        } else if (StringUtils.contains(runtime, JAVA) && !StringUtils.equalsAnyIgnoreCase(artifactPackage, "jar")) {
            throw new ConfigurationException(INVALID_JAVA_SE_ARTIFACT);
        }
        if (StringUtils.isEmpty(webAppSettingModel.getArtifactIdentifier())) {
            throw new ConfigurationException(MISSING_ARTIFACT);
        }
    }

    private String getRuntime() {
        if (getOS() == OperatingSystem.LINUX) {
            return getModel().getStack();
        } else {
            if (StringUtils.containsIgnoreCase(getWebContainer(), TOMCAT)) {
                return TOMCAT;
            } else if (StringUtils.containsIgnoreCase(getWebContainer(), JAVA)) {
                return JAVA;
            } else if (StringUtils.containsIgnoreCase(getWebContainer(), JBOSS)) {
                return JBOSS;
            } else {
                return null;
            }
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

    public String getWebContainer() {
        return webAppSettingModel.getWebContainer();
    }

    public void setWebContainer(String container) {
        webAppSettingModel.setWebContainer(container);
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

    public JavaVersion getJdkVersion() {
        return webAppSettingModel.getJdkVersion();
    }

    public void setJdkVersion(JavaVersion jdk) {
        webAppSettingModel.setJdkVersion(jdk);
    }

    public OperatingSystem getOS() {
        return webAppSettingModel.getOS();
    }

    public void setOS(OperatingSystem value) {
        webAppSettingModel.setOS(value);
    }

    public void setStack(String value) {
        webAppSettingModel.setStack(value);
    }

    public void setVersion(String value) {
        webAppSettingModel.setVersion(value);
    }

    public RuntimeStack getLinuxRuntime() {
        return webAppSettingModel.getLinuxRuntime();
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
            setOS(settingModel.getOS());
            setStack(settingModel.getStack());
            setVersion(settingModel.getVersion());
            setJdkVersion(settingModel.getJdkVersion());
            setWebContainer(settingModel.getWebContainer());
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
            final WebApp webApp = webAppComboBoxModel.getResource();
            if (webApp != null) {
                setOS(webApp.operatingSystem());
                setAppServicePlanId(webApp.appServicePlanId());
                setRegion(webApp.regionName());
                setWebContainer(webApp.javaContainer() + " " + webApp.javaContainerVersion());
                setJdkVersion(webApp.javaVersion());
                final String linuxFxVersion = webApp.linuxFxVersion();
                if (StringUtils.contains(linuxFxVersion, "|")) {
                    final String[] runtime = linuxFxVersion.split("\\|");
                    setStack(runtime[0]);
                    setVersion(runtime[1]);
                }
            }
        }
    }
}
