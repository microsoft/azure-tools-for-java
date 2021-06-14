/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.function;

import com.microsoft.azure.toolkit.intellij.appservice.AppServiceComboBoxModel;
import com.microsoft.azure.toolkit.intellij.function.runner.deploy.FunctionDeployModel;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import lombok.Getter;

@Getter
public class FunctionAppComboBoxModel extends AppServiceComboBoxModel<IFunctionApp> {
    private String runtime;
    private FunctionDeployModel functionDeployModel;
    private FunctionAppConfig functionAppConfig;

    public FunctionAppComboBoxModel(IFunctionApp functionApp) {
        super(functionApp);
        final Runtime runtimeConfig = functionApp.getRuntime();
        this.runtime = String.format("%s-Java %s", runtimeConfig.getOperatingSystem().getValue(), runtimeConfig.getJavaVersion());
    }

    public FunctionAppComboBoxModel(FunctionDeployModel functionDeployModel) {
        this.isNewCreateResource = functionDeployModel.isNewResource();
        this.subscriptionId = functionDeployModel.getSubscription();
        this.resourceId = functionDeployModel.getFunctionId();
        this.appName = isNewCreateResource ? functionDeployModel.getAppName() : AzureMvpModel.getSegment(resourceId, "sites");
        this.resourceGroup = functionDeployModel.getResourceGroup();
        this.runtime = String.format("%s-Java %s", functionDeployModel.getOs(), functionDeployModel.getJavaVersion());
        this.functionDeployModel = functionDeployModel;
    }

    public FunctionAppComboBoxModel(FunctionAppConfig functionAppConfig) {
        this.isNewCreateResource = true;
        this.appName = functionAppConfig.getName();
        this.resourceGroup = functionAppConfig.getResourceGroup().getName();
        this.subscriptionId = functionAppConfig.getSubscription().getId();
        this.runtime = functionAppConfig.getPlatform().toString();
        this.functionAppConfig = functionAppConfig;
    }
}
