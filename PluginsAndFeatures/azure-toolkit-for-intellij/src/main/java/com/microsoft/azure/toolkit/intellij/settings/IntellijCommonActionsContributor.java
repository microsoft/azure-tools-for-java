/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.settings;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

public class IntellijCommonActionsContributor implements IActionsContributor {

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiConsumer<Object, AnActionEvent> openSettingsHandler = (ignore, e) -> {
            final AzureString title = OperationBundle.description("user/common.open_azure_settings");
            final Project project = Optional.ofNullable(e).map(AnActionEvent::getProject).orElse(null);
            if (Objects.nonNull(project)) {
                AzureTaskManager.getInstance().runLater(new AzureTask<>(title, () -> openSettingsDialog(project)));
            } else {
                DataManager.getInstance().getDataContextFromFocusAsync().onSuccess(dataContext -> {
                    AzureTaskManager.getInstance().runLater(new AzureTask<>(title, () -> openSettingsDialog(dataContext.getData(CommonDataKeys.PROJECT))));
                }).onError(throwable -> {
                    final Project[] openProjects = ProjectManagerEx.getInstance().getOpenProjects();
                    AzureTaskManager.getInstance().runLater(new AzureTask<>(title, () -> openSettingsDialog(openProjects[0])));
                });
            }

        };
        am.registerHandler(ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS, (i, e) -> true, openSettingsHandler);
    }

    private static void openSettingsDialog(Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, AzureSettingsConfigurable.class);
    }
}