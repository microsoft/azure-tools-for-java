/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.intellij.actions.AzureSignInAction;

import java.util.Optional;
import java.util.function.BiConsumer;

public class LegacyIntellijActionsContributor implements IActionsContributor {
    @Override
    public void registerActions(AzureActionManager am) {
        final AzureString authzTitle = AzureOperationBundle.title("account.authorize_action");
        final ActionView.Builder authzView = new ActionView.Builder("Authorize").title((s) -> authzTitle);
        final BiConsumer<Runnable, AnActionEvent> authzHandler = (Runnable r, AnActionEvent e) ->
            AzureSignInAction.requireSignedIn(Optional.ofNullable(e).map(AnActionEvent::getProject).orElse(null), r);
        am.registerAction(Action.REQUIRE_AUTH, new Action<>(authzHandler, authzView).authRequired(false));

        final AzureString authnTitle = AzureOperationBundle.title("account.authenticate");
        final ActionView.Builder authnView = new ActionView.Builder("Sign in").title((s) -> authnTitle);
        final BiConsumer<Void, AnActionEvent> authnHandler = (Void v, AnActionEvent e) -> {
            final AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
            if (authMethodManager.isSignedIn()) authMethodManager.signOut();
            AzureSignInAction.onAzureSignIn(e.getProject());
        };
        am.registerAction(Action.AUTHENTICATE, new Action<>(authnHandler, authnView).authRequired(false));
    }
}