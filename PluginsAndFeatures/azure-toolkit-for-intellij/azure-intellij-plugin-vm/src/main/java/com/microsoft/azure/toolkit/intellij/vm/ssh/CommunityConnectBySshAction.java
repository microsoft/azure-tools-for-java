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
import org.jetbrains.plugins.terminal.TerminalView;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Paths;

public class CommunityConnectBySshAction {
    private static final String SSH_TERMINAL_TABLE_NAME = "SSH - %s";
    private static final String CMD_SSH_KEY_PAIR = "ssh %s@%s -i %s";
    @AzureOperation(name = "vm.connect_virtual_machine_ssh_community", params = "vm.name()", type = AzureOperation.Type.TASK)
    public static void connectBySsh(VirtualMachine vm, @Nonnull Project project) {
        final String machineName = vm.getName();
        final String terminalTitle =  String.format(SSH_TERMINAL_TABLE_NAME, machineName);
        AzureTaskManager.getInstance().runInBackground(terminalTitle, () -> {
            // create a new terminal tab
            TerminalView terminalView = TerminalView.getInstance(project);
            ShellTerminalWidget shellTerminalWidget = terminalView.createLocalShellWidget(null, terminalTitle);
            try {
                // create ssh connection in terminal
                shellTerminalWidget.executeCommand(String.format(CMD_SSH_KEY_PAIR, vm.getAdminUserName(), vm.getHostIp(), getDefaultSshPrivateKeyPath()));
            } catch (IOException e) {
                AzureMessager.getMessager().error(e);
            }
        });
    }

    private static String getDefaultSshPrivateKeyPath() {
        return Paths.get(System.getProperty("user.home"), ".ssh", "id_rsa").toString();
    }
}
