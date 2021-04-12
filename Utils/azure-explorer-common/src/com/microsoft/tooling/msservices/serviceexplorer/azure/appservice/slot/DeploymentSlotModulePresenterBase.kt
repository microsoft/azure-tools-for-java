/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2020-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot

import com.microsoft.azure.management.appservice.DeploymentSlotBase
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter

abstract class DeploymentSlotModulePresenterBase<TSlot : DeploymentSlotBase<TSlot>,
        TView : DeploymentSlotModuleView<TSlot>> : MvpPresenter<TView>() {

    abstract fun onRefreshDeploymentSlotModule(subscriptionId: String, appId: String)

    abstract fun onDeleteDeploymentSlot(subscriptionId: String, appId: String, slotName: String)
}
