/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.vm.VirtualMachineActionsContributor;
import com.microsoft.azure.toolkit.intellij.vm.ssh.AddSshConfigAction;
import com.microsoft.azure.toolkit.intellij.vm.ssh.BrowseRemoteHostSftpAction;
import com.microsoft.azure.toolkit.intellij.vm.ssh.ConnectUsingSshActionUltimateImpl;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;

import java.util.Objects;
import java.util.function.BiConsumer;

public class IntelliJVMActionsContributorForUltimate implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiConsumer<VirtualMachine, AnActionEvent> addSshConfigHandler = (c, e) -> AddSshConfigAction
            .addSshConfig(c, Objects.requireNonNull(e.getProject()));
        am.registerHandler(VirtualMachineActionsContributor.ADD_SSH_CONFIG, (c, e) -> c instanceof VirtualMachine, addSshConfigHandler);

        final BiConsumer<VirtualMachine, AnActionEvent> connectBySshHandler = (c, e) ->
                ConnectUsingSshActionUltimateImpl.getInstance().connectBySsh(c, Objects.requireNonNull(e.getProject()));
        am.registerHandler(VirtualMachineActionsContributor.CONNECT_SSH,  (c, e) -> c instanceof VirtualMachine, connectBySshHandler);

        final BiConsumer<VirtualMachine, AnActionEvent> sftpHandler = (c, e) -> BrowseRemoteHostSftpAction
                .browseRemoteHost(c, Objects.requireNonNull(e.getProject()));
        am.registerHandler(VirtualMachineActionsContributor.SFTP_CONNECTION, (c, e) -> c instanceof VirtualMachine, sftpHandler);
    }

    @Override
    public int getOrder() {
        return VirtualMachineActionsContributor.INITIALIZE_ORDER + 1;
    }
}
