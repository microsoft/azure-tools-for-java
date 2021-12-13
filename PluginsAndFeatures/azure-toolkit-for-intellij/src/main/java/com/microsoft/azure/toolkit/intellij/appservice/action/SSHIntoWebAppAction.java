/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.action;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.appservice.TunnelProxy;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebApp;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.Groupable;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalView;

import java.io.IOException;
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
    private final String webAppName;
    private final WebApp webApp;

    public SSHIntoWebAppAction(WebAppNode webAppNode) {
        super();
        this.project = (Project) webAppNode.getProject();
        this.webApp = webAppNode.getWebApp();
        this.webAppName = webApp.name();
    }

    @Override

    protected void actionPerformed(NodeActionEvent nodeActionEvent) throws AzureCmdException {
        logger.info(message("webapp.ssh.hint.startSSH", webAppName));
        // ssh to connect to remote web app container.
        final AzureString title = title("webapp.connect_ssh.app", webAppName);
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false,
            () -> {
                final TunnelProxy proxy = new TunnelProxy(webApp);

                final int localPort;
                try {
                    localPort = proxy.start();
                } catch (final IOException e) {
                    try {
                        proxy.close();
                    } catch (final Throwable ex) {
                        // ignore
                    }
                    throw new AzureToolkitRuntimeException(message("webapp.ssh.error.message"));
                }
                final int finalLocalPort = localPort;

                // ssh to local proxy and open terminal.
                AzureTaskManager.getInstance().runAndWait(() -> {
                    // create a new terminal tab.
                    final TerminalView terminalView = TerminalView.getInstance(project);
                    final ShellTerminalWidget shellTerminalWidget = terminalView.createLocalShellWidget(null, String.format(WEBAPP_TERMINAL_TABLE_NAME, webAppName));
                    final AzureString messageTitle = title("webapp.open_ssh.app", webAppName);
                    AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, messageTitle, false, () -> {
                        // create connection to the local proxy.
                        final SSHTerminalManager.CreateRemoteConnectionInfo info = new SSHTerminalManager.CreateRemoteConnectionInfo();
                        info.setUsername(TunnelProxy.DEFAULT_SSH_USERNAME);
                        info.setPassword(TunnelProxy.DEFAULT_SSH_PASSWORD);
                        info.setPort(finalLocalPort);
                        SSHTerminalManager.INSTANCE.openConnectionInTerminal(shellTerminalWidget, info);
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
