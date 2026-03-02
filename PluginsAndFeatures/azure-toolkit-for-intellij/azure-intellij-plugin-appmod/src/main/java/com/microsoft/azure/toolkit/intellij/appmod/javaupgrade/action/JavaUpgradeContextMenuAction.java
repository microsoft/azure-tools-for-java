/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaVersionNotificationService;

import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller.TO_INSTALL_APP_MODE_PLUGIN;
import static com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller.isAppModPluginInstalled;
import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.Constants.UPGRADE_JAVA_AND_FRAMEWORK_PROMPT;

/**
 * Context menu action to upgrade a Java project using GitHub Copilot.
 * This action appears in the GitHub Copilot submenu when right-clicking on:
 * - Project root folder
 * - pom.xml (Maven projects)
 * - build.gradle or build.gradle.kts (Gradle projects)
 */
@Slf4j
public class JavaUpgradeContextMenuAction extends AnAction {
    // text, description, and icon are defined in azure-intellij-plugin-appmod.xml
    public JavaUpgradeContextMenuAction() {
        super();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        try {
            final Project project = e.getProject();
            final VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);

            boolean visible = false;

            if (project != null && file != null) {
                // Check if it's a project root, pom.xml, or build.gradle file
                visible = isProjectRoot(project, file) ||
                        isMavenBuildFile(file) ||
                        isGradleBuildFile(file);
            }
            if (!isAppModPluginInstalled()) {
                e.getPresentation().setText(e.getPresentation().getText() + TO_INSTALL_APP_MODE_PLUGIN);
            }
            e.getPresentation().setEnabledAndVisible(visible);
        } catch (Throwable ex) {
            // In case of any error, hide the action
            e.getPresentation().setEnabledAndVisible(false);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            final Project project = e.getProject();
            if (project == null) {
                return;
            }

            final VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
            String prompt = buildUpgradePrompt();

            // Open Copilot chat with the upgrade prompt
            JavaVersionNotificationService.getInstance().openCopilotChatWithPrompt(project, prompt);
            AppModUtils.logTelemetryEvent("openJavaUpgradeCopilotChatFromContextMenu", Map.of("appmodPluginInstalled", String.valueOf(isAppModPluginInstalled())));
        } catch (Throwable ex) {
            // Log error but do not crash
            log.error("Failed to perform Java upgrade action from context menu", ex);
        }
    }

    /**
     * Builds the upgrade prompt based on the selected file.
     */
    private String buildUpgradePrompt() {
        return UPGRADE_JAVA_AND_FRAMEWORK_PROMPT;
    }

    /**
     * Checks if the file is the project root directory.
     */
    private boolean isProjectRoot(Project project, VirtualFile file) {
        if (!file.isDirectory()) {
            return false;
        }
        
        final VirtualFile projectBaseDir = project.getBaseDir();
        if (projectBaseDir == null) {
            return false;
        }
        
        return file.equals(projectBaseDir);
    }

    /**
     * Checks if the file is a Maven build file (pom.xml).
     */
    private boolean isMavenBuildFile(VirtualFile file) {
        return file != null && !file.isDirectory() && "pom.xml".equals(file.getName());
    }

    /**
     * Checks if the file is a Gradle build file (build.gradle or build.gradle.kts).
     */
    private boolean isGradleBuildFile(VirtualFile file) {
        if (file == null || file.isDirectory()) {
            return false;
        }
        final String name = file.getName();
        return "build.gradle".equals(name) || "build.gradle.kts".equals(name);
    }
}
