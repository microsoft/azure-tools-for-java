/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.ssh;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import javax.annotation.Nonnull;
import java.io.IOException;

public class ConnectUsingSshActionCommunityImpl implements ConnectUsingSshAction {
    private static final ConnectUsingSshAction instance = new ConnectUsingSshActionCommunityImpl();
    private static final String SSH_TERMINAL_TABLE_NAME = "%s";
    private static final String CMD_SSH_KEY_PAIR = "ssh %s@%s";

    public static ConnectUsingSshAction getInstance() {
        return instance;
    }

    public void connectBySsh(VirtualMachine vm, @Nonnull Project project) {
        final String machineName = vm.getName();
        AzureTaskManager.getInstance().runLater(() -> {
            // create a new terminal tab
            final TerminalToolWindowManager terminalManager = TerminalToolWindowManager.getInstance(project);
            final String terminalTitle =  String.format(SSH_TERMINAL_TABLE_NAME, machineName);
            final ShellTerminalWidget shellTerminalWidget = terminalManager.createLocalShellWidget(null, terminalTitle);
            try {
                // create ssh connection in terminal
                openTerminal(vm, shellTerminalWidget);
            } catch (final IOException e) {
                AzureMessager.getMessager().error(e);
            }
        });
    }

    @AzureOperation(name = "boundary/vm.create_ssh_session_ic")
    private static void openTerminal(VirtualMachine vm, ShellTerminalWidget shellTerminalWidget) throws IOException {
        shellTerminalWidget.executeCommand(String.format(CMD_SSH_KEY_PAIR, vm.getAdminUserName(), vm.getHostIp()));
    }

}
