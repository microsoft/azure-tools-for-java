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
package com.microsoft.azure.toolkit.intellij.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceComboBoxModel;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.intellij.runner.functions.deploy.FunctionDeployModel;
import lombok.Getter;

@Getter
public class FunctionAppComboBoxModel extends AppServiceComboBoxModel<FunctionApp> {
    private String runtime;
    private FunctionDeployModel functionDeployModel;
    private FunctionAppConfig functionAppConfig;

    public FunctionAppComboBoxModel(final ResourceEx<FunctionApp> resourceEx) {
        super(resourceEx);
        final FunctionApp functionApp = resourceEx.getResource();
        this.runtime = functionApp.operatingSystem() == OperatingSystem.WINDOWS ?
                       String.format("%s-Java %s", "Windows", functionApp.javaVersion()) :
                       String.format("%s-%s", "Linux", functionApp.linuxFxVersion().replace("|", " "));
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
        this.resourceGroup = functionAppConfig.getResourceGroup().name();
        this.subscriptionId = functionAppConfig.getSubscription().subscriptionId();
        this.runtime = functionAppConfig.getPlatform().toString();
        this.functionAppConfig = functionAppConfig;
    }
}
