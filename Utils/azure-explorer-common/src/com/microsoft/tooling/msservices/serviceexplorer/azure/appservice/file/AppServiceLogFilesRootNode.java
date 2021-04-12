/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file;

import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule;

public class AppServiceLogFilesRootNode extends AppServiceUserFilesRootNode {
    private static final String MODULE_ID = WebAppModule.class.getName();
    private static final String MODULE_NAME = "Logs";
    private static final String ROOT_PATH = "/LogFiles";

    public AppServiceLogFilesRootNode(final Node parent, final String subscriptionId, final WebAppBase app) {
        super(MODULE_NAME, parent, subscriptionId, app);
    }

    @NotNull
    @Override
    protected String getRootPath() {
        return ROOT_PATH;
    }

    @Override
    public int getPriority() {
        return HIGH_PRIORITY;
    }
}
