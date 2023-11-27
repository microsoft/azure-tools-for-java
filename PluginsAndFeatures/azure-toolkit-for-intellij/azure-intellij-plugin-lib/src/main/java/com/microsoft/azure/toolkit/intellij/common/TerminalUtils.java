/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalView;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
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
            final TerminalView terminalView = TerminalView.getInstance(project);
            final String workingDirectory = Optional.ofNullable(workingDir).map(Path::toString).orElse(null);
            final ShellTerminalWidget shellTerminalWidget = terminalView.getWidgets().stream()
                    .filter(widget -> widget instanceof ShellTerminalWidget)
                    .map(widget -> (ShellTerminalWidget) widget)
                    .filter(widget -> StringUtils.isBlank(terminalTabTitle) ||
                            StringUtils.equals(widget.getTerminalTitle().buildTitle(), terminalTabTitle))
                    .filter(widget -> !widget.hasRunningCommands())
                    .findFirst()
                    .orElseGet(() -> terminalView.createLocalShellWidget(workingDirectory, terminalTabTitle));
            AzureTaskManager.getInstance().runInBackground(OperationBundle.description("boundary/common.execute_in_terminal.command", command), () -> {
                try {
                    int count = 0;
                    while ((shellTerminalWidget.getTtyConnector() == null || shellTerminalWidget.getTerminalStarter() == null) && count++ < 30) {
                        Thread.sleep(500);
                    }
                    shellTerminalWidget.executeCommand(command);
                } catch (final IOException | InterruptedException t) {
                    throw new AzureToolkitRuntimeException(t);
                }
            });
        }, AzureTask.Modality.ANY);
    }
}