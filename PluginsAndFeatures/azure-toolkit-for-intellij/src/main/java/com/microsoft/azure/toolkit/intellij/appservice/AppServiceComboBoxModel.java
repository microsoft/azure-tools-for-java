/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice;

import com.microsoft.azure.toolkit.lib.appservice.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@Getter
public abstract class AppServiceComboBoxModel<R extends IAppService, T extends AppServiceConfig> {
    @Setter
    protected boolean isNewCreateResource;
    protected String subscriptionId;
    protected String resourceGroup;
    protected String appName;
    protected String resourceId;
    protected Runtime runtime;
    protected R resource;
    protected T config;

    public AppServiceComboBoxModel() {

    }

    public AppServiceComboBoxModel(R appService) {
        this.resource = appService;
        this.resourceId = appService.id();
        this.appName = appService.name();
        this.resourceGroup = appService.resourceGroup();
        this.runtime = appService.getRuntime();
        this.subscriptionId = appService.subscriptionId();
        this.isNewCreateResource = false;
    }

    public static boolean isSameApp(AppServiceComboBoxModel first, AppServiceComboBoxModel second) {
        if (Objects.isNull(first) || Objects.isNull(second)) {
            return first == second;
        }
        return StringUtils.equalsIgnoreCase(first.resourceId, second.resourceId) ||
                (StringUtils.equalsIgnoreCase(first.appName, second.appName) &&
                        StringUtils.equalsIgnoreCase(first.resourceGroup, second.resourceGroup) &&
                        StringUtils.equalsIgnoreCase(first.subscriptionId, second.subscriptionId));
    }
}
