/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.compute.vm.DraftVirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.vm.task.CreateVirtualMachineTask;

import java.util.function.Consumer;

public class CreateVirtualMachineAction {

    public static final String REOPEN_CREATION_DIALOG = "Reopen Creation Dialog";

    public static void createVirtualMachine(Project project) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> showVirtualMachineCreationDialog(project, DraftVirtualMachine.getDefaultVirtualMachineDraft()));
    }

    private static void showVirtualMachineCreationDialog(final Project project, final DraftVirtualMachine draftVirtualMachine) {
        final VMCreationDialog dialog = new VMCreationDialog(project);
        dialog.setValue(draftVirtualMachine);
        dialog.setOkActionListener((config) -> {
            dialog.close();
            createVirtualMachine(config, dialog);
        });
        dialog.show();
    }

    public static void createVirtualMachine(final DraftVirtualMachine draft, final VMCreationDialog dialog) {
        AzureTaskManager.getInstance().runInBackground(AzureOperationBundle.title("vm.create", draft.getName()), () -> {
            AzureTelemetry.getActionContext().setProperty("subscriptionId", draft.getSubscriptionId());
            try {
                new CreateVirtualMachineTask(draft).execute();
            } catch (final Exception e) {
                final Consumer<Object> act = t -> AzureTaskManager.getInstance().runLater("open dialog",
                        () -> showVirtualMachineCreationDialog(dialog.getProject(), draft));
                final Action<?> action = new Action<>(act, new ActionView.Builder(REOPEN_CREATION_DIALOG));
                AzureMessager.getMessager().error(e, null, action);
            }
        });
    }
}
