/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.vm.creation.VMCreationDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.compute.AzureCompute;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class VirtualMachineComboBox extends AzureComboBox<VirtualMachine> {
    private final List<VirtualMachine> draftItems = new ArrayList<>();
    private Subscription subscription;

    @Override
    protected String getItemText(Object item) {
        return item instanceof VirtualMachine ? ((VirtualMachine) item).getName() : super.getItemText(item);
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

//    @Nullable
//    @Override
//    protected ExtendableTextComponent.Extension getExtension() {
//        return ExtendableTextComponent.Extension.create(
//            AllIcons.General.Add, AzureMessageBundle.message("vm.create.tooltip").toString(), this::showVirtualMachineCreationPopup);
//    }

    @Nonnull
    @Override
    protected List<? extends VirtualMachine> loadItems() {
        final List<VirtualMachine> vms = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(this.draftItems)) {
            vms.addAll(new ArrayList<>(this.draftItems));
        }
        if (Objects.nonNull(subscription)) {
            final List<VirtualMachine> remoteVms = Azure.az(AzureCompute.class)
                .virtualMachines(subscription.getId()).list().stream()
                .sorted(Comparator.comparing(VirtualMachine::getName)).collect(Collectors.toList());
            remoteVms.parallelStream().forEach(VirtualMachine::getHostIp); // pre-load host ip
            vms.addAll(remoteVms);
        }
        return vms;
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(this.subscription).ifPresent(s -> Azure.az(AzureCompute.class).virtualMachines(s.getId()).refresh());
        super.refreshItems();
    }

    private void showVirtualMachineCreationPopup() {
        final VMCreationDialog dialog = new VMCreationDialog(null);
        dialog.setOkActionListener((vm) -> {
            this.draftItems.add(0, vm);
            dialog.close();
            final List<VirtualMachine> items = new ArrayList<>(this.getItems());
            items.add(0, vm);
            this.setItems(items);
            this.setValue(vm);
        });
        dialog.show();
    }
}
