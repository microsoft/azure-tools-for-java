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
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WebAppOnLinuxDeployConfiguration extends RunConfigurationBase {

    private static final String CONFIGURATION_ELEMENT_NODE_NAME = "WebAppOnLinuxDeployConfiguration";
    private final WebAppOnLinuxDeployModel webAppOnLinuxDeployModel;
    private boolean firstTimeCreated = true;

    protected WebAppOnLinuxDeployConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory,
                                               String name) {
        super(project, factory, name);
        webAppOnLinuxDeployModel = new WebAppOnLinuxDeployModel();
    }

    public boolean isFirstTimeCreated() {
        return firstTimeCreated;
    }

    public void setFirstTimeCreated(boolean firstTimeCreated) {
        this.firstTimeCreated = firstTimeCreated;
    }

    public WebAppOnLinuxDeployModel getWebAppOnLinuxDeployModel() {
        return webAppOnLinuxDeployModel;
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new WebAppOnLinuxDeploySettingsEditor(this.getProject());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        firstTimeCreated = Comparing.equal(element.getAttributeValue("default"), "true");
        webAppOnLinuxDeployModel.readExternal(element);
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        webAppOnLinuxDeployModel.writeExternal(element);
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment)
            throws ExecutionException {
        return new WebAppOnLinuxDeployState(getProject(), webAppOnLinuxDeployModel);
    }

    public void validate() throws ConfigurationException {
        // TODO: Add validation
    }

    public String getAppName() {
        return webAppOnLinuxDeployModel.getWebAppName();
    }

    public void setAppName(String appName) {
        webAppOnLinuxDeployModel.setWebAppName(appName);
    }

    public String getSubscriptionId() {
        return webAppOnLinuxDeployModel.getSubscriptionId();
    }

    public void setSubscriptionId(String subscriptionId) {
        webAppOnLinuxDeployModel.setSubscriptionId(subscriptionId);
    }

    public boolean isCreatingNewResourceGroup() {
        return webAppOnLinuxDeployModel.isCreatingNewResourceGroup();
    }

    public void setCreatingNewResourceGroup(boolean creatingNewResourceGroup) {
        webAppOnLinuxDeployModel.setCreatingNewResourceGroup
                (creatingNewResourceGroup);
    }

    public String getResourceGroupName() {
        return webAppOnLinuxDeployModel.getResourceGroupName();
    }

    public void setResourceGroupName(String resourceGroupName) {
        webAppOnLinuxDeployModel.setResourceGroupName(resourceGroupName);
    }

    public String getLocationName() {
        return webAppOnLinuxDeployModel.getLocationName();
    }

    public void setLocationName(String locationName) {
        webAppOnLinuxDeployModel.setLocationName(locationName);
    }

    public String getPricingSkuTier() {
        return webAppOnLinuxDeployModel.getPricingSkuTier();
    }

    public void setPricingSkuTier(String pricingSkuTier) {
        webAppOnLinuxDeployModel.setPricingSkuTier(pricingSkuTier);
    }

    public String getPricingSkuSize() {
        return webAppOnLinuxDeployModel.getPricingSkuSize();
    }

    public void setPricingSkuSize(String pricingSkuSize) {
        webAppOnLinuxDeployModel.setPricingSkuSize(pricingSkuSize);
    }

    public PrivateRegistryImageSetting getPrivateRegistryImageSetting() {
        return webAppOnLinuxDeployModel.getPrivateRegistryImageSetting();
    }

    public void setPrivateRegistryImageSetting(PrivateRegistryImageSetting privateRegistryImageSetting) {
        webAppOnLinuxDeployModel.setPrivateRegistryImageSetting(privateRegistryImageSetting);
    }

    public String getWebAppId() {
        return webAppOnLinuxDeployModel.getWebAppId();
    }

    public void setWebAppId(String webAppId) {
        webAppOnLinuxDeployModel.setWebAppId(webAppId);
    }

    public boolean isCreatingNewWebAppOnLinux() {
        return webAppOnLinuxDeployModel.isCreatingNewWebAppOnLinux();
    }

    public void setCreatingNewWebAppOnLinux(boolean creatingNewWebAppOnLinux) {
        webAppOnLinuxDeployModel.setCreatingNewWebAppOnLinux(creatingNewWebAppOnLinux);
    }
}
