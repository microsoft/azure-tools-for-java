/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNodeView;

public interface WebAppNodeView extends WebAppBaseNodeView {
    void renderSubModules();
}
