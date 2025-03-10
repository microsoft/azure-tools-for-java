package com.microsoft.azure.toolkit.intellij.sjad.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.terminal.ui.TerminalWidget;
import com.microsoft.azure.toolkit.intellij.sjad.utils.AzdCliUtils;
import com.microsoft.azure.toolkit.intellij.sjad.utils.TerminalWidgetUtils;
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

        final TerminalWidget terminalWidget = TerminalWidgetUtils.createTerminalWidget(project, directory, command);

        AzdCliUtils.aliasAzdCommand(terminalWidget);
        AzdCliUtils.setAzdConfigDir(terminalWidget);

        terminalWidget.sendCommandToExecute(command);

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
