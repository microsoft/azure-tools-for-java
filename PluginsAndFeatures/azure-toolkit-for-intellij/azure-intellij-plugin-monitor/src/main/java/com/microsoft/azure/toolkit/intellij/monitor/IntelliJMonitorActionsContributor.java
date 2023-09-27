/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.monitor.AzureLogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;

import java.util.Objects;

public class IntelliJMonitorActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        am.registerHandler(ResourceCommonActionsContributor.OPEN_MONITOR, (Object o, AnActionEvent e) -> true,
            (Object o, AnActionEvent e) -> {
                final Account account = Azure.az(AzureAccount.class).account();
                final LogAnalyticsWorkspace defaultWorkspace = account.getSelectedSubscriptions().stream()
                    .flatMap(s -> Azure.az(AzureLogAnalyticsWorkspace.class).logAnalyticsWorkspaces(s.getId()).list().stream())
                    .findFirst().orElse(null);
                AzureTaskManager.getInstance().runLater(()-> AzureMonitorManager.getInstance().openMonitorWindow(Objects.requireNonNull(e.getProject()), defaultWorkspace, null));
            });
    }

    @Override
    public int getOrder() {
        return ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    }
}
