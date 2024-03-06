/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.containerservice.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.containerservice.ContainerServiceActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesCluster;

import javax.annotation.Nonnull;

public class KubernetesAddContextFromAzureAction extends AnAction {
    @Override
    public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
        final AddContextDialog dialog = new AddContextDialog(anActionEvent.getProject());
        dialog.setOkActionListener(cluster -> {
            dialog.close();
            final Action<KubernetesCluster> action = AzureActionManager.getInstance().getAction(ContainerServiceActionsContributor.OPEN_KUBERNETES_PLUGIN);
            action.handle(cluster, anActionEvent);
        });
        dialog.show();
    }
}
