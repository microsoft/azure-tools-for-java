package com.microsoft.azure.toolkit.intellij.azd.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.terminal.ui.TerminalWidget;
import com.microsoft.azure.toolkit.intellij.azd.utils.AzdCliUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import java.util.Set;

public abstract class AzdCommandAction extends AnAction {

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

        // Check if azd installed
        if (AzdCliUtils.getAzdVersion() == null) {
            // Prompt the user to install azd
            int result = Messages.showYesNoDialog(
                    project,
                    "Azure Developer CLI is not installed. Would you like to install it?",
                    "Install Azure Developer CLI",
                    "Install",
                    "Later",
                    Messages.getQuestionIcon()
            );
            if (result == Messages.YES) {
                if (!AzdCliUtils.installAzdCli(project)) {
                    return;
                }
            } else {
                return;
            }
        }

        // Create new terminal tab under the `directory`
        TerminalToolWindowManager terminalManager = TerminalToolWindowManager.getInstance(project);
        TerminalWidget terminal = terminalManager.createShellWidget(directory, command, true, true);

        // Run the command
        terminal.sendCommandToExecute(command);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        // Only enable the action for specific files
        boolean enabled = file != null && getSupportedFiles().contains(file.getName());
        event.getPresentation().setEnabledAndVisible(enabled);
    }

    /**
     * `ActionUpdateThread.OLD_EDT` is deprecated and going to be removed soon.
     * override `getActionUpdateThread()` and chose EDT or BGT
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    public abstract Set<String> getSupportedFiles();
}
