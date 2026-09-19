/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaVersionNotificationService;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.ProblemsViewUtils;
import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.Constants.APPMOD_UPGRADE_AGENT_NAME;
import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.Constants.UPGRADE_JAVA_FRAMEWORK_PROMPT;

/**
 * Promotes Java and framework upgrade quick fixes to the Problems View context menu.
 */
@Slf4j
public class UpgradeInProblemsViewAction extends AnAction implements DumbAware {

    private static final Pattern UPGRADE_DESCRIPTION_PATTERN = Pattern.compile(
        "Your project uses (.+?) (\\S+)\\. Consider upgrading (.+?) to (\\S+), " +
            "the latest LTS version, for better performance and support"
    );

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final Project project = event.getData(CommonDataKeys.PROJECT);
        if (project == null || project.isDisposed()) {
            return;
        }
        final UpgradeDescription upgrade =
            parseUpgradeDescription(ProblemsViewUtils.extractProblemDescription(event));
        if (upgrade == null) {
            return;
        }

        try {
            JavaVersionNotificationService.getInstance().openCopilotChatWithPrompt(
                project,
                String.format(
                    UPGRADE_JAVA_FRAMEWORK_PROMPT,
                    upgrade.packageDisplayName(),
                    upgrade.currentVersion(),
                    upgrade.suggestedVersion()
                ),
                APPMOD_UPGRADE_AGENT_NAME
            );
            AppModUtils.logTelemetryEvent(
                "openCopilotChatForUpgradeInProblemsViewAction",
                Map.of(
                    "appmodPluginInstalled",
                    String.valueOf(AppModPluginInstaller.isAppModPluginInstalled())
                )
            );
        } catch (Throwable throwable) {
            log.error("Failed to open Copilot chat for Java or framework upgrade", throwable);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        final Project project = event.getData(CommonDataKeys.PROJECT);
        final VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        final UpgradeDescription upgrade =
            parseUpgradeDescription(ProblemsViewUtils.extractProblemDescription(event));
        final boolean visible = project != null && !project.isDisposed() &&
            isBuildFile(file) && upgrade != null;
        event.getPresentation().setEnabledAndVisible(visible);
        if (!visible) {
            return;
        }

        String actionName = getActionName(upgrade);
        if (!AppModPluginInstaller.isAppModPluginInstalled()) {
            actionName += AppModPluginInstaller.TO_INSTALL_APP_MODE_PLUGIN;
        }
        event.getPresentation().setText(actionName);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Nullable
    static UpgradeDescription parseUpgradeDescription(@Nullable String description) {
        if (description == null) {
            return null;
        }
        final Matcher matcher = UPGRADE_DESCRIPTION_PATTERN.matcher(description);
        if (!matcher.find() || !matcher.group(1).equals(matcher.group(3))) {
            return null;
        }
        return new UpgradeDescription(matcher.group(1), matcher.group(2), matcher.group(4));
    }

    @NotNull
    static String getActionName(@NotNull UpgradeDescription upgrade) {
        return "Upgrade " + upgrade.packageDisplayName() + " with Copilot";
    }

    private static boolean isBuildFile(@Nullable VirtualFile file) {
        return file != null && (
            file.getName().equals("pom.xml") ||
                file.getName().endsWith(".gradle") ||
                file.getName().endsWith(".gradle.kts")
        );
    }

    record UpgradeDescription(
        @NotNull String packageDisplayName,
        @NotNull String currentVersion,
        @NotNull String suggestedVersion
    ) {
    }
}
