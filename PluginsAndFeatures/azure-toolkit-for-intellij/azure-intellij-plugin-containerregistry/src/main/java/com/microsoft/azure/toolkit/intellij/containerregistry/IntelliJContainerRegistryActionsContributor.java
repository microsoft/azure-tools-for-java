/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.containerregistry.ContainerRegistryActionsContributor;
import com.microsoft.azure.toolkit.intellij.containerregistry.actions.OpenContainerRegistryPropertyViewAction;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntelliJContainerRegistryActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<AzResourceBase, AnActionEvent> condition = (r, e) -> r instanceof ContainerRegistry;
        final BiConsumer<AzResourceBase, AnActionEvent> handler = (c, e) ->
                OpenContainerRegistryPropertyViewAction.openContainerRegistryPropertyView((ContainerRegistry) c, e.getProject());
        am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, condition, handler);
    }

    @Override
    public int getOrder() {
        return ContainerRegistryActionsContributor.INITIALIZE_ORDER + 1;
    }
}
