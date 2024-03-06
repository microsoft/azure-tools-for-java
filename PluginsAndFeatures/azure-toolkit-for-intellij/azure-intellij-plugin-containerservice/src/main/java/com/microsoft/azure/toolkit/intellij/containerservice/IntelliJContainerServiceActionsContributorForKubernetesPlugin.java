/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerservice;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.containerservice.ContainerServiceActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesCluster;

import java.util.function.BiPredicate;

import static com.microsoft.azure.toolkit.intellij.containerservice.actions.OpenKubernetesPluginAction.selectKubernetesInKubernetesPlugin;

public class IntelliJContainerServiceActionsContributorForKubernetesPlugin implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<KubernetesCluster, AnActionEvent> clusterCondition = (r, e) -> r != null;
        am.registerHandler(ContainerServiceActionsContributor.OPEN_KUBERNETES_PLUGIN, clusterCondition, (c, e) ->
                AzureTaskManager.getInstance().runLater(() -> selectKubernetesInKubernetesPlugin(c, e.getProject())));
    }
}
