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

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.intellij.ui.webapp.deploysetting.WebAppSettingPanel;
import com.microsoft.intellij.util.MavenRunTaskUtil;

import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public class WebAppSettingEditor extends SettingsEditor<WebAppConfiguration> {

    // const string
    private static final String INVALID_PROJECT_TYPE = "Current project is not a Maven project.";
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



    private final WebAppSettingPanel mainPanel;
    private final Project project;

    public WebAppSettingEditor(Project project) {
        this.project = project;
        mainPanel = new WebAppSettingPanel(project);
    }

    @Override
    protected void resetEditorFrom(@NotNull WebAppConfiguration webAppConfiguration) {
        if (webAppConfiguration.isFirstTimeCreated()) {
            MavenRunTaskUtil.addMavenPackageBeforeRunTask(webAppConfiguration);
        }
        webAppConfiguration.setFirstTimeCreated(false);
        mainPanel.resetEditorForm(webAppConfiguration);
    }

    @Override
    protected void applyEditorTo(@NotNull WebAppConfiguration webAppConfiguration) throws ConfigurationException {
        mainPanel.applyEditorTo(webAppConfiguration);
        validateConfiguration(webAppConfiguration);
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return mainPanel.getMainPanel();
    }

    private void validateConfiguration(@NotNull WebAppConfiguration webAppConfiguration) throws ConfigurationException {
        if (!MavenRunTaskUtil.isMavenProject(project)) {
            throw new ConfigurationException(INVALID_PROJECT_TYPE);
        }

        WebAppSettingModel model = webAppConfiguration.getWebAppSettingModel();
        if (model.isCreatingNew()) {
            if (Utils.isEmptyString(model.getWebAppName())) {
                throw new ConfigurationException(MISSING_WEB_APP_NAME);
            }
            if (Utils.isEmptyString(model.getSubscriptionId())) {
                throw new ConfigurationException(MISSING_SUBSCRIPTION);
            }
            if (Utils.isEmptyString(model.getWebContainer())) {
                throw new ConfigurationException(MISSING_WEB_CONTAINER);
            }
            if (Utils.isEmptyString(model.getResourceGroup())) {
                throw new ConfigurationException(MISSING_RESOURCE_GROUP);
            }
            if (Utils.isEmptyString(model.getAppServicePlan())) {
                throw new ConfigurationException(MISSING_APP_SERVICE_PLAN);
            }
            if (model.isCreatingAppServicePlan()) {
                if (Utils.isEmptyString(model.getRegion())) {
                    throw new ConfigurationException(MISSING_LOCATION);
                }
                if (Utils.isEmptyString(model.getPricing())) {
                    throw new ConfigurationException(MISSING_PRICING_TIER);
                }
            }
            switch (WebAppSettingModel.JdkChoice.valueOf(model.getJdkChoice())) {
                case THIRD_PARTY:
                    if (Utils.isEmptyString(model.getJdkUrl())) {
                        throw new ConfigurationException(MISSING_JDK);
                    }
                    break;
                case CUSTOM:
                    if (Utils.isEmptyString(model.getJdkUrl())) {
                        throw new ConfigurationException(MISSING_JDK);
                    }
                    if (Utils.isEmptyString(model.getStorageKey())) {
                        throw new ConfigurationException(MISSING_KEY);
                    }
                    break;
                default:
                    break;
            }
        } else {
            if (Utils.isEmptyString(model.getWebAppId())) {
                throw new ConfigurationException(NEED_CHOOSE_WEB_APP);
            }
        }
    }
}
