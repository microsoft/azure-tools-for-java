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
import com.microsoft.azure.toolkit.intellij.azd.utils.TerminalUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public abstract class AzdCommandAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final Project project = event.getProject();
        if (project == null) return;

        final VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) return;

        final String directory = file.getParent() != null ? file.getParent().getPath() : null;
        if (directory == null) return;

        final String command = event.getPresentation().getDescription();
        if (command == null || command.isEmpty()) return;

        // Get or create terminal tab under the `directory`
        final TerminalUtils.TerminalWidgetInfo terminalWidget = TerminalUtils.getOrCreateTerminalWidget(project, directory);
        final TerminalWidget terminal = terminalWidget.terminalWidget;
        if (AzdCliUtils.azdCliInstallAttempted && terminalWidget.isNewTerminal) {
            AzdCliUtils.setupAzdEnvs(terminal);
        }

        // Check if azd installed, if not, prompt to install
        if (!AzdCliUtils.checkAzdCliInstalled(terminal)) {
            final int result = Messages.showYesNoDialog(
                    project,
                    "Azure Developer CLI is not installed. Would you like to install it?",
                    "Install Azure Developer CLI",
                    "Install",
                    "Later",
                    Messages.getQuestionIcon()
            );
            if (result == Messages.YES) {
                AzdCliUtils.installAzdCli(terminal);
            } else {
                return;
            }
        }

        terminal.sendCommandToExecute(AzdCliUtils.getAzdInvocation(command));
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        final VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        // Only enable the action for specific files
        final boolean enabled = file != null && getSupportedFiles().contains(file.getName());
        event.getPresentation().setEnabledAndVisible(enabled);
    }

    public abstract Set<String> getSupportedFiles();

    /**
     * `ActionUpdateThread.OLD_EDT` is deprecated and going to be removed soon.
     * Recommend to override `getActionUpdateThread()`
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
