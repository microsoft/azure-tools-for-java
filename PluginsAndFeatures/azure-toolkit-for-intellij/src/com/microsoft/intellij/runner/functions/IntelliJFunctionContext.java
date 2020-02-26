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
 *
 */

package com.microsoft.intellij.runner.functions;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.common.project.IProject;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.intellij.runner.functions.library.IFunctionContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IntelliJFunctionContext implements IFunctionContext {

    protected Map<Class<? extends Object>, Object> providerMap = new HashMap<>();

    protected String subscription;

    protected String resourceGroup;

    protected String appName;

    protected String region;

    protected String pricingTier;

    protected String appServicePlanResourceGroup;

    protected String appServicePlanName;

    protected String deploymentStagingDirectoryPath;

    protected AuthConfiguration authentication;

    protected String deployment;

    protected IntelliJFunctionRuntimeConfiguration runtime;

    private Map<String, String> appSettings = new HashMap<>();

    protected Project project;

    private String moduleFilePath;

    public IntelliJFunctionContext(Project project) {
        this.project = project;
    }

    @Override
    public String getDeploymentStagingDirectoryPath() {
        return deploymentStagingDirectoryPath;
    }

    @Override
    public String getSubscription() {
        return subscription;
    }

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public String getResourceGroup() {
        return resourceGroup;
    }

    @Override
    public RuntimeConfiguration getRuntime() {
        return runtime;
    }

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public String getPricingTier() {
        return pricingTier;
    }

    @Override
    public String getAppServicePlanResourceGroup() {
        return appServicePlanResourceGroup;
    }

    @Override
    public String getAppServicePlanName() {
        return appServicePlanName;
    }

    @Override
    public Map<String, String> getAppSettings() {
        return appSettings;
    }

    @Override
    public AuthConfiguration getAuth() {
        return null;
    }

    @Override
    public String getDeploymentType() {
        return "";
    }

    @Override
    public Azure getAzureClient() throws AzureExecutionException {
        try {
            return AuthMethodManager.getInstance().getAzureManager().getAzure(subscription);
        } catch (IOException e) {
            throw new AzureExecutionException(e.getMessage(), e);
        }
    }

    @Override
    public IProject getProject() {
        return null;
    }

    public String getModuleFilePath() {
        return moduleFilePath;
    }

    public void setSubscription(String subscription) {
        this.subscription = subscription;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setPricingTier(String pricingTier) {
        this.pricingTier = pricingTier;
    }

    public void setAppServicePlanName(String appServicePlanName) {
        this.appServicePlanName = appServicePlanName;
    }

    public void setAppServicePlanResourceGroup(String appServicePlanResourceGroup) {
        this.appServicePlanName = appServicePlanResourceGroup;
    }

    public void setAuthentication(AuthConfiguration authentication) {
        this.authentication = authentication;
    }

    public void setDeployment(String deployment) {
        this.deployment = deployment;
    }

    public void setRuntime(IntelliJFunctionRuntimeConfiguration runtime) {
        this.runtime = runtime;
    }

    public void setAppSettings(Map<String, String> appSettings) {
        this.appSettings = appSettings;
    }

    public void setDeploymentStagingDirectoryPath(String deploymentStagingDirectoryPath) {
        this.deploymentStagingDirectoryPath = deploymentStagingDirectoryPath;
    }

    public void setModuleFilePath(String moduleFilePath) {
        this.moduleFilePath = moduleFilePath;
    }

    public void validate() throws ConfigurationException {
        // todo: add validation method
    }

    public Map<String, String> getTelemetryProperties(Map<String, String> properties) {
        return new HashMap<>();
    }
}
