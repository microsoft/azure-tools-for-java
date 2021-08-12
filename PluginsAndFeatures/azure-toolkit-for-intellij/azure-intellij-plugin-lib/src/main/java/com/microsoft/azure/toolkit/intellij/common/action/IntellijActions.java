/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActions;
import com.microsoft.azure.toolkit.intellij.common.properties.IntellijShowPropertiesViewAction;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;

import java.util.Objects;

public class IntellijActions implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        am.<String>registerHandler(ResourceCommonActions.OPEN_URL, Objects::nonNull, BrowserUtil::browse);
        am.<IAzureResource<?>, AnActionEvent>registerHandler(ResourceCommonActions.SHOW_PROPERTIES,
                (s, e) -> Objects.nonNull(s) && Objects.nonNull(e.getProject()),
                (s, e) -> IntellijShowPropertiesViewAction.showPropertyView(s, Objects.requireNonNull(e.getProject())));
    }

    @Override
    public int getZOrder() {
        return 2; //after azure resource common actions registered
    }
}
