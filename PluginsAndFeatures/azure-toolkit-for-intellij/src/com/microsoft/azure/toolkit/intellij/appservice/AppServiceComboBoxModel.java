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
import org.apache.commons.lang3.StringUtils;

public class AppServiceComboBoxModel<T extends WebAppBase> {
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
        final WebAppBase resource = resourceEx.getResource();
        this.resourceId = resource.id();
        this.appName = resource.name();
        this.resourceGroup = resource.resourceGroupName();
        this.os = StringUtils.capitalize(resource.operatingSystem().toString());
        this.subscriptionId = resourceEx.getSubscriptionId();
        this.isNewCreateResource = false;
    }

    public boolean isNewCreateResource() {
        return isNewCreateResource;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getAppName() {
        return appName;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getOs() {
        return os;
    }

    public String getResourceId() {
        return resourceId;
    }

    public T getResource() {
        return resource;
    }
}
