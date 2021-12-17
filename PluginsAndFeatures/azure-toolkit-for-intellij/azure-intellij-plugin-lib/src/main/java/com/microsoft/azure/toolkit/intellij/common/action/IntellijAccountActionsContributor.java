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

import java.util.function.BiConsumer;

import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.OPEN_URL;

public class IntellijAccountActionsContributor implements IActionsContributor {
    public static final Action.Id<Void> TRY_AZURE = Action.Id.of("action.account.try_azure");
    public static final Action.Id<Void> SELECT_SUBS = Action.Id.of("action.account.select_subs");
    public static final Action.Id<Void> AUTHENTICATE = Action.AUTHENTICATE;

    public static final String URL_TRY_AZURE_FOR_FREE = "https://azure.microsoft.com/en-us/free/?utm_campaign=javatools";

    @Override
    public void registerActions(AzureActionManager am) {
        final AzureString tryAzureTitle = AzureOperationBundle.title("account.try_aure");
        final ActionView.Builder tryAzureView = new ActionView.Builder("Try Azure for Free").title((s) -> tryAzureTitle);
        final BiConsumer<Void, AnActionEvent> tryAzureHandler = (Void v, AnActionEvent e) ->
            AzureActionManager.getInstance().getAction(OPEN_URL).handle(URL_TRY_AZURE_FOR_FREE);
        am.registerAction(IntellijAccountActionsContributor.TRY_AZURE, new Action<>(tryAzureHandler, tryAzureView).authRequired(false));
    }

    @Override
    public int getOrder() {
        return 2; //after azure resource common actions registered
    }
}
