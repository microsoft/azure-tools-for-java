/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.auth.SignInAction;
import com.microsoft.azure.toolkit.intellij.common.subscription.SelectSubscriptionsAction;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.IAccountActions;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import java.util.Optional;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.OPEN_URL;

public class IntellijAccountActionsContributor implements IActionsContributor, IAccountActions {
    public static final String URL_TRY_AZURE_FOR_FREE = "https://azure.microsoft.com/en-us/free/?utm_campaign=javatools";

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(IAccountActions.TRY_AZURE)
            .withLabel("Try Azure for Free")
            .withHandler((Object v, AnActionEvent e) -> AzureActionManager.getInstance().getAction(OPEN_URL).handle(URL_TRY_AZURE_FOR_FREE))
            .withAuthRequired(false)
            .register(am);

        new Action<>(Action.REQUIRE_AUTH)
            .withLabel("Authorize")
            .withHandler((Consumer<IAccount> r, AnActionEvent e) ->
                SignInAction.requireSignedIn(Optional.ofNullable(e).map(AnActionEvent::getProject).orElse(null), r))
            .withAuthRequired(false)
            .register(am);

        new Action<>(Action.AUTHENTICATE)
            .withIcon((a) -> Azure.az(AzureAccount.class).isLoggedIn() ? AzureIcons.Common.SIGN_OUT.getIconPath() : AzureIcons.Common.SIGN_IN.getIconPath())
            .withLabel((a) -> Azure.az(AzureAccount.class).isLoggedIn() ? "Sign out" : "Sign in...")
            .withHandler((Object v, AnActionEvent e) -> AzureTaskManager.getInstance().runLater(()-> SignInAction.authActionPerformed(e.getProject())))
            .withAuthRequired(false)
            .register(am);

        new Action<>(IAccountActions.SELECT_SUBS)
            .withLabel("Select Subscriptions...")
            .withIcon(AzureIcons.Action.SELECT_SUBSCRIPTION.getIconPath())
            .withHandler((Object v, AnActionEvent e) -> SelectSubscriptionsAction.selectSubscriptions(e.getProject()))
            .withAuthRequired(false)
            .register(am);
    }

    @Override
    public int getOrder() {
        return ResourceCommonActionsContributor.INITIALIZE_ORDER + 1; //after azure resource common actions registered
    }
}
