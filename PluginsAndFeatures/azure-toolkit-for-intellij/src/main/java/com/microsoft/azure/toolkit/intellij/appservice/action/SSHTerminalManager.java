/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.action;

import com.jediterm.terminal.TerminalDataStream;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azuretools.utils.AzureCliUtils;
import com.microsoft.azuretools.utils.CommandUtils;
import com.microsoft.intellij.util.PatternUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import java.io.IOException;
import java.util.logging.Logger;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public enum SSHTerminalManager {
    INSTANCE;

    private static final Logger logger = Logger.getLogger(SSHTerminalManager.class.getName());

    private static final String SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE = message("webapp.ssh.error.title");
    private static final String SSH_INTO_WEB_APP_ERROR_MESSAGE = message("webapp.ssh.error.message");
    private static final String CLI_DIALOG_MESSAGE_DEFAULT = message("webapp.ssh.error.cli");
    private static final String SSH_INTO_WEB_APP_ERROR_MESSAGE_NOT_SUPPORT_WINDOWS = message("webapp.ssh.error.notSupport.Windows");
    private static final String SSH_INTO_WEB_APP_ERROR_MESSAGE_NOT_SUPPORT_DOCKER = message("webapp.ssh.error.notSupport.Docker");
    private static final String OS_LINUX = "linux";
    private static final String WEB_APP_DOCKER_PREFIX = "DOCKER|";
    private static final String CMD_SSH_TO_LOCAL_PROXY =
            "ssh -o StrictHostKeyChecking=no -o \"UserKnownHostsFile /dev/null\" -o \"LogLevel ERROR\" %s@127.0.0.1 -p %s";

    /**
     * these actions (validation, etc) before ssh into web app.
     *
     * @param subscriptionId
     * @param os
     * @param fxVersion
     * @return
     */
    public boolean beforeExecuteAzCreateRemoteConnection(String subscriptionId, Runtime runtime) {
        try {
            // check to confirm that azure cli is installed.
            if (!AzureCliUtils.isCliInstalled()) {
                DefaultLoader.getUIHelper().showError(CLI_DIALOG_MESSAGE_DEFAULT, SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
                return false;
            }
            // check if cli signed-in and the account has permission to connect to current subscription.
            if (!AzureCliUtils.containSubscription(subscriptionId)) {
                DefaultLoader.getUIHelper().showError(CLI_DIALOG_MESSAGE_DEFAULT, SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            DefaultLoader.getUIHelper().showError(SSH_INTO_WEB_APP_ERROR_MESSAGE, SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
            return false;
        }
        // only support these web app those os is linux.
        if (runtime.getOperatingSystem() == OperatingSystem.WINDOWS) {
            DefaultLoader.getUIHelper().showError(SSH_INTO_WEB_APP_ERROR_MESSAGE_NOT_SUPPORT_WINDOWS, SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
            return false;
        }
        if (runtime.getOperatingSystem() == OperatingSystem.DOCKER) {
            DefaultLoader.getUIHelper().showError(SSH_INTO_WEB_APP_ERROR_MESSAGE_NOT_SUPPORT_DOCKER, SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
            return false;
        }
        return true;
    }

    /**
     * create remote connection to remote web app container.
     *
     * @param parameters
     * @return
     */
    public CreateRemoteConnectionOutput executeAzCreateRemoteConnectionAndGetOutput(final String[] parameters) {
        CreateRemoteConnectionOutput connectionInfo = new CreateRemoteConnectionOutput();
        CommandUtils.CommandExecOutput commandExecOutput = null;
        try {
            commandExecOutput = AzureCliUtils.executeCommandAndGetOutputWithCompleteKeyWord(parameters,
                    AzureCliUtils.CLI_COMMAND_REMOTE_CONNECTION_EXEC_SUCCESS_KEY_WORDS, AzureCliUtils.CLI_COMMAND_REMOTE_CONNECTION_EXEC_FAILED_KEY_WORDS);
        } catch (IOException | InterruptedException e) {
            DefaultLoader.getUIHelper().showError(SSH_INTO_WEB_APP_ERROR_MESSAGE, SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
            return connectionInfo;
        }
        connectionInfo.setOutputMessage(commandExecOutput.getOutputMessage());
        connectionInfo.setSuccess(commandExecOutput.isSuccess());
        if (commandExecOutput.isSuccess()) {
            String username = PatternUtils.parseWordByPatternAndPrefix(commandExecOutput.getOutputMessage(), PatternUtils.PATTERN_WHOLE_WORD, "username: ");
            if (StringUtils.isBlank(username)) {
                username = PatternUtils.parseWordByPatternAndPrefix(commandExecOutput.getErrorMessage(), PatternUtils.PATTERN_WHOLE_WORD, "username: ");
            }
            String port = PatternUtils.parseWordByPatternAndPrefix(commandExecOutput.getOutputMessage(), PatternUtils.PATTERN_WHOLE_NUMBER_PORT, "port: ");
            if (StringUtils.isBlank(port)) {
                port = PatternUtils.parseWordByPatternAndPrefix(commandExecOutput.getErrorMessage(), PatternUtils.PATTERN_WHOLE_NUMBER_PORT, "port: ");
            }
            String password = PatternUtils.parseWordByPatternAndPrefix(commandExecOutput.getOutputMessage(), PatternUtils.PATTERN_WHOLE_WORD, "password: ");
            if (StringUtils.isBlank(password)) {
                password = PatternUtils.parseWordByPatternAndPrefix(commandExecOutput.getErrorMessage(), PatternUtils.PATTERN_WHOLE_WORD, "password: ");
            }
            connectionInfo.setUsername(username);
            connectionInfo.setPort(port);
            connectionInfo.setPassword(password);
        }
        return connectionInfo;
    }

    /**
     * ssh to connect to local proxy and open the terminal for remote container.
     *
     * @param shellTerminalWidget
     * @param connectionInfo
     */
    public void openConnectionInTerminal(ShellTerminalWidget shellTerminalWidget, CreateRemoteConnectionOutput connectionInfo) {
        if (connectionInfo == null || !connectionInfo.isSuccess() ||
                StringUtils.isAnyBlank(connectionInfo.getPort(), connectionInfo.getUsername(), connectionInfo.getPassword())) {
            DefaultLoader.getUIHelper().showError(SSH_INTO_WEB_APP_ERROR_MESSAGE, SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
            return;
        }
        try {
            int count = 0;
            while ((shellTerminalWidget.getTtyConnector() == null) && count++ < 200) {
                Thread.sleep(100);
            }
            shellTerminalWidget.executeCommand(String.format(CMD_SSH_TO_LOCAL_PROXY, connectionInfo.getUsername(), connectionInfo.getPort()));
            waitForInputPassword(shellTerminalWidget, 30000);
            shellTerminalWidget.executeCommand(connectionInfo.getPassword());
        } catch (IOException | InterruptedException | IllegalAccessException e) {
            DefaultLoader.getUIHelper().showError(SSH_INTO_WEB_APP_ERROR_MESSAGE, SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
        }
    }

    private void waitForInputPassword(ShellTerminalWidget shellTerminalWidget, int timeout) throws IllegalAccessException {
        TerminalDataStream terminalDataStream = (TerminalDataStream) FieldUtils.readField(shellTerminalWidget.getTerminalStarter(), "myDataStream", true);
        char[] myBuf = (char[]) FieldUtils.readField(terminalDataStream, "myBuf", true);
        int count = 0;
        int interval = 100;
        int countMax = timeout / interval;
        try {
            while (count++ < countMax) {
                if (myBuf != null && String.valueOf(myBuf).contains("password:")) {
                    logger.info(message("webapp.ssh.hint.passwordReady") + String.valueOf(myBuf));
                    return;
                }
                Thread.sleep(interval);
            }
        } catch (InterruptedException e) {
        }
        logger.info(message("webapp.ssh.hint.passwordNotReady"));
    }

    public static class CreateRemoteConnectionOutput extends CommandUtils.CommandExecOutput {
        private String username;
        private String password;
        private String port;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }
    }

}
