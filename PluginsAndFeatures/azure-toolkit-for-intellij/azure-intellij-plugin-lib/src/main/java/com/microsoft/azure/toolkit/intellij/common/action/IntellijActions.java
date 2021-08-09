/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.ide.BrowserUtil;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActions;

public class IntellijActions implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        am.<String>registerHandler(ResourceCommonActions.OPEN_URL, (s) -> true, BrowserUtil::browse);
    }

    @Override
    public int getZOrder() {
        return 2; //after azure resource common actions registered
    }
}
