/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.action;

import com.intellij.openapi.project.Project;
import com.intellij.terminal.ui.TerminalWidget;
import com.microsoft.azure.toolkit.intellij.common.TerminalUtils;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.legacy.appservice.TunnelProxy;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;
import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

/**
 * SSH into Web App Action
 */
@Slf4j
public class SSHIntoWebAppAction {
    private static final String SSH_INTO_WEB_APP_ERROR_DIALOG_TITLE = message("webapp.ssh.error.title");
    public static final String SSH_INTO_WEB_APP_ERROR_MESSAGE = message("webapp.ssh.error.message");
    private static final String SSH_INTO_WEB_APP_ERROR_MESSAGE_NOT_SUPPORT_WINDOWS = message("webapp.ssh.error.notSupport.Windows");
    private static final String SSH_INTO_WEB_APP_ERROR_MESSAGE_NOT_SUPPORT_DOCKER = message("webapp.ssh.error.notSupport.Docker");
    private static final String OS_LINUX = "linux";
    private static final String WEB_APP_DOCKER_PREFIX = "DOCKER|";
    private static final String CMD_SSH_TO_LOCAL_PROXY =
            "ssh -o StrictHostKeyChecking=no -o \"UserKnownHostsFile /dev/null\" -o \"LogLevel ERROR\" %s@127.0.0.1 -p %d";

    private static final String WEBAPP_TERMINAL_TABLE_NAME = "SSH - %s";
    private static final String RESOURCE_GROUP_PATH_PREFIX = "resourceGroups/";
    private static final String RESOURCE_ELEMENT_PATTERN = "[^/]+";

    private final Project project;
    private final String webAppName;
    private final WebApp webApp;

    public SSHIntoWebAppAction(@Nonnull final WebApp webApp, @Nullable final Project project) {
        super();
        this.project = project;
        this.webApp = webApp;
        this.webAppName = webApp.getName();
    }

    public void execute() {
        final Action<Void> retry = Action.retryFromFailure(this::execute);
        log.info(message("webapp.ssh.hint.startSSH", webAppName));
        // ssh to connect to remote web app container.
        final AzureString title = description("user/webapp.connect_ssh.app", webAppName);
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false,
                () -> {
                    if (webApp.getRuntime().getOperatingSystem() == OperatingSystem.WINDOWS) {
                        AzureMessager.getMessager().warning(message("webapp.ssh.windowsNotSupport"));
                        return;
                    }
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
                        throw new AzureToolkitRuntimeException(message("webapp.ssh.error.message"), retry);
                    }
                    final int finalLocalPort = localPort;

                    // ssh to local proxy and open terminal.
                    AzureTaskManager.getInstance().runAndWait(() -> {
                        // create a new terminal tab.
                        final TerminalWidget terminalWidget = TerminalUtils.createTerminalWidget(project, null, String.format(WEBAPP_TERMINAL_TABLE_NAME, webAppName));
                        final AzureString messageTitle = description("boundary/webapp.open_ssh.app", webAppName);
                        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, messageTitle, false, () -> {
                            // create connection to the local proxy.
                            openConnectionInTerminal(terminalWidget, finalLocalPort);
                        }));
                    }, AzureTask.Modality.ANY);
                }));
        log.info(message("webapp.ssh.hint.SSHDone", webAppName));
    }

    @AzureOperation(name = "boundary/$appservice.open_ssh_terminal")
    private void openConnectionInTerminal(TerminalWidget terminalWidget, int port) {
        terminalWidget.getTtyConnectorAccessor().executeWithTtyConnector((connector) -> {
            terminalWidget.sendCommandToExecute(String.format(CMD_SSH_TO_LOCAL_PROXY, TunnelProxy.DEFAULT_SSH_USERNAME, port));
            AzureTaskManager.getInstance().runOnPooledThread(() -> {
                try {
                    Thread.sleep(3000);
                    terminalWidget.sendCommandToExecute(TunnelProxy.DEFAULT_SSH_PASSWORD);
                } catch (final InterruptedException e) {
                    log.error("Error occurred when sending password to terminal", e);
                }
            });
        });
    }
}
