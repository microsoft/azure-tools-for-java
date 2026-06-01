/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.VulnerabilityInfo;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesCache;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaVersionNotificationService;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.ProblemsViewUtils;
import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.Constants.*;
import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.VulnerabilityInfo.parseVulnerabilityDescription;

/**
 * Action to fix vulnerable dependencies by opening GitHub Copilot chat with an upgrade prompt.
 * This action appears in the Problems View context menu for vulnerable dependency issues.
 */
@Slf4j
public class CveFixDependencyInProblemsViewAction extends AnAction implements DumbAware {

    private static final String CVE_MARKER = "CVE-";

    private VulnerabilityInfo vulnerabilityInfo;
    public CveFixDependencyInProblemsViewAction() {
        super();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            final Project project = e.getData(CommonDataKeys.PROJECT);
            if (project == null || project.isDisposed()) {
                return;
            }
            if (vulnerabilityInfo == null) {
                JavaVersionNotificationService.getInstance().openCopilotChatWithPrompt(
                        project,
                        SCAN_AND_RESOLVE_CVES_PROMPT
                );
            } else {
                JavaVersionNotificationService.getInstance().openCopilotChatWithPrompt(
                        project,
                        String.format(FIX_VULNERABLE_DEPENDENCY_WITH_COPILOT_PROMPT,
                                vulnerabilityInfo.getDependencyCoordinate())
                );
            }
            AppModUtils.logTelemetryEvent("openCopilotChatForCveFixDependencyInProblemsViewAction", Map.of("appmodPluginInstalled", String.valueOf(AppModPluginInstaller.isAppModPluginInstalled())));
        } catch (Throwable ex) {
            log.error("Failed to open Copilot chat for CVE fix", ex.getMessage());
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        try {
            final Project project = e.getData(CommonDataKeys.PROJECT);
            if (project == null || project.isDisposed()) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }

            // Check if we're in the Problems View context with a vulnerability
            final String description = ProblemsViewUtils.extractProblemDescription(e);
            if (description == null) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            vulnerabilityInfo = parseVulnerabilityDescription(description);
            // Also check if the file is pom.xml or build.gradle (common for dependency issues)
            final VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
            final boolean isBuildFile = isBuildFile(file);

            if (!isBuildFile || !isCVEIssue(description)) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            final var issue = JavaUpgradeIssuesCache.getInstance(project).findCveIssue(vulnerabilityInfo.getGroupId() + ":" + vulnerabilityInfo.getArtifactId());
            if (issue == null) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            e.getPresentation().setEnabledAndVisible(true);
            //  e.getPresentation().setText(SCAN_AND_RESOLVE_CVES_WITH_COPILOT_DISPLAY_NAME);
            final String baseText = getTemplatePresentation().getText();
            if (!AppModPluginInstaller.isAppModPluginInstalled()) {
                e.getPresentation().setText(baseText + AppModPluginInstaller.TO_INSTALL_APP_MODE_PLUGIN);
            } else {
                e.getPresentation().setText(baseText);
            }
        } catch (Throwable ex) {
            // In case of any error, hide the action
            e.getPresentation().setEnabledAndVisible(false);
            log.error("Failed to update CVE fix action visibility, hide the action", ex);
        }
    }

    private boolean isBuildFile(VirtualFile file) {
        return file != null &&
                (file.getName().equals("pom.xml") || file.getName().endsWith(".gradle") || file.getName().endsWith(".gradle.kts"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * Checks if the description contains a CVE issue marker.
     */
    private boolean isCVEIssue(@NotNull String description) {
        // Pattern: CVE-YYYY-NNNNN
        return description.toUpperCase().contains(CVE_MARKER);
    }
}
