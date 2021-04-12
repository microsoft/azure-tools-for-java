/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments;

import com.microsoft.azuretools.core.mvp.ui.base.MvpView;

public interface DeploymentNodeView extends MvpView {

    void showExportTemplateResult(boolean isSuccess, Throwable t);
}
