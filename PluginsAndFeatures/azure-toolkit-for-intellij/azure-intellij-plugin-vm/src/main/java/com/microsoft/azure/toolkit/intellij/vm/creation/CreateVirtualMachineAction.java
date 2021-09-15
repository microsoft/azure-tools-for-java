/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.vm.creation;

import com.azure.resourcemanager.compute.models.KnownWindowsVirtualMachineImage;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.vm.creation.component.NetworkAvailabilityOptionsComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.compute.ip.DraftPublicIpAddress;
import com.microsoft.azure.toolkit.lib.compute.network.DraftNetwork;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureImage;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureVirtualMachineSize;
import com.microsoft.azure.toolkit.lib.compute.vm.DraftVirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.vm.task.CreateVirtualMachineTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

public class CreateVirtualMachineAction {

    public static final String REOPEN_CREATION_DIALOG = "Reopen Creation Dialog";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmss");

    public static void createVirtualMachine(Project project) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> showVirtualMachineCreationDialog(project, getDefaultConfiguration()));
    }

    private static void showVirtualMachineCreationDialog(final Project project, final DraftVirtualMachine draftVirtualMachine) {
        final VMCreationDialog dialog = new VMCreationDialog(project);
        dialog.setData(draftVirtualMachine);
        dialog.setOkActionListener((config) -> {
            dialog.close();
            createVirtualMachine(config, dialog);
        });
        dialog.show();
    }

    private static DraftVirtualMachine getDefaultConfiguration() {
        final String timestamp = DATE_FORMAT.format(new Date());
        final DraftVirtualMachine virtualMachine = new DraftVirtualMachine();
        virtualMachine.setRegion(Region.US_CENTRAL);
        virtualMachine.setAvailabilitySet(NetworkAvailabilityOptionsComboBox.DISABLE);
        virtualMachine.setImage(new AzureImage(KnownWindowsVirtualMachineImage.WINDOWS_DESKTOP_10_20H1_PRO));
        virtualMachine.setSize(new AzureVirtualMachineSize("Standard_DS1_v2"));
        final DraftNetwork draftNetwork = new DraftNetwork();
        draftNetwork.setName(String.format("network-%s", timestamp));
        draftNetwork.setAddressSpace("10.0.2.0/24");
        draftNetwork.setSubnet("default");
        draftNetwork.setSubnetAddressSpace("10.0.2.0/24");
        virtualMachine.setNetwork(draftNetwork);
        final DraftPublicIpAddress publicIpAddress = new DraftPublicIpAddress();
        publicIpAddress.setName(String.format("public-ip-%s", timestamp));
        virtualMachine.setIpAddress(publicIpAddress);
        return virtualMachine;
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
