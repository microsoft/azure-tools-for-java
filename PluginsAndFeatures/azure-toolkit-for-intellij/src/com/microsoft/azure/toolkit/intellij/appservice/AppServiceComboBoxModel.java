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
