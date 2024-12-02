/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.dbtools;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.Objects;

public abstract class NoResourceTipLabel extends HyperlinkLabel {
    public NoResourceTipLabel(@Nonnull String text) {
        super();

        setHtmlText(text);
        setIcon(AllIcons.General.Information);
        addHyperlinkListener(e -> createResourceInIde(e.getInputEvent()));
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    protected void createResourceInIde(InputEvent e) {
        final DataContext context = DataManager.getInstance().getDataContext(e.getComponent());
        final Project project = context.getData(CommonDataKeys.PROJECT);
        final Window window = ComponentUtil.getActiveWindow();
        window.setVisible(false);
        window.dispose();
        final ToolWindow explorer = ToolWindowManager.getInstance(Objects.requireNonNull(project)).getToolWindow("Azure Explorer");
        Objects.requireNonNull(explorer).activate(() -> {
            final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), e, "database.dbtools", context);
            final Class<? extends AzService> clazz = getClazzForNavigationToExplorer();
            if (clazz == null) return;
            AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.SELECT_RESOURCE_IN_EXPLORER).handle(Azure.az(clazz), event);
            createResource(project);
        });
    }

    @Nullable
    protected abstract Class<? extends AzService> getClazzForNavigationToExplorer();
    protected abstract void createResource(Project project);
}
