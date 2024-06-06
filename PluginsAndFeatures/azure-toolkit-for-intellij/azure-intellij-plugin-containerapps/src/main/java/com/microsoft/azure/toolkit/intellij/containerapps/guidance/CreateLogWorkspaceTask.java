/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.guidance;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Phase;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspaceDraft;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Optional;

public class CreateLogWorkspaceTask implements Task {
    public static final String LOG_ANALYTICS_WORKSPACE = "logAnalyticsWorkspace";
    private final ComponentContext context;
    @Getter
    private boolean toSkip;

    public CreateLogWorkspaceTask(final ComponentContext context) {
        this.context = context;
    }

    @Override
    public void prepare() {
        final Optional<LogAnalyticsWorkspace> contextWorkspace = Optional.ofNullable((LogAnalyticsWorkspace) context.getParameter(LOG_ANALYTICS_WORKSPACE));
        this.toSkip = contextWorkspace.isEmpty() || contextWorkspace.filter(rg -> !rg.isDraftForCreating()).isPresent();
        if (!this.toSkip) {
            final Phase currentPhase = context.getCourse().getCurrentPhase();
            currentPhase.expandPhasePanel();
        }
    }

    @Override
    @AzureOperation(name = "internal/guidance.create_log_analytics_workspace")
    public void execute() {
        final Optional<LogAnalyticsWorkspace> contextWorkspace = Optional.ofNullable((LogAnalyticsWorkspace) context.getParameter(LOG_ANALYTICS_WORKSPACE));
        if (contextWorkspace.isPresent() && contextWorkspace.get() instanceof LogAnalyticsWorkspaceDraft workspaceDraft) {
            workspaceDraft.createIfNotExist();
            context.applyResult(LOG_ANALYTICS_WORKSPACE, contextWorkspace);
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.common.create_log_analytics_workspace";
    }
}
