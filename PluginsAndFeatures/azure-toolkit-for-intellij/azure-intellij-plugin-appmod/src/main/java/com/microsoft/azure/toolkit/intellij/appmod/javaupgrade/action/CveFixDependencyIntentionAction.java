/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.action;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.VulnerabilityInfo;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesCache;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaVersionNotificationService;

import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModUtils;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.GradleBuildFileUtils;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.PomXmlUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.Constants;

import java.util.Map;

/**
 * Intention action to fix CVE vulnerabilities in dependencies using GitHub Copilot.
 * This action appears in the editor's quick-fix popup (More actions...) for pom.xml files(yellow wavy dependency with CVE issue)
 * when a vulnerable dependency with CVE issues is detected.
 * 
 * Implements HighPriorityAction to appear at the top of the quick-fix list.
 */
@Slf4j
public class CveFixDependencyIntentionAction implements IntentionAction, HighPriorityAction {
    
    // Cached dependency info from isAvailable() for use in getText()
    private VulnerabilityInfo vulnerabilityInfo;

    @Override
    public @IntentionName @NotNull String getText() {
        if (!AppModPluginInstaller.isAppModPluginInstalled()) {
            return  Constants.FIX_VULNERABLE_DEPENDENCY_WITH_COPILOT_DISPLAY_NAME + AppModPluginInstaller.TO_INSTALL_APP_MODE_PLUGIN;
        }
        return Constants.FIX_VULNERABLE_DEPENDENCY_WITH_COPILOT_DISPLAY_NAME;
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return "Azure Toolkit";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        try {
            // Reset cached values
            vulnerabilityInfo = null;

            if (file == null || editor == null) {
                return false;
            }

            // Only available for pom.xml or Gradle build files
            final String fileName = file.getName();
            final boolean isPomFile = fileName.equals("pom.xml");
            final boolean isGradleFile = fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts");
            if (!isPomFile && !isGradleFile) {
                return false;
            }
            final int offset = editor.getCaretModel().getOffset();
            final String documentText = editor.getDocument().getText();

            if (isPomFile) {
                // Try to extract dependency info - only show if cursor is within a <dependency> block
                final int dependencyStart = PomXmlUtils.findDependencyStart(documentText, offset);
                final int dependencyEnd = PomXmlUtils.findDependencyEnd(documentText, offset);

                if (dependencyStart >= 0 && dependencyEnd > dependencyStart) {
                    final String dependencyBlock = documentText.substring(dependencyStart, dependencyEnd);
                    String cachedGroupId = PomXmlUtils.extractXmlValue(dependencyBlock, "groupId");
                    String cachedArtifactId = PomXmlUtils.extractXmlValue(dependencyBlock, "artifactId");
                    String cachedVersion = PomXmlUtils.extractXmlValue(dependencyBlock, "version");
                    vulnerabilityInfo = VulnerabilityInfo.builder().groupId(cachedGroupId).artifactId(cachedArtifactId).version(cachedVersion).build();
                    // Only show if we have valid dependency info (not for parent/plugin sections)
                    if (cachedGroupId != null && cachedArtifactId != null) {
                        //if the artifact is in the cached cve issues, show the intention
                        final var issue = JavaUpgradeIssuesCache.getInstance(project).findCveIssue(cachedGroupId + ":" + cachedArtifactId);
                        if (issue != null) {
                         //   AppModUtils.logTelemetryEvent("cveFixDependencyIntentionActionIsAvailable", Map.of("appmodPluginInstalled", String.valueOf(AppModPluginInstaller.isAppModPluginInstalled())));
                            return true;
                        }
                        return false;
                    }
                }
            } else if (isGradleFile) {
                final GradleBuildFileUtils.DependencyCoordinate coordinate =
                    GradleBuildFileUtils.findDependencyAtOffset(documentText, offset);
                if (coordinate.isValid()) {
                    vulnerabilityInfo = VulnerabilityInfo.builder()
                        .groupId(coordinate.groupId())
                        .artifactId(coordinate.artifactId())
                        .version(coordinate.version())
                        .build();
                    final var issue = JavaUpgradeIssuesCache.getInstance(project)
                        .findCveIssue(coordinate.getPackageId());
                    if (issue != null) {
                        AppModUtils.logTelemetryEvent("cveFixDependencyIntentionActionIsAvailable", Map.of("appmodPluginInstalled", String.valueOf(AppModPluginInstaller.isAppModPluginInstalled())));
                        return true;
                    }
                    return false;
                }
            }
        } catch (Throwable e) {
            // Ignore and return false
            log.error("Error checking availability of CveFixDependencyIntentionAction", e);
        }
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        try {
            if (file == null || editor == null) {
                return;
            }

            // Try to extract dependency information from the current context
            final String prompt = buildPromptFromContext();
            JavaVersionNotificationService.getInstance().openCopilotChatWithPrompt(project, prompt);
            AppModUtils.logTelemetryEvent("openCveFixDependencyCopilotChatFromIntentionAction", Map.of("appmodPluginInstalled", String.valueOf(AppModPluginInstaller.isAppModPluginInstalled())));
        } catch (Throwable e) {
            log.error("Failed to invoke CveFixDependencyIntentionAction: ", e);
        }
    }

    /**
     * Builds a prompt based on the current editor context.
     */
    private String buildPromptFromContext() {
        if (vulnerabilityInfo == null) {
            log.error("Vulnerability info is null in buildPromptFromContext");
            return Constants.SCAN_AND_RESOLVE_CVES_PROMPT;
        }
        return String.format(Constants.FIX_VULNERABLE_DEPENDENCY_WITH_COPILOT_PROMPT,
                vulnerabilityInfo.getDependencyCoordinate());
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
