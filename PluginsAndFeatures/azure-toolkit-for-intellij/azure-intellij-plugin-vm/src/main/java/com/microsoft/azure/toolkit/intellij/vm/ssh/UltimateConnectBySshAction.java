/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.ssh;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.remote.RemoteCredentials;
import com.intellij.remote.RemoteSdkException;
import com.intellij.ssh.config.unified.SshConfig;
import com.intellij.ssh.ui.unified.SshConfigConfigurable;
import com.intellij.ssh.ui.unified.SshUiData;
import com.intellij.ui.AppUIUtil;
import com.jetbrains.plugins.remotesdk.console.SshConsoleOptionsProvider;
import com.jetbrains.plugins.remotesdk.console.SshTerminalCachingRunner;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.TerminalView;

import javax.annotation.Nonnull;

import java.lang.reflect.InvocationTargetException;

/**
 * connect to selected machine by SSH in Intellij Ultimate.
 */
public class UltimateConnectBySshAction {
    private static final String SSH_CONNECTION_TITLE = "Connect Using SSH";

    @AzureOperation(name = "vm.connect_using_ssh_community_ultimate", params = "vm.getName()", type = AzureOperation.Type.ACTION)
    public static void connectBySsh(VirtualMachine vm, @Nonnull Project project) {
        final SshConfig existingConfig = AddSshConfigAction.getOrCreateSshConfig(vm, project);
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
            final Action.Id<Void> id = Action.Id.of("vm.open_ssh_configuration");
            final Action<?> openSshConfigurationAction = new Action<>(id, v -> AzureTaskManager.getInstance().runLater(() -> {
                final SshConfigConfigurable configurable = new SshConfigConfigurable.Main(project);
                ShowSettingsUtil.getInstance().editConfigurable(project, configurable, () -> {
                    try {
                        MethodUtils.invokeMethod(configurable, true, "select", data);
                    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e2) {
                        AzureMessager.getMessager().error(e2);
                    }
                });
            }), new ActionView.Builder("Modify SSH Configuration"));
            AzureMessager.getMessager().warning(e.getMessage(), SSH_CONNECTION_TITLE, openSshConfigurationAction);
        }
    }

}
