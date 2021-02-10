/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import java.util.List;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.ui.base.MvpView;

public interface WebAppModuleView extends MvpView {
    void renderChildren(List<ResourceEx<WebApp>> resourceExes);
}
