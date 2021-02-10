/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base;

import com.microsoft.azuretools.core.mvp.ui.base.MvpView;

public interface WebAppBaseNodeView extends MvpView {
    void renderNode(WebAppBaseState state);
}
