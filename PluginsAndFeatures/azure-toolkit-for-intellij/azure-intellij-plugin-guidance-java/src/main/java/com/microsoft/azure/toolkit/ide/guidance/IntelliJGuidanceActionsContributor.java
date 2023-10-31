/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guidance;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;

import java.util.Objects;

public class IntelliJGuidanceActionsContributor implements IActionsContributor {

    @Override
    public void registerHandlers(AzureActionManager am) {
        am.registerHandler(ResourceCommonActionsContributor.SHOW_COURSES, (Object o, AnActionEvent e) -> true,
            (Object o, AnActionEvent e) -> GuidanceViewManager.getInstance().showCoursesView(Objects.requireNonNull(e.getProject())));
    }

    @Override
    public int getOrder() {
        return ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    }
}
