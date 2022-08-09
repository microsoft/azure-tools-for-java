/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.ssh;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.remote.AuthType;
import com.intellij.remote.RemoteCredentials;
import com.intellij.remote.RemoteSdkException;
import com.intellij.ssh.config.unified.SshConfig;
import com.intellij.ssh.config.unified.SshConfigManager;
import com.intellij.ssh.ui.unified.SshConfigConfigurable;
import com.intellij.ssh.ui.unified.SshUiData;
import com.intellij.ui.AppUIUtil;
import com.jetbrains.plugins.remotesdk.console.SshConsoleOptionsProvider;
import com.jetbrains.plugins.remotesdk.console.SshTerminalCachingRunner;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.TerminalView;

import javax.annotation.Nonnull;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/**
 * connect to selected machine by SSH in Intellij Ultimate.
 */
public class UltimateConnectBySshAction {
    private static final String SSH_CONNECTION_TITLE = "Connect using SSH";

    @AzureOperation(name = "vm.connect_virtual_machine_ssh_ultimate", params = "vm.name()", type = AzureOperation.Type.TASK)
    public static void connectBySsh(VirtualMachine vm, @Nonnull Project project) {
        final String configKey = String.format("Azure: %s", vm.getName());
        final SshConfigConfigurable configurable = new SshConfigConfigurable.Main(project);
        final SshConfigManager manager = SshConfigManager.getInstance(project);
        SshConfig existingConfig = manager.findConfigByName(configKey);
        if (Objects.isNull(existingConfig)) {
            // add default config
            final SshConfig defaultConfig = toSshConfig(vm, String.format("Azure: %s", vm.getName()));
            final SshUiData uiData = new SshUiData(defaultConfig, true);
            final SshConfigManager.ConfigsData newConfigs = new SshConfigManager.ConfigsData(Collections.emptyList(), Collections.singletonList(uiData));
            final SshConfigManager.ConfigsData merged = manager.getLastSavedAndCurrentData().createMerged(newConfigs);
            manager.applyData(merged.getCurrentData(), new SshConfigManager.Listener() {
                @Override
                public void sshConfigsChanged() {
                    SshConfigManager.Listener.super.sshConfigsChanged();
                }
            });
            existingConfig = defaultConfig;
        }
        openPredefinedTerminalSshSessionAction(existingConfig, project);
    }

    private static void openPredefinedTerminalSshSessionAction(SshConfig existingConfig, Project project) {
        final SshConsoleOptionsProvider provider = SshConsoleOptionsProvider.getInstance(project);
        final RemoteCredentials sshCredential = new SshUiData(existingConfig, true);
        final SshTerminalCachingRunner runner = new SshTerminalCachingRunner(project, sshCredential, provider.getCharset());
        AzureTaskManager.getInstance().runInBackground(SSH_CONNECTION_TITLE,() ->
                connectToSshUnderProgress(project, runner, sshCredential)
        );
    }

    private static void connectToSshUnderProgress(final @NotNull Project project, final @NotNull SshTerminalCachingRunner runner, final @NotNull RemoteCredentials data) {
        try {
            runner.connect();
            AppUIUtil.invokeLaterIfProjectAlive(project, () ->
                    TerminalView.getInstance(project).createNewSession(runner));
        } catch (final RemoteSdkException e) {
            AppUIUtil.invokeLaterIfProjectAlive(project, () ->
                    Messages.showErrorDialog(project, e.getMessage(), SSH_CONNECTION_TITLE));
            // invoke ssh config window
            AzureTaskManager.getInstance().runLater(() -> {
                final SshConfigConfigurable configurable = new SshConfigConfigurable.Main(project);
                ShowSettingsUtil.getInstance().editConfigurable(project, configurable, () -> {
                    try {
                        MethodUtils.invokeMethod(configurable, true, "select", data);
                    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e2) {
                        AzureMessager.getMessager().error(e2);
                    }
                });
            });
        }
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
        config.setPort(22);
        return config;
    }

    private static String getDefaultSshPrivateKeyPath() {
        return Paths.get(System.getProperty("user.home"), ".ssh", "id_rsa").toString();
    }

}
