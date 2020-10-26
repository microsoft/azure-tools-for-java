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
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.intellij.runner.functions.deploy.FunctionDeployModel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class FunctionComboBoxModel extends AppServiceComboBoxModel<FunctionApp> {
    private String runtime;
    private FunctionDeployModel functionDeployModel;

    public FunctionComboBoxModel(final ResourceEx<FunctionApp> resourceEx) {
        super(resourceEx);
        final FunctionApp functionApp = resourceEx.getResource();
        this.runtime = functionApp.operatingSystem() == OperatingSystem.WINDOWS ?
                       String.format("%s-%s", "Windows", functionApp.javaVersion()) :
                       String.format("%s-%s", "Linux", functionApp.linuxFxVersion().replace("|", "-"));
    }

    public FunctionComboBoxModel(FunctionDeployModel functionDeployModel) {
        this.resourceId = functionDeployModel.getFunctionId();
        // In case recover from configuration, get the app name from resource id
        this.appName =
                StringUtils.isEmpty(functionDeployModel.getAppName()) && StringUtils.isNotEmpty(resourceId) ?
                AzureMvpModel.getSegment(resourceId, "sites") :
                functionDeployModel.getAppName();
        this.resourceGroup = functionDeployModel.getResourceGroup();
        this.subscriptionId = functionDeployModel.getSubscription();
        this.functionDeployModel = functionDeployModel;
    }
}
