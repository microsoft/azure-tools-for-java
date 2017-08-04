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

import com.microsoft.azuretools.azurecommons.util.Utils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.login.Configuration;

public class WebAppConfiguration extends RunConfigurationBase {

    // const string
    private static final String NEED_CHOOSE_WEB_APP = "Choose a web app to deploy.";
    private static final String MISSING_WEB_APP_NAME = "Web App name not provided.";
    private static final String MISSING_SUBSCRIPTION = "Subscription not provided.";
    private static final String MISSING_WEB_CONTAINER = "Web Container not provided.";
    private static final String MISSING_RESOURCE_GROUP = "Resource Group not provided.";
    private static final String MISSING_APP_SERVICE_PLAN = "App Service Plan not provided.";
    private static final String MISSING_LOCATION = "Location not provided.";
    private static final String MISSING_PRICING_TIER = "Pricing Tier not provided.";
    private static final String MISSING_JDK = "JDK not provided.";
    private static final String MISSING_KEY = "Storage key not provided.";

    private static String WEB_APP_CONFIGURATION_NODE = "AzureWebAppConfig";
    private WebAppSettingModel webAppSettingModel;
    private boolean firstTimeCreated = true;

    public WebAppConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, project.getName());
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
        return new WebAppSettingEditor(getProject());
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

    public WebAppSettingModel getWebAppSettingModel() {
        return this.webAppSettingModel;
    }

    public void validate() throws ConfigurationException {
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
            if (Utils.isEmptyString(webAppSettingModel.getAppServicePlan())) {
                throw new ConfigurationException(MISSING_APP_SERVICE_PLAN);
            }
            if (webAppSettingModel.isCreatingAppServicePlan()) {
                if (Utils.isEmptyString(webAppSettingModel.getRegion())) {
                    throw new ConfigurationException(MISSING_LOCATION);
                }
                if (Utils.isEmptyString(webAppSettingModel.getPricing())) {
                    throw new ConfigurationException(MISSING_PRICING_TIER);
                }
            }
            switch (WebAppSettingModel.JdkChoice.valueOf(webAppSettingModel.getJdkChoice())) {
                case THIRD_PARTY:
                    if (Utils.isEmptyString(webAppSettingModel.getJdkUrl())) {
                        throw new ConfigurationException(MISSING_JDK);
                    }
                    break;
                case CUSTOM:
                    if (Utils.isEmptyString(webAppSettingModel.getJdkUrl())) {
                        throw new ConfigurationException(MISSING_JDK);
                    }
                    if (Utils.isEmptyString(webAppSettingModel.getStorageKey())) {
                        throw new ConfigurationException(MISSING_KEY);
                    }
                    break;
                default:
                    break;
            }
        } else {
            if (Utils.isEmptyString(webAppSettingModel.getWebAppId())) {
                throw new ConfigurationException(NEED_CHOOSE_WEB_APP);
            }
        }
    }

    public void setWebAppId(String id) {
        webAppSettingModel.setWebAppId(id);
    }

    public void setSubscriptionId(String sid) {
        webAppSettingModel.setSubscriptionId(sid);
    }

    public void setWebAppUrl(String url) {
        webAppSettingModel.setWebAppUrl(url);
    }

    public void setDeployToRoot(boolean toRoot) {
        webAppSettingModel.setDeployToRoot(toRoot);
    }

    public void setCreatingNew(boolean isCreating) {
        webAppSettingModel.setCreatingNew(isCreating);
    }

    public void setWebAppName(String name) {
        webAppSettingModel.setWebAppName(name);
    }

    public void setWebContainer(String container) {
        webAppSettingModel.setWebContainer(container);
    }

    public void setCreatingResGrp(boolean isCreating) {
        webAppSettingModel.setCreatingResGrp(isCreating);
    }

    public void setResourceGroup(String name) {
        webAppSettingModel.setResourceGroup(name);
    }

    public void setCreatingAppServicePlan(boolean isCreating) {
        webAppSettingModel.setCreatingAppServicePlan(isCreating);
    }

    public void setAppServicePlan(String nameOrId) {
        webAppSettingModel.setAppServicePlan(nameOrId);
    }

    public void setRegion(String region) {
        webAppSettingModel.setRegion(region);
    }

    public void setPricing(String price) {
        webAppSettingModel.setPricing(price);
    }

    public void setJdkChoice(String jdk) {
        webAppSettingModel.setJdkChoice(jdk);
    }

    public void setJdkUrl(String url) {
        webAppSettingModel.setJdkUrl(url);
    }

    public void setStorageKey(String key) {
        webAppSettingModel.setStorageKey(key);
    }

    public void setTargetPath(String path) {
        webAppSettingModel.setTargetPath(path);
    }

    public void setTargetName(String name) {
        webAppSettingModel.setTargetName(name);
    }
}
