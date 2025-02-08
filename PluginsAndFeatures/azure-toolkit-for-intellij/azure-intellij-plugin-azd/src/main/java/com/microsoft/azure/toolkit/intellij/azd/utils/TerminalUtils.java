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

    public static TerminalWidgetInfo getOrCreateTerminalWidget(@NotNull Project project, @NotNull String workingDir) {
        final TerminalToolWindowManager terminalManager = TerminalToolWindowManager.getInstance(project);
        final Path projectBasePath = Paths.get(Objects.requireNonNull(project.getBasePath()));
        final String relativePath = Paths.get(workingDir).relativize(projectBasePath).toString();
        final String terminalTitle = StringUtils.isBlank(relativePath) ? project.getName() : relativePath;
        final Optional<TerminalWidget> matchTerminal = terminalManager.getTerminalWidgets().stream()
                .filter(terminal -> StringUtils.equals(terminal.getTerminalTitle().buildTitle(), terminalTitle))
                .filter(terminal -> !hasRunningCommands(terminal))
                .findFirst();
        if (matchTerminal.isPresent()) {
            final TerminalWidget terminalWidget = matchTerminal.get();
            return new TerminalWidgetInfo(terminalWidget, false);
        } else {
            return new TerminalWidgetInfo(
                    terminalManager.createShellWidget(workingDir, terminalTitle, true, true),
                    true
            );
        }
    }

    private static boolean hasRunningCommands(TerminalWidget terminal) throws IllegalStateException {
        final TtyConnector connector = terminal.getTtyConnector();
        if (connector == null) {
            return false;
        } else {
            return TerminalUtil.hasRunningCommands(connector);
        }
    }

    public static class TerminalWidgetInfo {

        public TerminalWidgetInfo(TerminalWidget widget, boolean isNew) {
            terminalWidget = widget;
            isNewTerminal = isNew;
        }

        public TerminalWidget terminalWidget;

        public boolean isNewTerminal;
    }
}
