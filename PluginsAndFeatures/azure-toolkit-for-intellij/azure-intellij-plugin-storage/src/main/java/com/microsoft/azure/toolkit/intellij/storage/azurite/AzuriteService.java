/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.azurite;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessHandlerFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.actions.CloseAction;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.Version;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.util.EnvironmentUtil;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.storage.StorageActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.CommonConst;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.TerminalUtils;
import com.microsoft.azure.toolkit.intellij.storage.IntellijStorageActionsContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.CommandUtils;
import com.microsoft.azure.toolkit.lib.storage.AzuriteStorageAccount;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AzuriteService {
    public static final String INTELLIJ_GLOBAL_STORAGE = "IntelliJ Global Storage";
    public static final String CURRENT_PROJECT = "Current Project";
    public static final String AZURITE_DISPLAY_NAME = "Azurite";
    public static final String INSTALL_AZURITE_LINK = "https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite?tabs=npm#install-azurite";
    public static final String NODE_VERSION_ERROR_MESSAGE = "Failed to get node version, or node version is too old (lower than 8) to run azurite, please visit https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite for installation guidances.";
    public static final String AZURITE_INSTALL_COMMAND = "npm install -g azurite";
    public static final String AZURITE_HAS_BEEN_TERMINATED = "Azurite has been terminated";
    public static final String DEFAULT_WORKSPACE = System.getProperty("user.home");
    public static final String AZURITE = "azurite";
    public static final String AZURITE_CMD = "azurite.cmd";
    public static final Icon ICON = IntelliJAzureIcons.getIcon(AzureIcons.StorageAccount.AZURITE);
    public static final int MIN_NODE_MAJOR_VERSION = 8;

    private ProcessHandler processHandler;

    public static AzuriteService getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public boolean startAzurite(@Nonnull final Project project) {
        // if process is still running, return
        final Boolean isProcessTerminated = Optional.ofNullable(processHandler).map(ProcessHandler::isProcessTerminated).orElse(true);
        if (!isProcessTerminated) {
            // show related toolwindow
            showAzuriteTerminal(project);
            return false;
        }
        try {
            final ConsoleView consoleView = getOrCreateConsoleView(project);
            final GeneralCommandLine commandLine = getExecutionCommand(project);
            commandLine.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE);
            this.processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine);
            this.processHandler.addProcessListener(new ProcessAdapter() {
                @Override
                public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                    if (StringUtils.containsIgnoreCase(event.getText(), "service is successfully listening at")) {
                        AzuriteStorageAccount.AZURITE_STORAGE_ACCOUNT.refresh();
                    }
                }

                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    consoleView.print(AZURITE_HAS_BEEN_TERMINATED, ConsoleViewContentType.SYSTEM_OUTPUT);
                    AzuriteStorageAccount.AZURITE_STORAGE_ACCOUNT.refresh();
                    AzureMessager.getMessager().warning(AZURITE_HAS_BEEN_TERMINATED);
                }
            });
            consoleView.attachToProcess(processHandler);
            this.processHandler.startNotify();
        } catch (final ExecutionException e) {
            AzureTaskManager.getInstance().runInBackground("Diagnose Azurite issues", () -> {
                final Action<?>[] azuriteFailureActions = getAzuriteFailureActions(project);
                AzureMessager.getMessager().error("Failed to run azurite, " + ExceptionUtils.getRootCauseMessage(e), azuriteFailureActions);
            });
            return false;
        }
        return true;
    }

    private Action<?>[] getAzuriteFailureActions(@Nonnull final Project project) {
        final Action<?> installAzuriteAction = AzureActionManager.getInstance().getAction(IntellijStorageActionsContributor.INSTALL_AZURITE)
                .bind(project);
        final Action<Object> settingAction = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS);
        final Action<String> learnAction = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL)
                .bind(INSTALL_AZURITE_LINK).withLabel("Learn More");
        return isNodeMeetAzuriteRequirement() ? new Action[]{installAzuriteAction, settingAction, learnAction} : new Action[]{settingAction, learnAction};
    }

    public static void installAzurite(@Nonnull final Project project) {
        if (!isNodeMeetAzuriteRequirement()) {
            AzureMessager.getMessager().error(NODE_VERSION_ERROR_MESSAGE);
        }
        TerminalUtils.executeInTerminal(project, AZURITE_INSTALL_COMMAND, "Azurite installation");
    }

    private static boolean isNodeMeetAzuriteRequirement() {
        try {
            final GeneralCommandLine commandLine = new GeneralCommandLine("node", "-v");
            final String nodeVersion = ExecUtil.execAndGetOutput(commandLine).getStdout();
            return Optional.ofNullable(Version.parseVersion(StringUtils.removeStartIgnoreCase(nodeVersion, "v")))
                    .map(version -> version.compareTo(MIN_NODE_MAJOR_VERSION) >= 0).orElse(false);
        } catch (final Exception e) {
            return false;
        }
    }

    private void showAzuriteTerminal(@Nonnull final Project project) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Run");
        if (Objects.nonNull(toolWindow)) {
            Arrays.stream(toolWindow.getContentManager().getContents())
                    .filter(content -> StringUtils.equalsIgnoreCase(content.getDisplayName(), AZURITE_DISPLAY_NAME))
                    .findFirst().ifPresent(content -> toolWindow.getContentManager().setSelectedContent(content));
        }
    }

    private ConsoleView getOrCreateConsoleView(final Project project) {
        final DataContext context = dataId -> CommonDataKeys.PROJECT.getName().equals(dataId) ? project : null;
        final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), null, "azure.azurite", context);
        ActionManager.getInstance().getAction("ActivateRunToolWindow").actionPerformed(event);
        final ConsoleView console = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Run");
        if (toolWindow == null) {
            return console;
        }
        final String displayName = AZURITE_DISPLAY_NAME;
        final Content result = Arrays.stream(toolWindow.getContentManager().getContents())
                .filter(content -> StringUtils.equalsIgnoreCase(content.getDisplayName(), displayName))
                .findFirst().orElseGet(() -> {
                    final Content content = ContentFactory.SERVICE.getInstance().createContent(console.getComponent(), displayName, false);
                    content.setIcon(ICON);
                    content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
                    toolWindow.getContentManager().addContent(content);
                    return content;
                });
        result.setComponent(console.getComponent());
        toolWindow.getContentManager().addContentManagerListener(new ContentManagerListener() {
            @Override
            public void contentRemoved(@Nonnull ContentManagerEvent event) {
                // terminate process once closed
                if (event.getContent() == result) {
                    AzuriteService.getInstance().stopAzurite();
                }
            }
        });
        toolWindow.getContentManager().setSelectedContent(result);
        return console;
    }

    private static GeneralCommandLine getExecutionCommand(@Nonnull final Project project) {
        final GeneralCommandLine result = new GeneralCommandLine();
        result.withExePath(getAzuritePath());
        result.withParameters("--location", getAzuriteWorkspace(project));
        if (Azure.az().config().getEnableLeaseMode()) {
            result.withParameters("--loose");
        }
        final String fileLocation = StringUtils.firstNonBlank(Azure.az().config().getAzuriteWorkspace(), DEFAULT_WORKSPACE);
        return result;
    }

    private static String getAzuriteWorkspace(@Nonnull final Project project) {
        final String fileLocation = StringUtils.firstNonBlank(Azure.az().config().getAzuriteWorkspace(), INTELLIJ_GLOBAL_STORAGE);
        switch (fileLocation) {
            case INTELLIJ_GLOBAL_STORAGE:
                return Path.of(CommonConst.PLUGIN_PATH, AZURITE).toString();
            case CURRENT_PROJECT:
                final VirtualFile virtualFile = Optional.ofNullable(ProjectUtil.guessProjectDir(project)).orElseGet(project::getBaseDir);
                return Path.of(virtualFile.getPath(), AZURITE).toString();
            default:
                return fileLocation;
        }
    }

    private static String getAzuritePath() {
        final String azuritePath = Azure.az().config().getAzuritePath();
        if (StringUtils.isNotBlank(azuritePath) && FileUtil.exists(azuritePath)) {
            return azuritePath;
        }
        final List<String> azurite = CommandUtils.resolveCommandPath(AZURITE);
        return azurite.stream().filter(StringUtils::isNoneBlank)
                .filter(file -> !(SystemInfo.isWindows && StringUtils.isBlank(FilenameUtils.getExtension(file)))) // for windows, azurite did not work as PATH_EXT is not supported with Java Process
                .findFirst().orElse(SystemInfo.isWindows ? AZURITE_CMD : AZURITE);
    }

    public void stopAzurite() {
        Optional.ofNullable(processHandler).ifPresent(handler -> {
            if (!handler.isProcessTerminated()) {
                handler.destroyProcess();
            }
        });
    }

    private static class SingletonHolder {
        public static final AzuriteService INSTANCE = new AzuriteService();
    }
}
