/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.deploy;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.intellij.function.FunctionAppComboBoxModel;
import com.microsoft.azure.toolkit.intellij.function.runner.IntelliJFunctionContext;
import com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.lib.appservice.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.appservice.MonitorConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.LogLevel;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
            setAppServicePlanName(functionAppConfig.getServicePlan().getName());
            setAppServicePlanResourceGroup(ResourceId.fromString(functionAppConfig.getServicePlan().getId()).resourceGroupName());
            setPricingTier(functionAppConfig.getServicePlan().getPricingTier().toString());
            setRegion(functionAppConfig.getRegion().getName());
            setOs(functionAppConfig.getRuntime().getOperatingSystem().getValue());
            setJavaVersion(functionAppConfig.getRuntime().getJavaVersion().getValue());
            final MonitorConfig monitorConfig = functionAppConfig.getMonitorConfig();
            final ApplicationInsightsConfig insightsModel = monitorConfig.getApplicationInsightsConfig();
            if (insightsModel != null) {
                setInsightsName(insightsModel.getName());
                setInstrumentationKey(insightsModel.getInstrumentationKey());
            } else {
                setInsightsName(null);
                setInstrumentationKey(null);
            }
            setEnableApplicationLog(monitorConfig.getDiagnosticConfig().isEnableApplicationLog());
            setApplicationLogLevel(monitorConfig.getDiagnosticConfig().getApplicationLogLevel());
        } else {
            setNewResource(false);
            final IFunctionApp functionApp = functionAppComboBoxModel.getResource();
            if (functionApp != null) {
                setRegion(functionApp.entity().getRegion().getName());
                setOs(functionApp.getRuntime().getOperatingSystem().getValue());
                setJavaVersion(FunctionUtils.getFunctionJavaVersion(functionApp));
            }
        }
    }

}
