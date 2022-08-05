/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.ssh;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.remote.AuthType;
import com.intellij.ssh.config.unified.SshConfig;
import com.intellij.ssh.config.unified.SshConfigManager;
import com.intellij.ssh.ui.unified.SshConfigConfigurable;
import com.intellij.ssh.ui.unified.SshUiData;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalView;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/**
 * connect to selected machine by SSH in Intellij Ultimate.
 */
public class ConnectBySshAction {
    private static final String SSH_TERMINAL_TABLE_NAME = "SSH - %s";
    private static final String CMD_SSH = "ssh %s@%s";
    private static final String CMD_SSH_KEY_PAIR = "ssh %s@%s -i %s";

    @AzureOperation(name = "vm.connect_virtual_machine_ssh_ultimate", params = "vm.name()", type = AzureOperation.Type.TASK)
    public static void connectBySshUltimate(VirtualMachine vm, @Nonnull Project project) {
        String configKey = String.format("Azure: %s", vm.getName());
        SshConfigConfigurable configurable = new SshConfigConfigurable.Main(project);
        SshConfigManager manager = SshConfigManager.getInstance(project);
        SshConfig existingConfigs = manager.findConfigByName(configKey);
        if (Objects.isNull(existingConfigs)) {
            // add default config
            String name = String.format("Azure: %s", vm.name());
            SshUiData uiData = new SshUiData(toSshConfig(vm, name), true);
            SshConfigManager.ConfigsData newConfigs = new SshConfigManager.ConfigsData(Collections.emptyList(), Collections.singletonList(uiData));
            SshConfigManager.ConfigsData merged = manager.getLastSavedAndCurrentData().createMerged(newConfigs);
            try {
                MethodUtils.invokeMethod(configurable, true, "resetFromData", merged);
                configurable.apply();
            } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                           ConfigurationException e) {
                AzureMessager.getMessager().error(e);
            }
            existingConfigs = toSshConfig(vm, name);
        }
        if (Objects.isNull(existingConfigs)) {
            // create config failed, use default key-pair
            connectBySshCommunity(vm, project);
        } else {
            // todo intellij action OpenRemoteConnectionAction
        }
    }

    @AzureOperation(name = "vm.connect_virtual_machine_ssh_community", params = "vm.name()", type = AzureOperation.Type.TASK)
    public static void connectBySshCommunity(VirtualMachine vm, @Nonnull Project project) {
        final String machineName = vm.getName();
        final String userName = vm.getAdminUserName();
        final String hostIp = vm.getHostIp();
        final String privateKeyPath = getDefaultSshPrivateKeyPath();
        AzureTaskManager.getInstance().runAndWait(() -> {
            // create a new terminal tab
            TerminalView terminalView = TerminalView.getInstance(project);
            String terminalTitle =  String.format(SSH_TERMINAL_TABLE_NAME, machineName);
            ShellTerminalWidget shellTerminalWidget = terminalView.createLocalShellWidget(null, terminalTitle);
            AzureTaskManager.getInstance().runInBackground(new AzureTask(project, terminalTitle, false, () -> {
                try {
                    // create ssh connection in terminal
                    shellTerminalWidget.executeCommand(String.format(CMD_SSH_KEY_PAIR, userName, hostIp, privateKeyPath));
                } catch (IOException e) {
                    AzureMessager.getMessager().error(e);
                }
            }));
        }, AzureTask.Modality.ANY);
    }

    private static String getDefaultSshPrivateKeyPath() {
        return Paths.get(System.getProperty("user.home"), ".ssh", "id_rsa").toString();
    }

    @Nonnull
    private static SshConfig toSshConfig(VirtualMachine vm, String name) {
        final SshConfig config = new SshConfig(true);
        config.setCustomName(name);
        config.setId(UUID.nameUUIDFromBytes(name.getBytes()).toString());
        config.setUsername(vm.getAdminUserName());
        config.setAuthType(AuthType.KEY_PAIR);
        config.setHost(vm.getHostIp());
        config.setKeyPath(getDefaultSshPrivateKeyPath());
        return config;
    }
}
