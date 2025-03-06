package com.microsoft.azure.toolkit.intellij.sjad.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.terminal.ui.TerminalWidget;
import com.microsoft.azure.toolkit.intellij.sjad.utils.AzdCliUtils;
import com.microsoft.azure.toolkit.intellij.sjad.utils.TerminalUtils;
import com.microsoft.azure.toolkit.intellij.common.action.AzureAnAction;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public abstract class AbstractAzdCommandAction extends AzureAnAction {

    @Override
    public boolean onActionPerformed(@NotNull AnActionEvent anActionEvent, @Nullable Operation operation) {
        final Project project = anActionEvent.getProject();
        if (project == null) return true;

        final VirtualFile file = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) return true;

        final String directory = file.getParent() != null ? file.getParent().getPath() : null;
        if (directory == null) return true;

        final String command = anActionEvent.getPresentation().getDescription();
        if (command == null || command.isEmpty()) return true;

        // Check if azd installed, if not, prompt to install
        AzdCliUtils.azdCliInstalledAsync().thenAccept(installed -> {
            // Switch back to Event Dispatch Thread for UI-related operations
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!installed) {
                    final int result = Messages.showYesNoDialog(
                            project,
                            "Azure Developer CLI is not installed. Would you like to install it?",
                            "Install Azure Developer CLI",
                            "Install",
                            "Later",
                            Messages.getQuestionIcon()
                    );
                    if (result == Messages.YES) {
                        final TerminalWidget terminal = TerminalUtils.createTerminalWidget(project, directory, command);
                        AzdCliUtils.installAndSetupAzdCli(terminal);
                        terminal.sendCommandToExecute(AzdCliUtils.getAzdInvocation(command));
                    } else {
                        Notifications.Bus.notify(new Notification(
                                "AzureDeveloperCLI",
                                "Azure Developer CLI is not installed",
                                "Please install the Azure Developer CLI to use this functionality.",
                                NotificationType.INFORMATION
                        ), project);
                    }
                } else {
                    final TerminalWidget terminal = TerminalUtils.createTerminalWidget(project, directory, command);
                    AzdCliUtils.setupAzdCli(terminal);
                    terminal.sendCommandToExecute(AzdCliUtils.getAzdInvocation(command));
                }
            });
        });

        return false;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        final VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        // Only enable the action for specific files
        final boolean enabled = file != null && getSupportedFiles().contains(file.getName());
        event.getPresentation().setEnabledAndVisible(enabled);
    }

    public abstract Set<String> getSupportedFiles();
}
