package com.microsoft.azure.toolkit.intellij.azd.utils;

import com.intellij.openapi.project.Project;
import com.intellij.terminal.ui.TerminalWidget;
import com.jediterm.terminal.TtyConnector;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;
import org.jetbrains.plugins.terminal.TerminalUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

public class TerminalUtils {

    public static TerminalWidget createTerminalWidget(@NotNull Project project, @NotNull String workingDir, String command) {
        final TerminalToolWindowManager terminalManager = TerminalToolWindowManager.getInstance(project);
        return terminalManager.createShellWidget(workingDir, command, true, true);
    }
}
