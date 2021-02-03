/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner;

import com.microsoft.azure.common.project.IProject;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azure.toolkit.intellij.function.runner.library.IFunctionContext;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
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

    private Map<String, String> appSettings = new HashMap<>();

    private String moduleName;

    private String insightsName;

    private String instrumentationKey;

    private String os;

    private String javaVersion;

    @Override
    public IntelliJFunctionRuntimeConfiguration getRuntime() {
        IntelliJFunctionRuntimeConfiguration result = new IntelliJFunctionRuntimeConfiguration();
        result.setJavaVersion(javaVersion);
        result.setOs(os);
        return result;
    }

    public void saveRuntime(IntelliJFunctionRuntimeConfiguration runtime) {
        setOs(runtime.getOs());
        setJavaVersion(runtime.getJavaVersion());
    }

    @Override
    public String getDeploymentType() {
        return "";
    }

    @Override
    public Azure getAzureClient() {
        return AuthMethodManager.getInstance().getAzureManager().getAzure(subscription);
    }

    @Override
    public IProject getProject() {
        return null;
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
