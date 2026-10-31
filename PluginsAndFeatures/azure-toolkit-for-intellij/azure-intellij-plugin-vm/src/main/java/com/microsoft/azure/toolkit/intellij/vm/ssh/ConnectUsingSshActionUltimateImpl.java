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
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.TerminalTabState;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;

/**
 * connect to selected machine by SSH in Intellij Ultimate.
 */
public class ConnectUsingSshActionUltimateImpl implements ConnectUsingSshAction {
    private static final ConnectUsingSshAction instance = new ConnectUsingSshActionUltimateImpl();
    private static final String SSH_CONNECTION_TITLE = "Connect Using SSH";

    public static ConnectUsingSshAction getInstance() {
        return instance;
    }

    public void connectBySsh(VirtualMachine vm, @Nonnull Project project) {
        final SshConfig ssh = AddSshConfigAction.getOrCreateSshConfig(vm, project);
        final SshConsoleOptionsProvider provider = SshConsoleOptionsProvider.getInstance(project);
        final RemoteCredentials sshCredential = new SshUiData(ssh, true);
        final SshTerminalCachingRunner runner = new SshTerminalCachingRunner(project, sshCredential, provider.getCharset());
        try {
            runner.connect();
            AppUIUtil.invokeLaterIfProjectAlive(project, () -> createSshSession(project, ssh, runner));
        } catch (final RemoteSdkException e) {
            AzureMessager.getMessager().warning(e.getMessage(), SSH_CONNECTION_TITLE,
                openSshConfigurationAction(project, sshCredential));
        }
    }

    @AzureOperation(name = "boundary/vm.create_ssh_session_iu")
    private static void createSshSession(@Nonnull Project project, SshConfig ssh, SshTerminalCachingRunner runner) {
        final TerminalTabState tabState = new TerminalTabState();
        tabState.myTabName = ssh.getName();
        TerminalToolWindowManager.getInstance(project).createNewSession(runner, tabState);
    }

    private Action<?> openSshConfigurationAction(final @NotNull Project project, RemoteCredentials sshCredential) {
        return new Action<>(Action.Id.of("boundary/vm.open_ssh_configuration"))
            .withLabel("Modify SSH Configuration")
            .withHandler(v -> AzureTaskManager.getInstance().runLater(() -> openSshConfiguration(project, sshCredential)));
    }

    @AzureOperation(name = "boundary/vm.open_ssh_configuration")
    private static void openSshConfiguration(@Nonnull Project project, RemoteCredentials sshCredential) {
        final SshConfigConfigurable configurable = new SshConfigConfigurable.Main(project);
        ShowSettingsUtil.getInstance().editConfigurable(project, configurable, () -> {
            try {
                MethodUtils.invokeMethod(configurable, true, "select", sshCredential);
            } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e2) {
                AzureMessager.getMessager().error(e2);
            }
        });
    }

}
