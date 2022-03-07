/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.vm;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIconProvider;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.compute.AbstractAzureResource;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureVirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.vm.VirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.vm.model.OperatingSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class VirtualMachineNodeProvider implements IExplorerNodeProvider {
    public static final AzureIconProvider<VirtualMachine> VIRTUAL_MACHINE_ICON_PROVIDER =
            new AzureResourceIconProvider<VirtualMachine>().withModifier(VirtualMachineNodeProvider::getOperatingSystemModifier);

    private static final String NAME = "Virtual Machines";
    private static final String ICON = "/icons/virtualmachine.svg";

    @Nullable
    @Override
    public Object getRoot() {
        return Azure.az(AzureVirtualMachine.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent) {
        return data instanceof AzureVirtualMachine ||
            data instanceof VirtualMachine;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureVirtualMachine) {
            final AzureVirtualMachine service = (AzureVirtualMachine) data;
            return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                .actions(VirtualMachineActionsContributor.SERVICE_ACTIONS)
                .addChildren(this::listVirtualMachines, (vm, vmNode) -> this.createNode(vm, vmNode, manager));
        } else if (data instanceof VirtualMachine) {
            final VirtualMachine vm = (VirtualMachine) data;
            return new Node<>(vm)
                .view(new AzureResourceLabelView<>(vm, VirtualMachine::getStatus, VIRTUAL_MACHINE_ICON_PROVIDER))
                .actions(VirtualMachineActionsContributor.VM_ACTIONS);
        }
        return null;
    }


    @Nullable
    private static AzureIcon.Modifier getOperatingSystemModifier(VirtualMachine resource) {
        return resource.getOperatingSystem() != OperatingSystem.Windows ? AzureIcon.Modifier.LINUX : null;
    }

    @Nonnull
    private List<VirtualMachine> listVirtualMachines(AzureVirtualMachine s) {
        return s.list().stream().sorted(Comparator.comparing(AbstractAzureResource::name)).collect(Collectors.toList());
    }
}
