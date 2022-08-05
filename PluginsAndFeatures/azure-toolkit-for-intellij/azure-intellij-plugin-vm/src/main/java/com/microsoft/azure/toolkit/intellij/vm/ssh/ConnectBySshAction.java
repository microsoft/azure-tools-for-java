/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.ssh;

import com.intellij.openapi.project.Project;
import com.intellij.ssh.config.unified.SshConfig;
import com.intellij.ssh.config.unified.SshConfigManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalView;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

/**
 * connect to selected machine by SSH in Intellij Ultimate.
 */
public class ConnectBySshAction {
    private static final String SSH_TERMINAL_TABLE_NAME = "SSH - %s";
    private static final String CMD_SSH = "ssh %s@%s";
    private static final String CMD_SSH_KEY_PAIR = "ssh %s@%s -i %s";

    public static void connectBySsh(VirtualMachine vm, @Nonnull Project project) {
        final String machineName = vm.getName();
        final SshConfig existingConfigs = SshConfigManager.getInstance(project)
                .findConfigByName(String.format("Azure: %s", machineName));
        final String userName = existingConfigs != null ? existingConfigs.getUsername() : vm.getAdminUserName();
        final String hostIp = existingConfigs != null ?  existingConfigs.getHost() : vm.getHostIp();
        final String privateKeyPath = defaultSshPrivateKeyPath();
        AzureTaskManager.getInstance().runAndWait(() -> {
            // create a new terminal tab
            TerminalView terminalView = TerminalView.getInstance(project);
            ShellTerminalWidget shellTerminalWidget = terminalView.createLocalShellWidget(null,
                    String.format(SSH_TERMINAL_TABLE_NAME, machineName));
            final AzureString messageTitle = description("webapp.open_ssh.app", machineName);
            AzureTaskManager.getInstance().runInBackground(new AzureTask(project, messageTitle, false, () -> {
                try {
                    // create ssh connection in terminal
                    if (existingConfigs != null) {
                        shellTerminalWidget.executeCommand(String.format(CMD_SSH, userName,
                                hostIp));
                    } else {
                        shellTerminalWidget.executeCommand(String.format(CMD_SSH_KEY_PAIR, userName,
                                hostIp, privateKeyPath));
                    }
                } catch (IOException e) {
                    AzureMessager.getMessager().error(e);
                }
            }));
        }, AzureTask.Modality.ANY);
    }

    private static String defaultSshPrivateKeyPath() {
        Path userHomePath = Paths.get(System.getProperty("user.home"));
        Path filePath = Paths.get(userHomePath.toString(), ".ssh", "id_rsa");
        return filePath.toString();
    }
}
