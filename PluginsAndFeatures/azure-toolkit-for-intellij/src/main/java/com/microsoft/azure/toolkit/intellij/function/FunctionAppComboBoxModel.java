/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.function;

import com.microsoft.azure.toolkit.intellij.appservice.AppServiceComboBoxModel;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.function.FunctionAppService;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

@Getter
public class FunctionAppComboBoxModel extends AppServiceComboBoxModel<IFunctionApp, FunctionAppConfig> {

    public FunctionAppComboBoxModel(IFunctionApp functionApp) {
        super(functionApp);
        this.config = FunctionAppService.getInstance().getFunctionAppConfigFromExistingFunction(functionApp);
    }

    public FunctionAppComboBoxModel(FunctionAppConfig functionAppConfig) {
        super();
        this.config = functionAppConfig;
        this.appName = functionAppConfig.getName();
        this.resourceGroup = Optional.ofNullable(functionAppConfig.getResourceGroup()).map(ResourceGroup::getName).orElse(StringUtils.EMPTY);
        this.subscriptionId = Optional.ofNullable(functionAppConfig.getSubscription()).map(Subscription::getId).orElse(StringUtils.EMPTY);
        this.resourceId = functionAppConfig.getResourceId();
        this.isNewCreateResource = StringUtils.isEmpty(resourceId);
        this.runtime = functionAppConfig.getRuntime();
    }
}
