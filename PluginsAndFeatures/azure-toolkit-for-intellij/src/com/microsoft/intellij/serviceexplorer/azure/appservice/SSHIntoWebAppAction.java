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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.intellij.util.AzureCliUtils;
import com.microsoft.intellij.util.PatternUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import org.jetbrains.plugins.terminal.TerminalView;

/**
 * SSH into Web App Action
 */
@Name(WebAppNode.SSH_INTO)
public class SSHIntoWebAppAction extends NodeActionListener {

    public static final String SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE = "SSH into Web App Error";
    private static final String CLI_NOT_INSTALLED_DIALOG_MESSAGE = "Azure CLI is vital for SSH into Web App. Please install it and then try again.";
    private static final String WIN_NOT_SUPPORT_DIALOG_TITLE = "SSH into Web App Error";
    private static final String WIN_NOT_SUPPORT_DIALOG_MESSAGE = "Azure SSH is only supported for Linux web apps";
    private static final String OS_LINUX = "linux";
    private static final String WEBAPP_TERMINAL_TABLE_NAME = "SSH - %s";
    private static final String RESOUCE_GROUP_PATH_PREFIX = "resourceGroups/";
    private static final String RESOUCE_ELEMENT_PATTERN = "[^/]+";

    private final Project project;
    private final String resourceId;
    private final String webAppName;
    private final String subscriptionId;
    private final String resourceGroupName;
    private final String os;

    public SSHIntoWebAppAction(WebAppNode webAppNode) {
        super();
        this.project = (Project) webAppNode.getProject();
        this.resourceId = webAppNode.getId();
        this.webAppName = webAppNode.getWebAppName();
        this.subscriptionId = webAppNode.getSubscriptionId();
        this.resourceGroupName = PatternUtils.parseWordByPatternAndPrefix(resourceId, RESOUCE_ELEMENT_PATTERN, RESOUCE_GROUP_PATH_PREFIX);
        this.os = webAppNode.getOs();
    }

    @Override
    protected void actionPerformed(NodeActionEvent nodeActionEvent) throws AzureCmdException {
        // check to confirm that azure cli is installed.
        if (!SSHTerminalManager.INSTANCE.checkToConfirmAzureCliInstalled()) {
            Messages.showWarningDialog(CLI_NOT_INSTALLED_DIALOG_MESSAGE, SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE);
            return;
        }
        // only support these web app those os is linux.
        if (!OS_LINUX.equalsIgnoreCase(os)) {
            Messages.showWarningDialog(WIN_NOT_SUPPORT_DIALOG_MESSAGE, WIN_NOT_SUPPORT_DIALOG_TITLE);
            return;
        }
        System.out.println(String.format("Start to perform SSH into Web App (%s)....", webAppName));
        // create a new terminal tab.
        TerminalView terminalView = TerminalView.getInstance(project);
        terminalView.createLocalShellWidget(null, String.format(WEBAPP_TERMINAL_TABLE_NAME, webAppName));
        // ssh to connect to remote web app container.
        DefaultLoader.getIdeHelper().runInBackground(project, String.format("Connecting to Web App (%s) ...", webAppName), true, false, null, () -> {
            // build proxy between remote and local
            SSHTerminalManager.CreateRemoteConnectionOutput connectionInfo = SSHTerminalManager.INSTANCE.executeAzCreateRemoteConnectionAndGetOutput(
                    AzureCliUtils.CLI_GROUP_AZ, AzureCliUtils.formatCreateWebAppRemoteConnectionParameters(subscriptionId, resourceGroupName, webAppName));
            System.out.println(String.format("Complete to execute ssh connection. output message is below: %s", connectionInfo.getOutputMessage()));
            // validate create-remote-connection output to ensure it's ready to ssh to local proxy and open terminal.
            if (!SSHTerminalManager.INSTANCE.validateConnectionOutputForOpenInTerminal(connectionInfo, webAppName)) {
                return;
            }
            // create connection to the local proxy.
            SSHTerminalManager.INSTANCE.openConnectionInTerminal(project, webAppName, connectionInfo);
        });
        System.out.println(String.format("End to perform SSH into Web App (%s)", webAppName));
    }
}
