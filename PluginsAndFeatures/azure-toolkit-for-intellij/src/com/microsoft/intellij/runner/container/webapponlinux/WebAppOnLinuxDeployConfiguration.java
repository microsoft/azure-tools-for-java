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

package com.microsoft.intellij.runner.container.webapponlinux;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppOnLinuxDeployModel;
import com.microsoft.intellij.runner.AzureRunConfigurationBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;

public class WebAppOnLinuxDeployConfiguration extends AzureRunConfigurationBase<WebAppOnLinuxDeployModel> {

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
        return new WebAppOnLinuxDeploySettingsEditor(this.getProject());
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment)
            throws ExecutionException {
        return new WebAppOnLinuxDeployState(getProject(), deployModel);
    }

    /**
     * Configuration value Validation.
     */
    @Override
    public void validate() throws ConfigurationException {
        checkAzureOperationPreconditions();
        if (Utils.isEmptyString(deployModel.getDockerFilePath())
                || !Paths.get(deployModel.getDockerFilePath()).toFile().exists()) {
            throw new ConfigurationException(INVALID_DOCKER_FILE);
        }
        // acr
        PrivateRegistryImageSetting setting = deployModel.getPrivateRegistryImageSetting();
        if (Utils.isEmptyString(setting.getServerUrl()) || !setting.getServerUrl().matches(DOMAIN_NAME_REGEX)) {
            throw new ConfigurationException(MISSING_SERVER_URL);
        }
        if (Utils.isEmptyString(setting.getUsername())) {
            throw new ConfigurationException(MISSING_USERNAME);
        }
        if (Utils.isEmptyString(setting.getPassword())) {
            throw new ConfigurationException(MISSING_PASSWORD);
        }
        String imageTag = setting.getImageTagWithServerUrl();
        if (Utils.isEmptyString(imageTag)) {
            throw new ConfigurationException(MISSING_IMAGE_WITH_TAG);
        }
        if (imageTag.endsWith(":")) {
            throw new ConfigurationException(CANNOT_END_WITH_COLON);
        }
        final String[] repoAndTag = imageTag.split(":");

        // check repository first
        if (repoAndTag[0].length() < 1 || repoAndTag[0].length() > REPO_LENGTH) {
            throw new ConfigurationException(REPO_LENGTH_INVALID);
        }
        if (repoAndTag[0].endsWith("/")) {
            throw new ConfigurationException(CANNOT_END_WITH_SLASH);
        }
        final String[] repoComponents = repoAndTag[0].split("/");
        for (String component : repoComponents) {
            if (!component.matches(REPO_COMPONENTS_REGEX)) {
                throw new ConfigurationException(String.format(REPO_COMPONENT_INVALID, component,
                        REPO_COMPONENTS_REGEX));
            }
        }
        // check when contains tag
        if (repoAndTag.length == 2) {
            if (repoAndTag[1].length() > TAG_LENGTH) {
                throw new ConfigurationException(TAG_LENGTH_INVALID);
            }
            if (!repoAndTag[1].matches(TAG_REGEX)) {
                throw new ConfigurationException(String.format(TAG_INVALID, repoAndTag[1], TAG_REGEX));
            }
        }
        if (repoAndTag.length > 2) {
            throw new ConfigurationException(INVALID_IMAGE_WITH_TAG);
        }
        // web app
        if (deployModel.isCreatingNewWebAppOnLinux()) {
            if (Utils.isEmptyString(deployModel.getWebAppName())) {
                throw new ConfigurationException(MISSING_WEB_APP);
            }
            if (Utils.isEmptyString(deployModel.getSubscriptionId())) {
                throw new ConfigurationException(MISSING_SUBSCRIPTION);
            }
            if (Utils.isEmptyString(deployModel.getResourceGroupName())) {
                throw new ConfigurationException(MISSING_RESOURCE_GROUP);
            }

            if (deployModel.isCreatingNewAppServicePlan()) {
                if (Utils.isEmptyString(deployModel.getAppServicePlanName())) {
                    throw new ConfigurationException(MISSING_APP_SERVICE_PLAN);
                }
            } else {
                if (Utils.isEmptyString(deployModel.getAppServicePlanId())) {
                    throw new ConfigurationException(MISSING_APP_SERVICE_PLAN);
                }
            }

        } else {
            if (Utils.isEmptyString(deployModel.getWebAppId())) {
                throw new ConfigurationException(MISSING_WEB_APP);
            }
        }

        // target package
        if (Utils.isEmptyString(deployModel.getTargetName())) {
            throw new ConfigurationException(MISSING_ARTIFACT);
        }
        if (!deployModel.getTargetName().matches(ARTIFACT_NAME_REGEX)) {
            throw new ConfigurationException(String.format(INVALID_WAR_FILE, deployModel.getTargetName()));
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

    public String getAppServicePlanId() {
        return deployModel.getAppServicePlanId();
    }

    public void setAppServicePlanId(String appServicePlanId) {
        deployModel.setAppServicePlanId(appServicePlanId);
    }

    public String getAppServicePlanName() {
        return deployModel.getAppServicePlanName();
    }

    public void setAppServicePlanName(String appServicePlanName) {
        deployModel.setAppServicePlanName(appServicePlanName);
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
}
