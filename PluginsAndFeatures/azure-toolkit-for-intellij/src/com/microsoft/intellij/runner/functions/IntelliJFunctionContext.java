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

import com.microsoft.azure.common.project.IProject;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.intellij.runner.functions.library.IFunctionContext;
import lombok.Data;

import java.io.IOException;
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
    public Azure getAzureClient() throws IOException {
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
