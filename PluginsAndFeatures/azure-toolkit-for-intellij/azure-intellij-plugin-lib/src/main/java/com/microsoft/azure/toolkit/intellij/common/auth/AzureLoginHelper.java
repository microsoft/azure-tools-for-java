/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.common.auth;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class AzureLoginHelper {

    private static final String NEED_SIGN_IN = "Please sign in with your Azure account.";
    private static final String NO_SUBSCRIPTION = "No subscription in current account, you may get a free one from "
            + "https://azure.microsoft.com/en-us/free/";
    public static final String MUST_SELECT_SUBSCRIPTION =
            "Please select at least one subscription first (Tools -> Azure -> Select Subscriptions)";

    public static void ensureAzureSubsAvailable() throws AzureExecutionException {
        try {
            final Account account = Azure.az(AzureAccount.class).account();
            final List<Subscription> subscriptions = account.getSubscriptions();
            if (CollectionUtils.isEmpty(subscriptions)) {
                throw new AzureExecutionException(NO_SUBSCRIPTION);
            }
            final List<Subscription> selectedSubscriptions = account.getSelectedSubscriptions();
            if (CollectionUtils.isEmpty(selectedSubscriptions)) {
                throw new AzureExecutionException(MUST_SELECT_SUBSCRIPTION);
            }
        } catch (AzureToolkitAuthenticationException ex) {
            throw new AzureExecutionException(ex.getMessage(), ex);
        }
    }

    public static boolean isAzureSubsAvailableOrReportError(String dialogTitle) {
        try {
            AzureLoginHelper.ensureAzureSubsAvailable();
            return true;
        } catch (AzureExecutionException e) {
            AzureMessager.getMessager().error(e.getMessage());
            return false;
        }
    }

    public static void requireSignedIn(Project project, Consumer<IAccount> consumer) {
        // Todo(andxu): legacy code shall be deleted later.
        final Action<Consumer<IAccount>> requireAuth = AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH);
        final AnActionEvent event = AnActionEvent.createEvent(ActionManager.getInstance().getAction("AzureToolkit.AzureSignIn"),
                SimpleDataContext.getSimpleContext(CommonDataKeys.PROJECT, project), new Presentation(), "not_used", ActionUiKind.NONE, null);
        if (Objects.nonNull(requireAuth)) {
            requireAuth.handle(consumer, event);
        }
    }
}
