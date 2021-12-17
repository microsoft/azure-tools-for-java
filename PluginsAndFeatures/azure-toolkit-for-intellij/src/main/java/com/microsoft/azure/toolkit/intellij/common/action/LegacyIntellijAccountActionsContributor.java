/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.auth.IAccountActions;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.intellij.AzureConfigurable;
import com.microsoft.intellij.actions.AzureSignInAction;
import com.microsoft.intellij.actions.SelectSubscriptionsAction;

import java.util.Optional;
import java.util.function.BiConsumer;

public class LegacyIntellijAccountActionsContributor implements IActionsContributor {
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

        final AzureString selectSubsTitle = AzureOperationBundle.title("account.select_subscription");
        final ActionView.Builder selectSubsView = new ActionView.Builder("Select Subscriptions").title((s) -> authnTitle);
        final BiConsumer<Void, AnActionEvent> selectSubsHandler = (Void v, AnActionEvent e) ->
            SelectSubscriptionsAction.selectSubscriptions(e.getProject()).subscribe();
        am.registerAction(IAccountActions.SELECT_SUBS, new Action<>(selectSubsHandler, selectSubsView).authRequired(true));
    }

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiConsumer<Void, AnActionEvent> openSettingsHandler = (ignore, e) ->
            AzureTaskManager.getInstance().runAndWait(() ->
                ShowSettingsUtil.getInstance().showSettingsDialog(Optional.ofNullable(e).map(AnActionEvent::getProject).orElse(null),
                    AzureConfigurable.AzureAbstractConfigurable.class));
        am.registerHandler(ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS, (i, e) -> true, openSettingsHandler);
    }
}