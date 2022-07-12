/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.IAccountActions;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.intellij.AzureConfigurable;
import com.microsoft.intellij.actions.AzureSignInAction;
import com.microsoft.intellij.actions.SelectSubscriptionsAction;
import com.microsoft.intellij.ui.ServerExplorerToolWindowFactory;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

public class LegacyIntellijAccountActionsContributor implements IActionsContributor {
    @Override
    public void registerActions(AzureActionManager am) {
        final AzureString authzTitle = OperationBundle.description("account.authorize_action");
        final ActionView.Builder authzView = new ActionView.Builder("Authorize").title((s) -> authzTitle);
        final BiConsumer<Runnable, AnActionEvent> authzHandler = (Runnable r, AnActionEvent e) ->
            AzureSignInAction.requireSignedIn(Optional.ofNullable(e).map(AnActionEvent::getProject).orElse(null), r);
        am.registerAction(Action.REQUIRE_AUTH, new Action<>(Action.REQUIRE_AUTH, authzHandler, authzView).setAuthRequired(false));

        final AzureString authnTitle = OperationBundle.description("account.authenticate");
        final ActionView.Builder authnView = new ActionView.Builder("Sign in").title((s) -> authnTitle);
        final BiConsumer<Object, AnActionEvent> authnHandler = (Object v, AnActionEvent e) -> {
            final AzureAccount az = Azure.az(AzureAccount.class);
            if (az.isLoggedIn()) az.logout();
            AzureSignInAction.authActionPerformed(e.getProject());
        };
        am.registerAction(Action.AUTHENTICATE, new Action<>(Action.AUTHENTICATE, authnHandler, authnView).setAuthRequired(false));

        final ActionView.Builder selectSubsView = new ActionView.Builder("Select Subscriptions", AzureIcons.Action.SELECT_SUBSCRIPTION.getIconPath())
            .title((s) -> authnTitle);
        final BiConsumer<Object, AnActionEvent> selectSubsHandler = (Object v, AnActionEvent e) ->
            SelectSubscriptionsAction.selectSubscriptions(e.getProject());
        am.registerAction(IAccountActions.SELECT_SUBS, new Action<>(IAccountActions.SELECT_SUBS, selectSubsHandler, selectSubsView).setAuthRequired(true));
    }

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiConsumer<Object, AnActionEvent> openSettingsHandler = (ignore, e) -> {
            final Project project = Optional.ofNullable(e).map(AnActionEvent::getProject).orElseGet(() -> {
                final Project[] openProjects = ProjectManagerEx.getInstance().getOpenProjects();
                return ArrayUtils.isEmpty(openProjects) ? null : openProjects[0];
            });
            final AzureString title = OperationBundle.description("common.open_azure_settings");
            AzureTaskManager.getInstance().runLater(new AzureTask<>(title, () ->
                ShowSettingsUtil.getInstance().showSettingsDialog(project, AzureConfigurable.class)));
        };
        am.registerHandler(ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS, (i, e) -> true, openSettingsHandler);

        final BiConsumer<Object, AnActionEvent> openAzureExplorer = (ignore, e) -> {
            final ToolWindow toolWindow = ToolWindowManager.getInstance(Objects.requireNonNull(e.getProject())).
                getToolWindow(ServerExplorerToolWindowFactory.EXPLORER_WINDOW);
            if (Objects.nonNull(toolWindow) && !toolWindow.isVisible()) {
                AzureTaskManager.getInstance().runLater(toolWindow::show);
            }
        };
        am.registerHandler(ResourceCommonActionsContributor.OPEN_AZURE_EXPLORER, (i, e) -> true, openAzureExplorer);
    }
}