package com.microsoft.azure.toolkit.intellij.azd;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.terminal.ui.TerminalWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import java.util.Set;

public class RunAzdCommandAction extends AnAction {

    private static final Set<String> SUPPORTED_FILES = Set.of("pom.xml", "azure.yaml");

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;

        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) return;

        String directory = file.getParent() != null ? file.getParent().getPath() : null;
        if (directory == null) return;

        String command = event.getPresentation().getDescription();
        if (command == null || command.isEmpty()) return;

        // Create new terminal tab under the `directory`
        TerminalToolWindowManager terminalManager = TerminalToolWindowManager.getInstance(project);
        TerminalWidget terminal = terminalManager.createShellWidget(directory, command, true, true);

        // Run the command
        terminal.sendCommandToExecute(command);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean enabled = file != null && SUPPORTED_FILES.contains(file.getName());
        event.getPresentation().setEnabledAndVisible(enabled);
    }
}
