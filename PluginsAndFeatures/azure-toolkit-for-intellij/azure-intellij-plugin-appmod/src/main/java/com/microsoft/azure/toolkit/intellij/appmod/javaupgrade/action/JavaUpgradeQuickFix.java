/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.action;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.JavaUpgradeIssue;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaVersionNotificationService;
import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.Constants.APPMOD_CVE_AGENT_NAME;
import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.Constants.APPMOD_UPGRADE_AGENT_NAME;
import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.Constants.FIX_VULNERABLE_DEPENDENCY_WITH_COPILOT_PROMPT;
import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.Constants.UPGRADE_JAVA_FRAMEWORK_PROMPT;

/**
 * Quick fix for Java/Spring version upgrade issues detected in pom.xml files.
 * This action is triggered from Java upgrade issue inspections and opens Copilot
 * to assist with the upgrade process.
 */
@Slf4j
public class JavaUpgradeQuickFix implements LocalQuickFix {
    private final JavaUpgradeIssue issue;

    public JavaUpgradeQuickFix(@NotNull JavaUpgradeIssue issue) {
        this.issue = issue;
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
        return "Azure Toolkit";
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getName() {
        String name = "Upgrade " + issue.getPackageDisplayName() + " with Copilot";
        if (!AppModPluginInstaller.isAppModPluginInstalled()) {
            return name + AppModPluginInstaller.TO_INSTALL_APP_MODE_PLUGIN;
        }
        return name;
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        try {
            String prompt = buildPromptForIssue(issue);
            final String agentName = issue.getUpgradeReason() == JavaUpgradeIssue.UpgradeReason.CVE
                ? APPMOD_CVE_AGENT_NAME
                : APPMOD_UPGRADE_AGENT_NAME;
            JavaVersionNotificationService.getInstance().openCopilotChatWithPrompt(project, prompt, agentName);
            AppModUtils.logTelemetryEvent("openCopilotChatForJavaUpgradeQuickFix", Map.of("appmodPluginInstalled", String.valueOf(AppModPluginInstaller.isAppModPluginInstalled())));
        } catch (Throwable ex) {
            log.error("Failed to apply Java upgrade quick fix", ex);
        }
    }

    private String buildPromptForIssue(@NotNull JavaUpgradeIssue issue) {
        if (issue.getUpgradeReason() == JavaUpgradeIssue.UpgradeReason.CVE) {
            final String coordinate = issue.getCurrentVersion() == null
                ? issue.getPackageId()
                : issue.getPackageId() + ":" + issue.getCurrentVersion();
            return String.format(FIX_VULNERABLE_DEPENDENCY_WITH_COPILOT_PROMPT, coordinate);
        }
        return String.format(
            UPGRADE_JAVA_FRAMEWORK_PROMPT,
            issue.getPackageDisplayName(), issue.getCurrentVersion(), issue.getSuggestedVersion()
        );
    }
}
