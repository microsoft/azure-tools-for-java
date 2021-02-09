/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.runner.deploy;

import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ServiceResourceInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.core.mvp.ui.base.MvpView;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SpringCloudDeploySettingMvpView extends MvpView {
    void fillSubscription(@NotNull List<Subscription> subscriptions);

    void fillClusters(@NotNull List<ServiceResourceInner> clusters);

    void fillApps(@NotNull List<AppResourceInner> apps);
}
