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

package com.microsoft.intellij.runner.functions;

import com.azure.resourcemanager.Azure;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.common.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.common.project.IProject;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.intellij.runner.functions.library.IFunctionContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IntelliJFunctionContext implements IFunctionContext {

    private Map<Class<? extends Object>, Object> providerMap = new HashMap<>();

    private String subscription;

    private String resourceGroup;

    private String appName;

    private String region;

    private String pricingTier;

    private String appServicePlanResourceGroup;

    private String appServicePlanName;

    private String deploymentStagingDirectoryPath;

    private String deployment;

    private IntelliJFunctionRuntimeConfiguration runtime;

    private Map<String, String> appSettings = new HashMap<>();

    private Project project;

    private String moduleName;

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
    public String getDeploymentType() {
        return "";
    }

    @Override
    public Azure getAzureClient() throws IOException {
        return AuthMethodManager.getInstance().getTrack2AzureClient(subscription);
    }

    @Override
    public IProject getProject() {
        return null;
    }

    public String getModuleName() {
        return moduleName;
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
        this.appServicePlanResourceGroup = appServicePlanResourceGroup;
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

    public void setModuleName(String name) {
        this.moduleName = name;
    }

    public void validate() throws ConfigurationException {
        // todo: add validation method
    }

    public Map<String, String> getTelemetryProperties(Map<String, String> properties) {
        HashMap result = new HashMap();

        try {
            if (properties != null) {
                result.putAll(properties);
            }
            result.put("runtime", this.getRuntime().getOs());
            result.put("subscriptionId", this.getSubscription());
            result.put("pricingTier", this.getPricingTier());
            result.put("region", this.getRegion());
        } catch (Exception e) {
            // swallow exception as telemetry should not break users operation
        }

        return result;
    }
}
