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

package com.microsoft.intellij.runner.functions.deploy;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.LogLevel;
import com.microsoft.azure.toolkit.intellij.function.FunctionAppComboBoxModel;
import com.microsoft.azure.toolkit.lib.appservice.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.appservice.MonitorConfig;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import com.microsoft.intellij.runner.functions.IntelliJFunctionContext;
import com.microsoft.intellij.runner.functions.core.FunctionUtils;
import lombok.Data;

@Data
public class FunctionDeployModel extends IntelliJFunctionContext {

    private boolean isNewResource;
    private String functionId;
    private boolean enableApplicationLog;
    private LogLevel applicationLogLevel;

    public FunctionDeployModel() {

    }

    public void saveModel(FunctionAppComboBoxModel functionAppComboBoxModel) {
        setFunctionId(functionAppComboBoxModel.getResourceId());
        setAppName(functionAppComboBoxModel.getAppName());
        setResourceGroup(functionAppComboBoxModel.getResourceGroup());
        setSubscription(functionAppComboBoxModel.getSubscriptionId());
        if (functionAppComboBoxModel.isNewCreateResource() && functionAppComboBoxModel.getFunctionAppConfig() != null) {
            setNewResource(true);
            final FunctionAppConfig functionAppConfig = functionAppComboBoxModel.getFunctionAppConfig();
            setAppServicePlanName(functionAppConfig.getServicePlan().name());
            setAppServicePlanResourceGroup(functionAppConfig.getServicePlan().resourceGroupName());
            setPricingTier(functionAppConfig.getServicePlan().pricingTier().toSkuDescription().size());
            setRegion(functionAppConfig.getRegion().name());
            setOs(functionAppConfig.getPlatform().getOs().name());
            setJavaVersion(functionAppConfig.getPlatform().getStackVersionOrJavaVersion());
            final MonitorConfig monitorConfig = functionAppConfig.getMonitorConfig();
            final ApplicationInsightsConfig insightsModel = monitorConfig.getApplicationInsightsConfig();
            if (insightsModel != null) {
                setInsightsName(insightsModel.getName());
                setInstrumentationKey(insightsModel.getInstrumentationKey());
            } else {
                setInsightsName(null);
                setInstrumentationKey(null);
            }
            setEnableApplicationLog(monitorConfig.isEnableApplicationLog());
            setApplicationLogLevel(monitorConfig.getApplicationLogLevel());
        } else {
            setNewResource(false);
            final FunctionApp functionApp = functionAppComboBoxModel.getResource();
            if (functionApp != null) {
                setRegion(functionApp.regionName());
                setOs(functionApp.operatingSystem().name());
                setJavaVersion(FunctionUtils.getFunctionJavaVersion(functionApp));
            }
        }
    }

}
