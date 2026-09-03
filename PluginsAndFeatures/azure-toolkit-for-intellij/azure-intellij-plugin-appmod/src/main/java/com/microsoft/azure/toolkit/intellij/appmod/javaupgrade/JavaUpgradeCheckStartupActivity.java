/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.util.Alarm;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.JavaUpgradeIssue;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesCache;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaVersionNotificationService;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.List;

/**
 * Startup activity that detects outdated JDK and framework versions when a project is opened.
 * This runs after the project is fully loaded and shows notifications for any detected issues.
 */
@Slf4j
public class JavaUpgradeCheckStartupActivity implements ProjectActivity, DumbAware {
    
    // Additional delay after smart mode to ensure Maven/Gradle sync is complete
    private static final long POST_INDEXING_DELAY_SECONDS = 3;
    private static final int MAVEN_IMPORT_DEBOUNCE_MILLIS = 1000;
    private static final String COPILOT_PLUGIN_ID = "com.github.copilot";
    
    @Override
    public Object execute(@Nonnull Project project, @Nonnull Continuation<? super Unit> continuation) {
        final Alarm mavenImportAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, project);
        MavenProjectsManager.getInstance(project).addManagerListener(
            new MavenProjectsManager.Listener() {
                @Override
                public void projectImportCompleted() {
                    mavenImportAlarm.cancelAllRequests();
                    mavenImportAlarm.addRequest(
                        () -> performJavaUpgradeCheck(project, false),
                        MAVEN_IMPORT_DEBOUNCE_MILLIS
                    );
                }
            },
            project
        );

        // Wait for indexing to complete before running the check
        DumbService.getInstance(project).runWhenSmart(() -> {
            // Add a small delay after smart mode to ensure Maven/Gradle sync is done
            Mono.delay(Duration.ofSeconds(POST_INDEXING_DELAY_SECONDS))
                .subscribe(
                    next -> {
                        if (project.isDisposed()) {
                            return;
                        }
                        performJavaUpgradeCheck(project, true);
                    },
                    error -> {
                        /* Error during Java upgrade check startup */
                    log.error("Error during Java upgrade check startup for project: {}", project.getName(), error);
                    }
                );
        });
        
        return null;
    }
    
    /**
     * Performs the jdk version, framework version and CVE issue check and shows notifications for any issues found.
     */
    private void performJavaUpgradeCheck(@Nonnull Project project, boolean showNotification) {
        try {
            log.info("Starting Java upgrade issues detection for project: {}", project.getName());
            // Run the analysis in a background thread
            AzureTaskManager.getInstance().runInBackground("Checking Java upgrade issues", () -> {
                if (project.isDisposed()) {
                    return;
                }

                // Warm up Copilot's lazy chat-mode indexing once per project open. Copilot only scans
                // custom agents from .github/agents/ when its chat-mode registry is explicitly refreshed
                // (otherwise not until the chat panel is first opened), so we trigger that refresh now —
                // before the user clicks a fix action — so the agent is resolvable on the very first click.
                warmUpCopilotChatModes(project);
                
                // Refresh the cache (this populates JDK and dependency issues for use by inspections)
                final JavaUpgradeIssuesCache cache = JavaUpgradeIssuesCache.getInstance(project);
                cache.refresh();
                
                // Get all issues including CVEs
                final List<JavaUpgradeIssue> allIssues = new java.util.ArrayList<>();
                allIssues.addAll(cache.getJdkIssues());
                allIssues.addAll(cache.getDependencyIssues());
                allIssues.addAll(cache.getCveIssues());
                
                // Update UI on the main thread
                AzureTaskManager.getInstance().runLater(() -> {
                    if (project.isDisposed()) {
                        return;
                    }
                    
                    // Restart code analysis to refresh inspections in open editors
                    // This ensures wavy underlines appear for JDK/framework issues
                    DaemonCodeAnalyzer.getInstance(project).restart();
                    
                    // Show notifications if there are issues
                    if (showNotification && !allIssues.isEmpty()) {
                        final JavaVersionNotificationService notificationService = JavaVersionNotificationService.getInstance();
                        notificationService.showNotifications(project, allIssues);
                    }
                });
            });
            
        } catch (Throwable e) {
            // Error performing Java version check
            log.error("Error performing Java upgrade check for project: {}", project.getName(), e);
        }
    }

    private void warmUpCopilotChatModes(@Nonnull Project project) {
        try {
            final IdeaPluginDescriptor copilot = PluginManagerCore.getPlugin(PluginId.getId(COPILOT_PLUGIN_ID));
            if (copilot == null || !copilot.isEnabled() || copilot.getPluginClassLoader() == null) {
                return;
            }
            // Actively trigger Copilot to (re)scan custom agents so its chat-mode registry is populated
            // before the first fix-action click. Merely reading the chatModes StateFlow does NOT populate
            // it — only refreshChatModes() does — which is why a cold first click previously missed the agent.
            JavaVersionNotificationService.triggerChatModesRefresh(project, copilot.getPluginClassLoader());
        } catch (Throwable e) {
            // Best effort only; the fix action still falls back to its own URI path.
            log.warn("Failed to warm up Copilot Chat modes: {}", project.getName(), e);
        }
    }
}
