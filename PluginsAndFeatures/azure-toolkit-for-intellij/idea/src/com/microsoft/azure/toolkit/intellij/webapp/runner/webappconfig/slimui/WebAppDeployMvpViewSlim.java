/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig.slimui;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.toolkit.intellij.webapp.WebAppComboBoxModel;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.ui.base.MvpView;

import java.util.List;

public interface WebAppDeployMvpViewSlim extends MvpView {
    void fillDeploymentSlots(@NotNull List<DeploymentSlot> slots, final WebAppComboBoxModel selectedWebApp);
}
