/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.action;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.utils.AzureCliUtils;
import com.microsoft.intellij.util.PatternUtils;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.Groupable;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalView;

import java.util.logging.Logger;

import static com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle.title;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

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
    private final Runtime runtime;

    public SSHIntoWebAppAction(WebAppNode webAppNode) {
        super();
        this.project = (Project) webAppNode.getProject();
        this.resourceId = webAppNode.getId();
        this.webAppName = webAppNode.getWebAppName();
        this.subscriptionId = webAppNode.getSubscriptionId();
        this.resourceGroupName = PatternUtils.parseWordByPatternAndPrefix(resourceId, RESOURCE_ELEMENT_PATTERN, RESOURCE_GROUP_PATH_PREFIX);
        this.runtime = webAppNode.getWebAppRuntime();
    }

    @Override
    protected void actionPerformed(NodeActionEvent nodeActionEvent) throws AzureCmdException {
        logger.info(message("webapp.ssh.hint.startSSH", webAppName));
        // ssh to connect to remote web app container.
        final AzureString title = title("webapp|ssh.connect", webAppName);
        AzureTaskManager.getInstance().runInBackground(new AzureTask(project, title, false, () -> {
            // check these conditions to ssh into web app
            if (!SSHTerminalManager.INSTANCE.beforeExecuteAzCreateRemoteConnection(subscriptionId, runtime)) {
                return;
            }
            // build proxy between remote and local
            SSHTerminalManager.CreateRemoteConnectionOutput connectionInfo = SSHTerminalManager.INSTANCE.executeAzCreateRemoteConnectionAndGetOutput(
                AzureCliUtils.formatCreateWebAppRemoteConnectionParameters(subscriptionId, resourceGroupName, webAppName));
            logger.info(message("webapp.ssh.hint.sshConnectionDone", connectionInfo.getOutputMessage()));
            // ssh to local proxy and open terminal.
            AzureTaskManager.getInstance().runAndWait(() -> {
                // create a new terminal tab.
                TerminalView terminalView = TerminalView.getInstance(project);
                ShellTerminalWidget shellTerminalWidget = terminalView.createLocalShellWidget(null, String.format(WEBAPP_TERMINAL_TABLE_NAME, webAppName));
                final AzureString messageTitle = title("webapp|ssh.open", webAppName);
                AzureTaskManager.getInstance().runInBackground(new AzureTask(project, messageTitle, false, () -> {
                    // create connection to the local proxy.
                    SSHTerminalManager.INSTANCE.openConnectionInTerminal(shellTerminalWidget, connectionInfo);
                }));
            }, AzureTask.Modality.ANY);
        }));
        logger.info(message("webapp.ssh.hint.SSHDone", webAppName));
    }

    @Override
    public int getGroup() {
        return Groupable.DIAGNOSTIC_GROUP;
    }

    @Override
    protected String getServiceName(final NodeActionEvent event) {
        return TelemetryConstants.WEBAPP;
    }

    @Override
    protected String getOperationName(final NodeActionEvent event) {
        return TelemetryConstants.WEBAPP_SSHINTO;
    }
}
