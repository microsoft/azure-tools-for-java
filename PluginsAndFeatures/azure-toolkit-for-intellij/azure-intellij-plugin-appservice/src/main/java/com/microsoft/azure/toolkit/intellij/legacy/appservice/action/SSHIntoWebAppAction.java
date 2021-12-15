/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.action;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebApp;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.legacy.appservice.TunnelProxy;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalView;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.logging.Logger;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;
import static com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle.title;

/**
 * SSH into Web App Action
 */
public class SSHIntoWebAppAction {

    private static final Logger logger = Logger.getLogger(SSHIntoWebAppAction.class.getName());

    private static final String WEBAPP_TERMINAL_TABLE_NAME = "SSH - %s";
    private static final String RESOURCE_GROUP_PATH_PREFIX = "resourceGroups/";
    private static final String RESOURCE_ELEMENT_PATTERN = "[^/]+";

    private final Project project;
    private final String webAppName;
    private final String subscriptionId;
    private final String resourceGroupName;
    private final WebApp webApp;

    public SSHIntoWebAppAction(@Nonnull final WebApp webApp, @Nullable final Project project) {
        super();
        this.project = project;
        this.webApp = webApp;
        this.webAppName = webApp.name();
        this.subscriptionId = webApp.subscriptionId();
        this.resourceGroupName = webApp.resourceGroup();
    }

    public void execute() {
        final Action<Void> retry = Action.retryFromFailure(this::execute);
        logger.info(message("webapp.ssh.hint.startSSH", webAppName));
        // ssh to connect to remote web app container.
        final AzureString title = title("webapp.connect_ssh.app", webAppName);
        AzureTaskManager.getInstance().runInBackground(new AzureTask(project, title, false,
            () -> {
                final TunnelProxy proxy = new TunnelProxy(webApp);

                int localPort;
                try {
                    localPort = proxy.start();
                } catch (IOException e) {
                    try {
                        proxy.close();
                    } catch (Throwable ex) {
                        // ignore
                    }
                    throw new AzureToolkitRuntimeException(message("webapp.ssh.error.message"), retry);
                }
                final int finalLocalPort = localPort;

                // ssh to local proxy and open terminal.
                AzureTaskManager.getInstance().runAndWait(() -> {
                    // create a new terminal tab.
                    TerminalView terminalView = TerminalView.getInstance(project);
                    ShellTerminalWidget shellTerminalWidget = terminalView.createLocalShellWidget(null, String.format(WEBAPP_TERMINAL_TABLE_NAME, webAppName));
                    final AzureString messageTitle = title("webapp.open_ssh.app", webAppName);
                    AzureTaskManager.getInstance().runInBackground(new AzureTask(project, messageTitle, false, () -> {
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
}
