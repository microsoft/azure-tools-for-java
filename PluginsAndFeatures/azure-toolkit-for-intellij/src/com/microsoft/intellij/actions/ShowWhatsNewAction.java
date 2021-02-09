/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.intellij.AzureAnAction;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.helpers.WhatsNewManager;
import com.microsoft.intellij.util.PluginUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.SHOW_WHATS_NEW;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SYSTEM;

public class ShowWhatsNewAction extends AzureAnAction {

    private static final String FAILED_TO_LOAD_WHATS_NEW = "Failed to load what's new document";

    @Override
    @AzureOperation(name = "common.load_whatsnew", type = AzureOperation.Type.ACTION)
    public boolean onActionPerformed(@NotNull final AnActionEvent anActionEvent, @Nullable final Operation operation) {
        final Project project = anActionEvent.getProject();
        try {
            WhatsNewManager.INSTANCE.showWhatsNew(true, project);
        } catch (Exception e) {
            EventUtil.logError(operation, ErrorType.systemError, e, null, null);
            PluginUtil.showInfoNotificationProject(project, FAILED_TO_LOAD_WHATS_NEW, e.getMessage());
        }
        return true;
    }

    protected String getServiceName(AnActionEvent event) {
        return SYSTEM;
    }

    protected String getOperationName(AnActionEvent event) {
        return SHOW_WHATS_NEW;
    }
}
