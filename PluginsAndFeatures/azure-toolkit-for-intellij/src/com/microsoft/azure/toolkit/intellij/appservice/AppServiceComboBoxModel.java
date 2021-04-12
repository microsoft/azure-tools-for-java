/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice;

import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@Getter
public abstract class AppServiceComboBoxModel<T extends WebAppBase> {
    @Setter
    protected boolean isNewCreateResource;
    protected String subscriptionId;
    protected String resourceGroup;
    protected String appName;
    protected String os;
    protected String resourceId;
    protected T resource;

    public AppServiceComboBoxModel() {

    }

    public AppServiceComboBoxModel(ResourceEx<T> resourceEx) {
        this.resource = resourceEx.getResource();
        this.resourceId = resource.id();
        this.appName = resource.name();
        this.resourceGroup = resource.resourceGroupName();
        this.os = StringUtils.capitalize(resource.operatingSystem().toString());
        this.subscriptionId = resourceEx.getSubscriptionId();
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

    public abstract String getRuntime();
}
