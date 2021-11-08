/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.util;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;

import java.util.Objects;

public class AzureSignInHelper {
    public static void requireSignedIn(Project project, Runnable runnable) {
        // Todo(andxu): legacy code shall be deleted later.
        final Action<Runnable> requireAuth = AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH);
        final AnActionEvent event = AnActionEvent.createFromAnAction(ActionManager.getInstance().getAction("AzureToolkit.AzureSignIn"),
                null, "not_used", SimpleDataContext.getSimpleContext(CommonDataKeys.PROJECT, project));
        if (Objects.nonNull(requireAuth)) {
            requireAuth.handle(() -> runnable.run(), event);
        }
    }
}
