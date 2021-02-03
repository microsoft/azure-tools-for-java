/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azuretools.core.mvp.ui.base.MvpView;
import java.util.List;

public interface DeploymentSlotModuleView extends MvpView {
    void renderDeploymentSlots(final List<DeploymentSlot>slots);
}
