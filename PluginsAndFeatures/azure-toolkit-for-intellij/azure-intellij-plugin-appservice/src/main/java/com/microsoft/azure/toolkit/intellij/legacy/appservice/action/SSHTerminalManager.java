/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.action;

import com.jediterm.terminal.TerminalDataStream;
import com.jediterm.terminal.emulator.Emulator;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.CustomLog;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import java.io.IOException;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

@CustomLog
public enum SSHTerminalManager {
    INSTANCE;

    private static final String SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE = message("webapp.ssh.error.title");
    public static final String SSH_INTO_WEB_APP_ERROR_MESSAGE = message("webapp.ssh.error.message");
    private static final String SSH_INTO_WEB_APP_ERROR_MESSAGE_NOT_SUPPORT_WINDOWS = message("webapp.ssh.error.notSupport.Windows");
    private static final String SSH_INTO_WEB_APP_ERROR_MESSAGE_NOT_SUPPORT_DOCKER = message("webapp.ssh.error.notSupport.Docker");
    private static final String OS_LINUX = "linux";
    private static final String WEB_APP_DOCKER_PREFIX = "DOCKER|";
    private static final String CMD_SSH_TO_LOCAL_PROXY =
            "ssh -o StrictHostKeyChecking=no -o \"UserKnownHostsFile /dev/null\" -o \"LogLevel ERROR\" %s@127.0.0.1 -p %d";

    /**
     * ssh to connect to local proxy and open the terminal for remote container.
     *
     * @param shellTerminalWidget
     * @param connectionInfo
     */
    public void openConnectionInTerminal(ShellTerminalWidget shellTerminalWidget, CreateRemoteConnectionInfo connectionInfo) {
        if (connectionInfo == null || connectionInfo.getPort() <= 0 ||
                StringUtils.isAnyBlank(connectionInfo.getUsername(), connectionInfo.getPassword())) {
            AzureMessager.getMessager().error(SSH_INTO_WEB_APP_ERROR_MESSAGE, SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
            return;
        }
        try {
            int count = 0;
            while ((shellTerminalWidget.getTtyConnector() == null) && count++ < 200) {
                Thread.sleep(100);
            }

            while ((shellTerminalWidget.getTerminalStarter() == null) && count++ < 200) {
                Thread.sleep(100);
            }
            doOpenTerminal(shellTerminalWidget, connectionInfo);
        } catch (IOException | InterruptedException | IllegalAccessException e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

    @AzureOperation(name = "boundary/appservice.open_ssh_terminal")
    private void doOpenTerminal(ShellTerminalWidget shellTerminalWidget, CreateRemoteConnectionInfo connectionInfo) throws IOException, IllegalAccessException {
        shellTerminalWidget.executeCommand(String.format(CMD_SSH_TO_LOCAL_PROXY, connectionInfo.getUsername(), connectionInfo.getPort()));
        waitForInputPassword(shellTerminalWidget, 30000);
        shellTerminalWidget.executeCommand(connectionInfo.getPassword());
    }

    private void waitForInputPassword(ShellTerminalWidget shellTerminalWidget, int timeout) throws IllegalAccessException {
        final Emulator emulator = (Emulator) FieldUtils.readField(shellTerminalWidget.getTerminalStarter(), "myEmulator", true);
        final TerminalDataStream terminalDataStream = (TerminalDataStream) FieldUtils.readField(emulator, "myDataStream", true);
        char[] myBuf = (char[]) FieldUtils.readField(terminalDataStream, "myBuf", true);
        int count = 0;
        int interval = 100;
        int countMax = timeout / interval;
        try {
            while (count++ < countMax) {
                if (myBuf != null && String.valueOf(myBuf).contains("password:")) {
                    return;
                }
                Thread.sleep(interval);
            }
        } catch (InterruptedException e) {
        }
        log.info(message("webapp.ssh.hint.passwordNotReady"));
    }

    @Data
    public static class CreateRemoteConnectionInfo {
        private String username;
        private String password;
        private int port;
    }
}
