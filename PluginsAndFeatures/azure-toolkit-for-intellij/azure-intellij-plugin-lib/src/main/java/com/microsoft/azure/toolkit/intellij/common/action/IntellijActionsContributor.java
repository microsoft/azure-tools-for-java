/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.properties.IntellijShowPropertiesViewAction;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;

import java.util.Objects;
import java.util.function.BiConsumer;

public class IntellijActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        am.registerHandler(ResourceCommonActionsContributor.OPEN_URL, Objects::nonNull, BrowserUtil::browse);
        am.<AzResourceBase, AnActionEvent>registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES,
            (s, e) -> Objects.nonNull(s) && Objects.nonNull(e.getProject()),
            (s, e) -> IntellijShowPropertiesViewAction.showPropertyView(s, Objects.requireNonNull(e.getProject())));

        final BiConsumer<Object, AnActionEvent> highlightResource = (r, e) -> {
            AzureEventBus.emit("azure.explorer.highlight_resource", r);
            AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_AZURE_EXPLORER).handle(null, e);
        };
        am.registerHandler(ResourceCommonActionsContributor.HIGHLIGHT_RESOURCE_IN_EXPLORER, (s, e) -> Objects.nonNull(s) && Objects.nonNull(e.getProject()), highlightResource);
    }

    @Override
    public int getOrder() {
        return ResourceCommonActionsContributor.INITIALIZE_ORDER + 1; //after azure resource common actions registered
    }
}
