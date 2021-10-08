/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;

public class LegacyIntellijActionsContributor implements IActionsContributor {
    @Override
    public void registerActions(AzureActionManager am) {
        am.registerAction(Action.REQUIRE_AUTH, new Action<>((Runnable r, AnActionEvent e) -> r.run()).authRequired(false));
    }
}
