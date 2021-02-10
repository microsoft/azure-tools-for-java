/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.serviceexplorer.azure;

import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule;

public class AzureModuleImpl extends AzureModule {
    public AzureModuleImpl(@Nullable Object project) {
        super(project);
    }

    @Override
    protected void loadActions() {
        super.loadActions();
        addAction(new SignInOutAction(this));
        addAction(new ManageSubscriptionsAction(this));
    }
}
