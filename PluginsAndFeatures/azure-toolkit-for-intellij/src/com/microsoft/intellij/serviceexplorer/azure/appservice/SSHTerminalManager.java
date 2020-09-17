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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.terminal.JBTerminalPanel;
import com.jediterm.terminal.TerminalOutputStream;
import com.microsoft.azuretools.utils.CommandUtils;
import com.microsoft.intellij.util.AzureCliUtils;
import com.microsoft.intellij.util.PatternUtils;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory;

import javax.swing.*;
import java.io.IOException;

public enum SSHTerminalManager {
    INSTANCE;

    private static final long CMD_EXEC_TIMEOUT = 20 * 1000L;
    private static final int CMD_EXEC_EXIT_CODE_SUCCESS = 0;

    private static final String CMD_SSH_TO_LOCAL_PROXY =
            "ssh -o StrictHostKeyChecking=no -o \"UserKnownHostsFile /dev/null\" -o \"LogLevel ERROR\" root@127.0.0.1 -p %s \r\n";
    private static final String CMD_SSH_TO_LOCAL_PWD = "Docker!\r\n";
    private static final String SSH_INTO_WEB_APP_ERROR_MESSAGE = "SSH into Web App (%s) is not started. Please try again.";
    private static final String SSH_INTO_WEB_APP_DISABLE_MESSAGE =
            "SSH is not enabled for this app. To enable SSH follow this instructions: https://go.microsoft.com/fwlink/?linkid=2132395";

    /**
     * try to execute azure CLI command to detect it is installed or not.
     *
     * @return
     * true : azure installed.
     * false : azure not installed.
     */
    public boolean checkToConfirmAzureCliInstalled() {
        try {
            DefaultExecuteResultHandler resultHandler = CommandUtils.executeCommandAndGetResultHandler(
                    AzureCliUtils.CLI_GROUP_AZ, new String[]{AzureCliUtils.CLI_COMMAND_VERSION});
            resultHandler.waitFor(CMD_EXEC_TIMEOUT);
            int exitValue = resultHandler.getExitValue();
            System.out.println("exitCode: " + exitValue);
            if (exitValue == CMD_EXEC_EXIT_CODE_SUCCESS) {
                return true;
            }
            return false;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * create remote connection to remote web app container.
     *
     * @param command
     * @param parameters
     * @return
     */
    public CreateRemoteConnectionOutput executeAzCreateRemoteConnectionAndGetOutput(final String command, final String[] parameters) {
        CreateRemoteConnectionOutput connectionInfo = new CreateRemoteConnectionOutput();
        CommandUtils.CommendExecOutput commendExecOutput = null;
        try {
            commendExecOutput = AzureCliUtils.executeCommandAndGetOutputWithCompleteKeyWord(command, parameters,
                    AzureCliUtils.CLI_COMMAND_REMOTE_CONNECTION_EXEC_SUCCESS_KEY_WORDS, AzureCliUtils.CLI_COMMAND_REMOTE_CONNECTION_EXEC_FAILED_KEY_WORDS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        connectionInfo.setOutputMessage(commendExecOutput.getOutputMessage());
        connectionInfo.setSuccess(commendExecOutput.getSuccess());
        if (commendExecOutput.getSuccess()) {
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
        } else {

        }
        return connectionInfo;
    }

    /**
     * validate create-remote-connection output to ensure it's ready to ssh to local proxy and open terminal.
     *
     * @param connectionInfo
     * @param webAppName
     */
    public boolean validateConnectionOutputForOpenInTerminal(CreateRemoteConnectionOutput connectionInfo, String webAppName) {
        if (connectionInfo == null) {
            ApplicationManager.getApplication().invokeLater(() -> PluginUtil.displayErrorDialog(SSHIntoWebAppAction.SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE,
                    String.format(SSH_INTO_WEB_APP_ERROR_MESSAGE, webAppName)));
            return false;
        }
        if (connectionInfo.getSuccess()) {
            if (StringUtils.isBlank(connectionInfo.getPort())) {
                ApplicationManager.getApplication().invokeLater(() -> PluginUtil.displayErrorDialog(SSHIntoWebAppAction.SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE,
                        String.format(SSH_INTO_WEB_APP_ERROR_MESSAGE, webAppName)));
                return false;
            }
        } else {
            ApplicationManager.getApplication().invokeLater(() -> PluginUtil.displayErrorDialog(SSHIntoWebAppAction.SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE,
                    SSH_INTO_WEB_APP_DISABLE_MESSAGE));
            return false;
        }
        return true;
    }

    /**
     * ssh to connect to local proxy and open the terminal for remote container.
     *
     * @param project
     * @param webAppName
     * @param connectionInfo
     */
    public void openConnectionInTerminal(Project project, final String webAppName, CreateRemoteConnectionOutput connectionInfo) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID);
                JComponent root = toolWindow.getComponent();
                toolWindow.show(() -> {
                    // TerminalToolWindowPanel is an inner class of TerminalView, and sub class of SimpleToolWindowPanel
                    SimpleToolWindowPanel terminalToolWindowPanel = (SimpleToolWindowPanel) root.getComponent(0);
                    JPanel panel = (JPanel) terminalToolWindowPanel.getComponent(0);
                    ShellTerminalWidget panel1 = (ShellTerminalWidget) panel.getComponent(0);
                    JLayeredPane javaLayeredPane = (JLayeredPane) panel1.getComponent(0);
                    JBTerminalPanel terminalPanel = (JBTerminalPanel) javaLayeredPane.getComponent(0);
                    TerminalOutputStream terminalOutputStream = terminalPanel.getTerminalOutputStream();
                    terminalOutputStream.sendString(String.format(CMD_SSH_TO_LOCAL_PROXY, connectionInfo.getPort()));
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    terminalOutputStream.sendString(CMD_SSH_TO_LOCAL_PWD);
                });
            }
        });
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
