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

package com.microsoft.intellij.runner.webapp.webappconfig.ui;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.JdkModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpView;
import com.microsoft.azuretools.utils.WebAppUtils;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface WebAppDeployMvpView extends MvpView {

    void renderWebAppsTable(@NotNull List<ResourceEx<WebApp>> webAppLists);

    void enableDeploymentSlotPanel();

    void fillDeploymentSlots(@NotNull List<DeploymentSlot> slots);

    void fillSubscription(@NotNull List<Subscription> subscriptions);

    void fillResourceGroup(@NotNull List<ResourceGroup> resourceGroups);

    void fillAppServicePlan(@NotNull List<AppServicePlan> appServicePlans);

    void fillLocation(@NotNull List<Location> locations);

    void fillPricingTier(@NotNull List<PricingTier> prices);

    void fillWebContainer(@NotNull List<WebAppUtils.WebContainerMod> webContainers);

    void fillJdkVersion(@NotNull List<JdkModel> jdks);

    void fillLinuxRuntime(@NotNull List<RuntimeStack> linuxRuntimes);
}
