/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.project.Project;
import com.intellij.terminal.ui.TerminalWidget;
import com.jediterm.terminal.ProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.plugins.terminal.ProxyTtyConnector;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;
import org.jetbrains.plugins.terminal.TerminalUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Optional;

public class TerminalUtils {

    public static void executeInTerminal(@Nonnull Project project, @Nonnull String command) {
        executeInTerminal(project, command, null, null);
    }

    public static void executeInTerminal(@Nonnull Project project, @Nonnull String command, @Nonnull Path workingDir) {
        executeInTerminal(project, command, workingDir, null);
    }

    public static void executeInTerminal(@Nonnull Project project, @Nonnull String command, @Nonnull String terminalTabTitle) {
        executeInTerminal(project, command, null, terminalTabTitle);
    }

    @AzureOperation(name = "boundary/common.execute_in_terminal.command", params = "command")
    public static void executeInTerminal(@Nonnull Project project, @Nonnull String command, @Nullable Path workingDir, @Nullable String terminalTabTitle) {
        AzureTaskManager.getInstance().runLater(() -> {
            final TerminalWidget terminalWidget = getOrCreateTerminalWidget(project, workingDir, terminalTabTitle);
            AzureTaskManager.getInstance().runInBackground(OperationBundle.description("boundary/common.execute_in_terminal.command", command), () -> {
                terminalWidget.getTtyConnectorAccessor().executeWithTtyConnector((connector) -> {
                    terminalWidget.sendCommandToExecute(command);
                });
            });
        }, AzureTask.Modality.ANY);
    }

    @Nonnull
    private static TerminalWidget getOrCreateTerminalWidget(@Nonnull Project project, @Nullable Path workingDir, String terminalTabTitle) {
        final TerminalToolWindowManager manager = TerminalToolWindowManager.getInstance(project);
        final String workingDirectory = Optional.ofNullable(workingDir).map(Path::toString).orElse(null);
        return manager.getTerminalWidgets().stream()
            .filter(widget -> StringUtils.isBlank(terminalTabTitle) ||
                StringUtils.equals(widget.getTerminalTitle().buildTitle(), terminalTabTitle))
            .filter(widget -> !hasRunningCommands(widget))
            .findFirst()
            .orElseGet(() -> manager.createShellWidget(workingDirectory, terminalTabTitle, true, true));
    }

    public static boolean hasRunningCommands(TerminalWidget widget) throws IllegalStateException {
        final TtyConnector connector = widget.getTtyConnector();
        if (connector == null) {
            return false;
        } else {
            final ProcessTtyConnector processTtyConnector = getProcessTtyConnector(connector);
            if (processTtyConnector != null) {
                return TerminalUtil.hasRunningCommands(processTtyConnector);
            } else {
                throw new IllegalStateException("Cannot determine if there are running processes for " + connector.getClass());
            }
        }
    }

    public static @Nullable ProcessTtyConnector getProcessTtyConnector(@Nullable TtyConnector connector) {
        if (connector instanceof ProcessTtyConnector) {
            return (ProcessTtyConnector) connector;
        } else {
            return connector instanceof ProxyTtyConnector ? getProcessTtyConnector(((ProxyTtyConnector) connector).getConnector()) : null;
        }
    }
}