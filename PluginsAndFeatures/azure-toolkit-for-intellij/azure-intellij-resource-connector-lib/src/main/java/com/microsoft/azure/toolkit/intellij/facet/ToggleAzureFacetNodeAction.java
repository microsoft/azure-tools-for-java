/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.action.Action.EMPTY_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class ToggleAzureFacetNodeAction extends AnAction {
    @Override
    @AzureOperation(name = "user/connector.show_azure_node")
    public void actionPerformed(@Nonnull AnActionEvent event) {
        OperationContext.current().setTelemetryProperty(PLACE, StringUtils.firstNonBlank(event.getPlace(), EMPTY_PLACE));
        final Module module = getModule(event);
        if (Objects.nonNull(module) && !module.getProject().isDisposed()) {
            final Project project = Objects.requireNonNull(event.getProject());
            final PropertiesComponent properties = PropertiesComponent.getInstance(project);
            properties.setValue(module.getName() + ".azure", isAzureNodeShown(module) ? "hide" : "show");
            Optional.of(module.getProject())
                .map(ProjectView::getInstance)
                .map(ProjectView::getCurrentProjectViewPane)
                .ifPresent(p -> p.updateFromRoot(true));
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        final Module module = getModule(e);
        final Presentation presentation = e.getPresentation();
        if (Objects.isNull(module)) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        presentation.setEnabledAndVisible(true);
        if (isAzureNodeShown(module)) {
            presentation.setText("Hide 'Azure' Node");
        } else {
            presentation.setText("Show 'Azure' Node");
        }
    }

    private static boolean isAzureNodeShown(final Module module) {
        final AzureModule azureModule = AzureModule.from(module);
        final Boolean state = azureModule.getAzureFacetState();
        final boolean forceShow = BooleanUtils.isTrue(state);
        final boolean forceHide = BooleanUtils.isFalse(state);
        final boolean defaultShow = state == null && (azureModule.hasAzureFacet() || azureModule.isInitialized() || azureModule.hasAzureDependencies());
        return forceShow || defaultShow;
    }

    @Nullable
    private static Module getModule(final @Nonnull AnActionEvent e) {
        Module module = LangDataKeys.MODULE.getData(e.getDataContext());
        final Object[] selected = PlatformDataKeys.SELECTED_ITEMS.getData(e.getDataContext());
        final boolean onDotAzure = Objects.nonNull(selected) && selected.length == 1 && (selected[0] instanceof PsiDirectoryNode) && ".azure".equalsIgnoreCase(((PsiDirectoryNode)selected[0]).getValue().getName());
        if (Objects.isNull(module) && onDotAzure && Objects.nonNull(e.getProject())) {
            module = ModuleUtil.findModuleForFile(((PsiDirectoryNode) selected[0]).getValue().getVirtualFile(), e.getProject());
        }
        return module;
    }
}
