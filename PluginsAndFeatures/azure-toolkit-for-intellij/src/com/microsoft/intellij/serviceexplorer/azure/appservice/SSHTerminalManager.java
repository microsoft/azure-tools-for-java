/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.serviceexplorer.azure.appservice;

import com.microsoft.azuretools.utils.CommandUtils;
import com.microsoft.intellij.util.AzureCliUtils;
import com.microsoft.intellij.util.PatternUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import java.io.IOException;

public enum SSHTerminalManager {
    INSTANCE;

    private static final String SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE = "SSH into Web App Error";
    private static final String SSH_INTO_WEB_APP_ERROR_MESSAGE = "SSH into Web App (%s) is not started. Please try again.";
    private static final String CLI_DIALOG_MESSAGE_NOT_INSTALLED = "Azure CLI is vital for SSH into Web App. Please install it and then try again.";
    private static final String CLI_DIALOG_MESSAGE_NOT_LOGIN = "Please run 'az login' to setup account.";
    private static final String CLI_DIALOG_MESSAGE_NO_PERMISSION_TO_CURRENT_SUBS =
            "Azure CLI acccount has NO permission to access current subscription. Please sign in with same account.";
    private static final String SSH_INTO_WEB_APP_ERROR_MESSAGE_NOT_SUPPORT_WINDOWS = "Azure SSH is only supported for Linux web apps";
    private static final String SSH_INTO_WEB_APP_ERROR_MESSAGE_NOT_SUPPORT_DOCKER = "Azure SSH is only supported for Java web apps. Current app is a Docker.";
    private static final String OS_LINUX = "linux";
    private static final String WEB_APP_FX_VERSION_PREFIX = "DOCKER|";
    private static final String CMD_SSH_TO_LOCAL_PROXY =
            "ssh -o StrictHostKeyChecking=no -o \"UserKnownHostsFile /dev/null\" -o \"LogLevel ERROR\" root@127.0.0.1 -p %s \r\n";
    private static final String CMD_SSH_TO_LOCAL_PWD = "Docker!\r\n";
    private static final String SSH_INTO_WEB_APP_DISABLE_MESSAGE =
            "SSH is not enabled for this app. To enable SSH follow this instructions: https://go.microsoft.com/fwlink/?linkid=2132395";

    /**
     * these actions (validation, etc) before ssh into web app.
     *
     * @param os
     * @param fxVersion
     * @return
     */
    public boolean beforeExecuteAzCreateRemoteConnection(String subscriptionId, String os, String fxVersion) {
        // check to confirm that azure cli is installed.
        if (!AzureCliUtils.isCliInstalled()) {
            DefaultLoader.getUIHelper().showError(CLI_DIALOG_MESSAGE_NOT_INSTALLED, SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
            return false;
        }
        // check azure cli login
        if (!AzureCliUtils.isCliLogined()) {
            DefaultLoader.getUIHelper().showError(CLI_DIALOG_MESSAGE_NOT_LOGIN, SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
            return false;
        }
        // check cli signed-in account has permission to connect to current subscription.
        if (!AzureCliUtils.containSubscription(subscriptionId)) {
            DefaultLoader.getUIHelper().showError(CLI_DIALOG_MESSAGE_NO_PERMISSION_TO_CURRENT_SUBS, SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
            return false;
        }
        // only support these web app those os is linux.
        if (!OS_LINUX.equalsIgnoreCase(os)) {
            DefaultLoader.getUIHelper().showError(SSH_INTO_WEB_APP_ERROR_MESSAGE_NOT_SUPPORT_WINDOWS, SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
            return false;
        }
        // check non-docker web app
        if (StringUtils.containsIgnoreCase(fxVersion, WEB_APP_FX_VERSION_PREFIX)) {
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
        CommandUtils.CommendExecOutput commendExecOutput = null;
        try {
            commendExecOutput = AzureCliUtils.executeCommandAndGetOutputWithCompleteKeyWord(parameters,
                    AzureCliUtils.CLI_COMMAND_REMOTE_CONNECTION_EXEC_SUCCESS_KEY_WORDS, AzureCliUtils.CLI_COMMAND_REMOTE_CONNECTION_EXEC_FAILED_KEY_WORDS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        connectionInfo.setOutputMessage(commendExecOutput.getOutputMessage());
        connectionInfo.setSuccess(commendExecOutput.isSuccess());
        if (commendExecOutput.isSuccess()) {
            String username = PatternUtils.parseWordByPatternAndPrefix(commendExecOutput.getOutputMessage(), PatternUtils.PATTERN_WHOLE_WORD, "username: ");
            if (StringUtils.isNotBlank(username)) {
                connectionInfo.setUsername(username);
            }
            String port = PatternUtils.parseWordByPatternAndPrefix(commendExecOutput.getOutputMessage(), PatternUtils.PATTERN_WHOLE_NUMBER_PORT, "port: ");
            if (StringUtils.isNotBlank(port)) {
                connectionInfo.setPort(port);
            }
            String password = PatternUtils.parseWordByPatternAndPrefix(commendExecOutput.getOutputMessage(), PatternUtils.PATTERN_WHOLE_WORD, "password: ");
            if (StringUtils.isNotBlank(password)) {
                connectionInfo.setPassword(password);
            }
        }
        return connectionInfo;
    }

    /**
     * validate create-remote-connection output to ensure it's ready to ssh to local proxy and open terminal.
     *
     * @param connectionInfo
     * @param webAppName
     */
    public boolean validateConnectionOutputBeforeOpenInTerminal(CreateRemoteConnectionOutput connectionInfo, String webAppName) {
        if (connectionInfo == null) {
            DefaultLoader.getUIHelper().showError(String.format(SSH_INTO_WEB_APP_ERROR_MESSAGE, webAppName), SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
            return false;
        }
        if (connectionInfo.isSuccess()) {
            if (StringUtils.isBlank(connectionInfo.getPort())) {
                DefaultLoader.getUIHelper().showError(String.format(SSH_INTO_WEB_APP_ERROR_MESSAGE, webAppName), SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
                return false;
            }
        } else {
            DefaultLoader.getUIHelper().showError(String.format(SSH_INTO_WEB_APP_ERROR_MESSAGE, webAppName), SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
            return false;
        }
        return true;
    }

    /**
     * ssh to connect to local proxy and open the terminal for remote container.
     *
     * @param shellTerminalWidget
     * @param connectionInfo
     */
    public void openConnectionInTerminal(ShellTerminalWidget shellTerminalWidget, CreateRemoteConnectionOutput connectionInfo) {
        try {
            int count = 0;
            while ((shellTerminalWidget.getTtyConnector() == null) && count++ < 200) {
                Thread.sleep(100);
            }
            shellTerminalWidget.executeCommand(String.format(CMD_SSH_TO_LOCAL_PROXY, connectionInfo.getPort()));
            Thread.sleep(5000);
            shellTerminalWidget.executeCommand(CMD_SSH_TO_LOCAL_PWD);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class CreateRemoteConnectionOutput extends CommandUtils.CommendExecOutput {
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
