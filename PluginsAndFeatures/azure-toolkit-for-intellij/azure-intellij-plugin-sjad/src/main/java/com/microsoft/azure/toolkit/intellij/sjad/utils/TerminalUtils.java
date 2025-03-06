package com.microsoft.azure.toolkit.intellij.sjad.utils;

import com.intellij.openapi.project.Project;
import com.intellij.terminal.ui.TerminalWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

public class TerminalUtils {

    public static TerminalWidget createTerminalWidget(@NotNull Project project, @NotNull String workingDir, String command) {
        final TerminalToolWindowManager terminalManager = TerminalToolWindowManager.getInstance(project);
        return terminalManager.createShellWidget(workingDir, command, true, true);
    }
}
