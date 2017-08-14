/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
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
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.intellij.runner.webapp.WebAppConfigurationType;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class WebAppConfiguration extends RunConfigurationBase {

    // const string
    private static final String NEED_SIGN_IN = "Please sign in with your Azure account.";
    private static final String NEED_CHOOSE_WEB_APP = "Choose a web app to deploy.";
    private static final String MISSING_WEB_APP_NAME = "Web App name not provided.";
    private static final String MISSING_SUBSCRIPTION = "Subscription not provided.";
    private static final String MISSING_WEB_CONTAINER = "Web Container not provided.";
    private static final String MISSING_RESOURCE_GROUP = "Resource Group not provided.";
    private static final String MISSING_APP_SERVICE_PLAN = "App Service Plan not provided.";
    private static final String MISSING_LOCATION = "Location not provided.";
    private static final String MISSING_PRICING_TIER = "Pricing Tier not provided.";
    private static final String MISSING_ARTIFACT = "A web archive (.war) artifact has not been configured.";
    private static final String INVALID_WAR_FILE = "The artifact name %s is invalid. "
        + "An artifact name maycontain only the ASCII letters 'a' through 'z' (case-insensitive), "
        + "the digits '0' through '9', '-' and '_'.";

    private static final String WAR_NAME_REGEX = "^[A-Za-z0-9_-]+\\.war$";
    private static final String WEB_APP_CONFIGURATION_NODE = "AzureWebAppConfig";
    private final WebAppSettingModel webAppSettingModel;
    private boolean firstTimeCreated = true;

    public WebAppConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, String.format("%s:%s", WebAppConfigurationType.getInstance().getDisplayName(),
                project.getName()));
        webAppSettingModel = new WebAppSettingModel();
    }

    public boolean isFirstTimeCreated() {
        return firstTimeCreated;
    }

    public void setFirstTimeCreated(boolean firstTimeCreated) {
        this.firstTimeCreated = firstTimeCreated;
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new WebAppSettingEditor(getProject(), this);
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        firstTimeCreated = Comparing.equal(element.getAttributeValue("default"), "true");
        webAppSettingModel.readExternal(element.getChild(WEB_APP_CONFIGURATION_NODE));
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        Element newElement = new Element(WEB_APP_CONFIGURATION_NODE);
        webAppSettingModel.writeExternal(newElement);
        element.addContent(newElement);
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment)
            throws ExecutionException {
        return new WebAppRunState(getProject(), this.webAppSettingModel);
    }

    public void validate() throws ConfigurationException {
        try {
            if (!AuthMethodManager.getInstance().isSignedIn()) {
                throw new ConfigurationException(NEED_SIGN_IN);
            }
        } catch (IOException e) {
            throw new ConfigurationException(NEED_SIGN_IN);
        }
        if (webAppSettingModel.isCreatingNew()) {
            if (Utils.isEmptyString(webAppSettingModel.getWebAppName())) {
                throw new ConfigurationException(MISSING_WEB_APP_NAME);
            }
            if (Utils.isEmptyString(webAppSettingModel.getSubscriptionId())) {
                throw new ConfigurationException(MISSING_SUBSCRIPTION);
            }
            if (Utils.isEmptyString(webAppSettingModel.getWebContainer())) {
                throw new ConfigurationException(MISSING_WEB_CONTAINER);
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
        }
        if (Utils.isEmptyString(webAppSettingModel.getTargetName())) {
            throw new ConfigurationException(MISSING_ARTIFACT);
        }
        if (!webAppSettingModel.isDeployToRoot() && !webAppSettingModel.getTargetName().matches(WAR_NAME_REGEX)) {
            throw new ConfigurationException(String.format(INVALID_WAR_FILE, webAppSettingModel.getTargetName()));
        }
    }

    public void setWebAppId(String id) {
        webAppSettingModel.setWebAppId(id);
    }

    public String getWebAppId() {
        return webAppSettingModel.getWebAppId();
    }

    public void setSubscriptionId(String sid) {
        webAppSettingModel.setSubscriptionId(sid);
    }

    public String getSubscriptionId() {
        return webAppSettingModel.getSubscriptionId();
    }

    public void setDeployToRoot(boolean toRoot) {
        webAppSettingModel.setDeployToRoot(toRoot);
    }

    public boolean isDeployToRoot() {
        return webAppSettingModel.isDeployToRoot();
    }

    public void setCreatingNew(boolean isCreating) {
        webAppSettingModel.setCreatingNew(isCreating);
    }

    public boolean isCreatingNew() {
        return webAppSettingModel.isCreatingNew();
    }

    public void setWebAppName(String name) {
        webAppSettingModel.setWebAppName(name);
    }

    public String getWebAppName() {
        return webAppSettingModel.getWebAppName();
    }

    public void setWebContainer(String container) {
        webAppSettingModel.setWebContainer(container);
    }

    public String getWebContainer() {
        return webAppSettingModel.getWebContainer();
    }

    public void setCreatingResGrp(boolean isCreating) {
        webAppSettingModel.setCreatingResGrp(isCreating);
    }

    public boolean isCreatingResGrp() {
        return webAppSettingModel.isCreatingResGrp();
    }

    public void setResourceGroup(String name) {
        webAppSettingModel.setResourceGroup(name);
    }

    public String getResourceGroup() {
        return webAppSettingModel.getResourceGroup();
    }

    public void setCreatingAppServicePlan(boolean isCreating) {
        webAppSettingModel.setCreatingAppServicePlan(isCreating);
    }

    public boolean isCreatingAppServicePlan() {
        return webAppSettingModel.isCreatingAppServicePlan();
    }

    public void setAppServicePlanName(String name) {
        webAppSettingModel.setAppServicePlanName(name);
    }

    public String getAppServicePlanName() {
        return webAppSettingModel.getAppServicePlanName();
    }

    public void setAppServicePlanId(String id) {
        webAppSettingModel.setAppServicePlanId(id);
    }

    public String getAppServicePlanId() {
        return webAppSettingModel.getAppServicePlanId();
    }

    public void setRegion(String region) {
        webAppSettingModel.setRegion(region);
    }

    public String getRegion() {
        return webAppSettingModel.getRegion();
    }

    public void setPricing(String price) {
        webAppSettingModel.setPricing(price);
    }

    public String getPricing() {
        return webAppSettingModel.getPricing();
    }

    public void setJdkChoice(String jdk) {
        webAppSettingModel.setJdkChoice(jdk);
    }

    public String getJdkChoice() {
        return webAppSettingModel.getJdkChoice();
    }

    public void setJdkUrl(String url) {
        webAppSettingModel.setJdkUrl(url);
    }

    public String getJdkUrl() {
        return webAppSettingModel.getJdkUrl();
    }

    public void setTargetPath(String path) {
        webAppSettingModel.setTargetPath(path);
    }

    public String getTargetPath() {
        return webAppSettingModel.getTargetPath();
    }

    public void setTargetName(String name) {
        webAppSettingModel.setTargetName(name);
    }

    public String getTargetName() {
        return webAppSettingModel.getTargetName();
    }
}
