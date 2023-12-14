/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.samples;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.util.ui.cloneDialog.VcsCloneDialog;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.samples.view.AzureSamplesCloneDialogExtension;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import java.util.Objects;

public class IntelliJAzureSamplesActionsContributor implements IActionsContributor {
    @Override
    public void registerActions(final AzureActionManager am) {
        new Action<>(ResourceCommonActionsContributor.BROWSE_AZURE_SAMPLES)
            .withLabel("Browse Azure samples")
            .withHandler((c, e) -> AzureTaskManager.getInstance().runLater(() -> {
                final AnActionEvent event = (AnActionEvent) e;
                new VcsCloneDialog.Builder(Objects.requireNonNull(event.getProject()))
                    .forExtension(AzureSamplesCloneDialogExtension.class).show();
            }))
            .withAuthRequired(false)
            .register(am);

    }
}
