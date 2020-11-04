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
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.utils.AzureCliUtils;
import com.microsoft.intellij.util.PatternUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalView;

import java.util.logging.Logger;

/**
 * SSH into Web App Action
 */
@Name(WebAppNode.SSH_INTO)
public class SSHIntoWebAppAction extends NodeActionListener {

    private static final Logger logger = Logger.getLogger(SSHIntoWebAppAction.class.getName());

    private static final String WEBAPP_TERMINAL_TABLE_NAME = "SSH - %s";
    private static final String RESOURCE_GROUP_PATH_PREFIX = "resourceGroups/";
    private static final String RESOURCE_ELEMENT_PATTERN = "[^/]+";

    private final Project project;
    private final String resourceId;
    private final String webAppName;
    private final String subscriptionId;
    private final String resourceGroupName;
    private final String os;
    private final WebApp app;

    public SSHIntoWebAppAction(WebAppNode webAppNode) {
        super();
        this.app = webAppNode.getWebapp();
        this.project = (Project) webAppNode.getProject();
        this.resourceId = webAppNode.getId();
        this.webAppName = webAppNode.getWebAppName();
        this.subscriptionId = webAppNode.getSubscriptionId();
        this.resourceGroupName = PatternUtils.parseWordByPatternAndPrefix(resourceId, RESOURCE_ELEMENT_PATTERN, RESOURCE_GROUP_PATH_PREFIX);
        this.os = webAppNode.getOs();
    }

    @Override
    protected void actionPerformed(NodeActionEvent nodeActionEvent) throws AzureCmdException {
        logger.info(String.format("Start to perform SSH into Web App (%s)....", webAppName));
        // ssh to connect to remote web app container.
        DefaultLoader.getIdeHelper().runInBackground(project, String.format("Connecting to Web App (%s) ...", webAppName), true, false, null, () -> {
            // check these conditions to ssh into web app
            if (!SSHTerminalManager.INSTANCE.beforeExecuteAzCreateRemoteConnection(subscriptionId, os, this.app.linuxFxVersion())) {
                return;
            }
            // build proxy between remote and local
            SSHTerminalManager.CreateRemoteConnectionOutput connectionInfo = SSHTerminalManager.INSTANCE.executeAzCreateRemoteConnectionAndGetOutput(
                    AzureCliUtils.formatCreateWebAppRemoteConnectionParameters(subscriptionId, resourceGroupName, webAppName));
            logger.info(String.format("Complete to execute ssh connection. output message is below: %s", connectionInfo.getOutputMessage()));
            // ssh to local proxy and open terminal.
            DefaultLoader.getIdeHelper().invokeAndWait(() -> {
                // create a new terminal tab.
                TerminalView terminalView = TerminalView.getInstance(project);
                ShellTerminalWidget shellTerminalWidget = terminalView.createLocalShellWidget(null, String.format(WEBAPP_TERMINAL_TABLE_NAME, webAppName));
                DefaultLoader.getIdeHelper().runInBackground(project, String.format("Opening SSH - %s session ...", webAppName), true, false, null, () -> {
                    // create connection to the local proxy.
                    SSHTerminalManager.INSTANCE.openConnectionInTerminal(shellTerminalWidget, connectionInfo);
                });
            });

        });
        logger.info(String.format("End to perform SSH into Web App (%s)", webAppName));
    }
}
