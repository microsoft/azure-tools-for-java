/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.JavaUpgradeIssue;
import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModUtils;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import static com.microsoft.azure.toolkit.intellij.appmod.common.AppModPluginInstaller.*;
import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.Constants.*;
import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesDetectionService.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to display notifications about outdated Java project versions.
 * Notifications appear in the bottom-right corner of the IDE.
 * Only shows one notification at a time (the first detected issue).
 */
@Slf4j
public class JavaVersionNotificationService {
    
    private static final String NOTIFICATION_GROUP_ID = "Azure Toolkit - Java Version Check";
    private static final String NOTIFICATIONS_ENABLED_KEY = "azure.toolkit.java.version.notifications.enabled";
    private static final String DEFERRED_UNTIL_KEY = "azure.toolkit.java.version.deferred_until";
    private static final long DEFER_INTERVAL_MS = 10 * 24 * 60 * 60 * 1000L; // 10 days in milliseconds
    private static final String DEFAULT_MODEL_NAME = "Claude Sonnet 4.6";

    // GitHub Copilot plugin ID
    private static final String COPILOT_PLUGIN_ID = "com.github.copilot";
    // App Modernization plugin (registers the custom agents we want to pre-select).
    private static final String APPMOD_PLUGIN_ID = "com.github.copilot.appmod";
    // Resolved from the Copilot plugin via reflection (older versions don't expose it).
    private static final String COPILOT_CHAT_MODE_SERVICE_CLASS = "com.github.copilot.agent.chatMode.ChatModeService";
    // Copilot indexes project-scope .github/agents/*.agent.md asynchronously after project open;
    // the first action can miss the agent for quite a while, so retry off-EDT for up to ~20 s.
    private static final int AGENT_RESOLVE_RETRY_ATTEMPTS = 100;
    private static final long AGENT_RESOLVE_RETRY_DELAY_MS = 200L;

    // Successfully resolved agent URIs, keyed by "<project location hash>|<agent name>". Once an agent
    // is resolved we never need to pay the retry cost again for that project, so repeated clicks are instant.
    private static final Map<String, Object> RESOLVED_AGENT_URIS = new ConcurrentHashMap<>();
    // Keys with an in-flight resolution (same key format). Lets rapid repeat clicks share/skip a single
    // ~20 s retry loop instead of each spawning its own sleeping thread.
    private static final Set<String> IN_FLIGHT_AGENT_RESOLUTIONS = ConcurrentHashMap.newKeySet();
    
    private static JavaVersionNotificationService instance;
    
    private JavaVersionNotificationService() {
    }
    
    public static synchronized JavaVersionNotificationService getInstance() {
        if (instance == null) {
            instance = new JavaVersionNotificationService();
        }
        return instance;
    }
    
    /**
     * Shows a notification for the highest-priority upgrade issue.
     * Only shows if notifications are enabled for the Java upgrade feature.
     *
     * @param project The project context
     * @param issues  List of detected issues
     */
    public void showNotifications(@Nonnull Project project, @Nonnull List<JavaUpgradeIssue> issues) {
        if (project.isDisposed() || issues.isEmpty()) {
            return;
        }
        
        // Check if notifications are enabled for this feature
        if (!isNotificationsEnabled(project)) {
            log.info("Java upgrade notifications are disabled.");
            return;
        }
        
        // Check if we should skip based on timing (deferred)
        if (!shouldCheckNow(project)) {
            log.info("Java upgrade notifications are deferred until later.");
            return;
        }
        
        showNotification(project, selectNotificationIssue(issues));
    }

    @Nonnull
    static JavaUpgradeIssue selectNotificationIssue(@Nonnull List<JavaUpgradeIssue> issues) {
        return issues.stream()
            .min(Comparator.comparingInt(JavaVersionNotificationService::getNotificationPriority))
            .orElseThrow();
    }

    private static int getNotificationPriority(@Nonnull JavaUpgradeIssue issue) {
        return switch (issue.getUpgradeReason()) {
            case JRE_TOO_OLD -> 0;
            case CVE -> 1;
            default -> 2;
        };
    }
    
    /**
     * Shows a single notification for an outdated version issue.
     */
    private void showNotification(@Nonnull Project project, 
                                   @Nonnull JavaUpgradeIssue issue) {
        final NotificationType notificationType = getNotificationType(issue.getSeverity());
        String title = issue.getTitle();
        if (issue.getEofDate() != null){
            //change 2020-06 to June 2020
            String formattedDate = formatEolDate(issue.getEofDate());
            title = String.format("%s (%s)", title, formattedDate);
        }
        final Notification notification = new Notification(
                NOTIFICATION_GROUP_ID,
                title,
                formatMessage(issue),
                notificationType
        );
        String issueStr = issue.toString();
        if (isAppModPluginInstalled()) {
            // Plugin is installed - show "Upgrade" action
            AppModUtils.logTelemetryEvent("showNotification.install.appmod", Map.of("javaupgrade.issue", issueStr));
            notification.addAction(new NotificationAction("Upgrade") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                    openCopilotChatWithUpgradePrompt(project, issue);
                }
            });
        } else {
            // Plugin is not installed - show "Install and Upgrade" action
            AppModUtils.logTelemetryEvent("showNotification.upgrade", Map.of("javaupgrade.issue", issueStr));
            notification.addAction(new NotificationAction("Install and Upgrade") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                    AppModPluginInstaller.showInstallConfirmation(project, true, () -> AppModPluginInstaller.installPlugin(project, true));
                }
            });
        }


        // Add "Not Now" action - defers the notification for 10 days
        notification.addAction(new NotificationAction("Not Now") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                deferNotifications();
                notification.expire();
            }
        });
        
        // Add "Don't Show Again" action - disables the entire Java upgrade notification feature
        notification.addAction(new NotificationAction("Don't Show Again") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                setNotificationsEnabled(false);
                notification.expire();
            }
        });
        
        // Show the notification
        Notifications.Bus.notify(notification, project);
    }
    
    /**
     * Formats the notification message with HTML for better display.
     */
    private String formatMessage(@Nonnull JavaUpgradeIssue issue) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        if (isAppModPluginInstalled()){
            sb.append(issue.getMessage());
        } else {
            sb.append(issue.getMessage() + TO_INSTALL_APP_MODE_PLUGIN);
        }
        sb.append(".");
        
//        if (issue.getCurrentVersion() != null && issue.getSuggestedVersion() != null) {
//            sb.append("<br/><br/>");
//            sb.append("<b>Current:</b> ").append(issue.getCurrentVersion());
//            sb.append(" → <b>Suggested:</b> ").append(issue.getSuggestedVersion());
//        }
        
        sb.append("</html>");
        return sb.toString();
    }
    
    /**
     * Gets the notification type based on issue severity.
     */
    private NotificationType getNotificationType(@Nonnull JavaUpgradeIssue.Severity severity) {
        return switch (severity) {
            case CRITICAL -> NotificationType.ERROR;
            case WARNING -> NotificationType.WARNING;
            case INFO -> NotificationType.INFORMATION;
        };
    }
    
    /**
     * Generates a unique key for an issue to track dismissals.
     */
    private String getIssueKey(@Nonnull JavaUpgradeIssue issue) {
        return issue.getPackageId() + ":" + 
               issue.getUpgradeReason().name() + ":" +
               Objects.requireNonNullElse(issue.getCurrentVersion(), "unknown");
    }
    
    /**
     * Checks if Java upgrade notifications are enabled globally.
     * @return true if notifications are enabled (default), false otherwise
     */
    public boolean isNotificationsEnabled() {
        final PropertiesComponent properties = PropertiesComponent.getInstance();
        return properties.getBoolean(NOTIFICATIONS_ENABLED_KEY, true);
    }
    
    /**
     * Checks if Java upgrade notifications are enabled for the project.
     * Uses application-level setting.
     * @param project The project (for backwards compatibility, not used)
     * @return true if notifications are enabled (default), false otherwise
     */
    public boolean isNotificationsEnabled(@Nonnull Project project) {
        return isNotificationsEnabled();
    }
    
    /**
     * Enables or disables Java upgrade notifications globally.
     * @param enabled true to enable notifications, false to disable
     */
    public void setNotificationsEnabled(boolean enabled) {
        final PropertiesComponent properties = PropertiesComponent.getInstance();
        properties.setValue(NOTIFICATIONS_ENABLED_KEY, enabled, true);
    }
    
    /**
     * Enables or disables Java upgrade notifications.
     * Uses application-level setting.
     * @param project The project (for backwards compatibility, not used)
     * @param enabled true to enable notifications, false to disable
     */
    public void setNotificationsEnabled(@Nonnull Project project, boolean enabled) {
        setNotificationsEnabled(enabled);
    }
    
    /**
     * Checks if the notification should be shown now.
     * Returns false if the user has clicked "Not Now" and the defer period hasn't passed.
     */
    private boolean shouldCheckNow(@Nonnull Project project) {
        final PropertiesComponent properties = PropertiesComponent.getInstance();
        final long deferredUntil = properties.getLong(DEFERRED_UNTIL_KEY, 0);
        final long now = System.currentTimeMillis();
        
        // If we're still in the deferred period, don't show notification
        return now >= deferredUntil;
    }
    
    /**
     * Defers notifications for 10 days.
     * Called when user clicks "Not Now".
     */
    public void deferNotifications() {
        final PropertiesComponent properties = PropertiesComponent.getInstance();
        final long deferUntil = System.currentTimeMillis() + DEFER_INTERVAL_MS;
        properties.setValue(DEFERRED_UNTIL_KEY, String.valueOf(deferUntil));
    }
    
    /**
     * Gets the timestamp until which notifications are deferred.
     * @return The deferred-until timestamp in milliseconds, or 0 if not deferred
     */
    public long getDeferredUntil() {
        final PropertiesComponent properties = PropertiesComponent.getInstance();
        return properties.getLong(DEFERRED_UNTIL_KEY, 0);
    }
    
    /**
     * Clears the deferred notification state.
     */
    public void clearDeferral() {
        final PropertiesComponent properties = PropertiesComponent.getInstance();
        properties.unsetValue(DEFERRED_UNTIL_KEY);
    }
    
    /**
     * Checks if upgrade is supported for this issue type.
     */
    private boolean isUpgradeSupported(@Nonnull JavaUpgradeIssue issue) {
        // Upgrade support for JDK and Spring Boot
        return issue.getUpgradeReason() == JavaUpgradeIssue.UpgradeReason.JRE_TOO_OLD ||
               issue.getPackageId().startsWith(GROUP_ID_SPRING_BOOT + ":");
    }
    
    /**
     * Opens GitHub Copilot chat in agent mode with an upgrade prompt.
     * @param project The project context
     * @param issue The upgrade issue to address
     */
    private void openCopilotChatWithUpgradePrompt(@Nonnull Project project, @Nonnull JavaUpgradeIssue issue) {
        final String prompt = buildUpgradePrompt(issue);
        openCopilotChatWithPrompt(project, prompt, APPMOD_UPGRADE_AGENT_NAME);
    }
    
    /**
     * Opens GitHub Copilot chat in agent mode with a given prompt.
     * Tries direct API first, falls back to reflection for cross-version compatibility.
     * @param project The project context
     * @param prompt The prompt to send to Copilot
     */
    public void openCopilotChatWithPrompt(@Nonnull Project project, @Nonnull String prompt) {
        try {
            AzureTaskManager.getInstance().runLater(() -> {
                if (!isAppModPluginInstalled()) {
                    // showGenericUpgradeGuidance(project, prompt);
                    AppModPluginInstaller.showInstallConfirmation(project, true, () -> AppModPluginInstaller.installPlugin(project, true));
                    return;
                }

//            //TODO Try direct API call first (works when plugin versions match)
//            if (tryDirectCopilotCall(project, prompt)) {
//                return; // Success, no need for reflection
//            }

                // Fallback to reflection for cross-version compatibility
                if (tryReflectionCopilotCall(project, prompt, null, null)) {
                    return; // Success via reflection
                }

                // Both approaches failed
                log.info("Failed to open Copilot chat via both direct and reflection methods.");
                showGenericUpgradeGuidance(project, prompt);
            });
        } catch (Exception e) {
            log.error("Error opening Copilot chat: " + e.getMessage());
            showGenericUpgradeGuidance(project, prompt);
        }
    }
    
    /**
     * Same as {@link #openCopilotChatWithPrompt(Project, String)} but pre-selects a Copilot custom
     * chat-mode (agent) by name (e.g. {@code "modernize-java-security"}). The URI is resolved on a
     * pooled background thread (with a short bounded retry to absorb Copilot's lazy {@code
     * .github/agents/} indexing on first project open) to keep the EDT responsive. When the agent
     * is not registered (older Copilot, appmod plugin not installed, etc.) we silently fall back
     * to plain Agent Mode.
     */
    public void openCopilotChatWithPrompt(@Nonnull Project project, @Nonnull String prompt,
                                          @Nullable String customAgentName) {
        if (customAgentName == null || customAgentName.isBlank()) {
            openCopilotChatWithPrompt(project, prompt);
            return;
        }
        // Capture this once so users without the appmod plugin don't pay the retry cost — we'll just
        // prompt to install on the EDT below.
        final boolean appmodInstalled = isAppModPluginInstalled();
        // De-dup in-flight resolution: while one pooled thread is already resolving this agent, drop
        // rapid repeat clicks instead of each spawning its own ~20 s sleeping thread.
        final String cacheKey = project.getLocationHash() + "|" + customAgentName;
        if (appmodInstalled && RESOLVED_AGENT_URIS.get(cacheKey) == null && !IN_FLIGHT_AGENT_RESOLUTIONS.add(cacheKey)) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Object uri = appmodInstalled ? RESOLVED_AGENT_URIS.get(cacheKey) : null;
            if (appmodInstalled && uri == null) {
                final IdeaPluginDescriptor copilot = PluginManagerCore.getPlugin(PluginId.getId(COPILOT_PLUGIN_ID));
                if (copilot != null && copilot.isEnabled()) {
                    final ClassLoader cl = copilot.getPluginClassLoader();
                    if (cl != null) {
                        // Copilot populates its chat-mode registry lazily (otherwise only after the chat
                        // panel is first opened). Without this active trigger the very first click finds an
                        // empty registry, the agent URI isn't honored, and the chat opens in default Agent
                        // Mode — so kick a refresh, then poll until the agent shows up.
                        triggerChatModesRefresh(project, cl);
                        for (int i = 0; i < AGENT_RESOLVE_RETRY_ATTEMPTS && !project.isDisposed(); i++) {
                            uri = resolveCustomAgentUri(project, cl, customAgentName);
                            if (uri != null) {
                                break;
                            }
                            try {
                                Thread.sleep(AGENT_RESOLVE_RETRY_DELAY_MS);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                        if (uri == null && !project.isDisposed()) {
                            log.info("openCopilotChatWithPrompt: agent '{}' not in ChatModeService after {} attempts ({} ms); will try file-based fallback.",
                                    customAgentName, AGENT_RESOLVE_RETRY_ATTEMPTS, AGENT_RESOLVE_RETRY_ATTEMPTS * AGENT_RESOLVE_RETRY_DELAY_MS);
                        }
                    }
                }
                // Fallback: ChatModeService is populated lazily by Copilot only after the first
                // chat-panel open (~17 s after triggering query()), so the first click after project
                // open will miss the agent via that path. Bypass it by pointing withAgentMode(...)
                // directly at the .agent.md file on disk; Copilot loads the definition on demand.
                if (uri == null && !project.isDisposed()) {
                    uri = tryConstructFileBasedAgentUri(project, customAgentName);
                    if (uri != null) {
                        log.info("openCopilotChatWithPrompt: ChatModeService didn't expose '{}'; using file-based fallback uri={}", customAgentName, uri);
                    }
                }
                if (uri != null) {
                    // Cache so subsequent clicks open instantly without re-running the retry loop.
                    RESOLVED_AGENT_URIS.put(cacheKey, uri);
                }
            }
            if (appmodInstalled) {
                IN_FLIGHT_AGENT_RESOLUTIONS.remove(cacheKey);
            }
            final Object preResolvedAgentUri = uri;
            AzureTaskManager.getInstance().runLater(() -> {
                if (!appmodInstalled) {
                    AppModPluginInstaller.showInstallConfirmation(project, true, () -> AppModPluginInstaller.installPlugin(project, true));
                    return;
                }
                if (tryReflectionCopilotCall(project, prompt, customAgentName, preResolvedAgentUri)) {
                    return;
                }
                log.info("Failed to open Copilot chat via both direct and reflection methods.");
                showGenericUpgradeGuidance(project, prompt);
            });
        });
    }

    /**
     * Tries to call CopilotChatService directly (works when compile-time and runtime versions match).
     * @return true if successful, false if an error occurred
     */
//    private boolean tryDirectCopilotCall(@Nonnull Project project, @Nonnull String prompt) {
//        try {
//            CopilotChatService service = project.getService(CopilotChatService.class);
//            if (service != null) {
//                service.query(DataContext.EMPTY_CONTEXT, builder -> {
//                    builder.withInput(prompt);
//                    builder.withAgentMode();
//                    builder.withNewSession();
//                    withModelCompatibility(builder, DEFAULT_MODEL_NAME);
//                    builder.withSessionIdReceiver(sessionId -> null);
//                    return null;
//                });
//                return true;
//            }
//        } catch (Error | Exception e) {
//            // Direct call failed (version mismatch, class not found, etc.) - will try reflection
//            log.info("Direct Copilot call failed: " +  e.getMessage());
//        }
//        return false;
//    }
    
    /**
     * Tries to call CopilotChatService via reflection for cross-version compatibility.
     * @param customAgentName     optional name of the Copilot custom chat-mode to select; null for default Agent Mode
     * @param preResolvedAgentUri optional URI already resolved off-EDT for {@code customAgentName};
     *                            when non-null, the lookup against ChatModeService is skipped
     * @return true if successful, false if an error occurred
     */
    private boolean tryReflectionCopilotCall(@Nonnull Project project, @Nonnull String prompt,
                                             @Nullable String customAgentName,
                                             @Nullable Object preResolvedAgentUri) {
        try {
            // Get the Copilot plugin's classloader to load its classes
            final IdeaPluginDescriptor copilotPlugin = PluginManagerCore.getPlugin(PluginId.getId(COPILOT_PLUGIN_ID));
            if (copilotPlugin == null || !copilotPlugin.isEnabled()) {
                return false;
            }

            final ClassLoader copilotClassLoader = copilotPlugin.getPluginClassLoader();
            if (copilotClassLoader == null) {
                return false;
            }

            // Use reflection to load CopilotChatService from the Copilot plugin's classloader
            Class<?> copilotChatServiceClass = copilotClassLoader.loadClass("com.github.copilot.api.CopilotChatService");
            Object service = project.getService(copilotChatServiceClass);

            if (service != null) {
                // Find the query method dynamically - signature may vary across Copilot versions
                Method queryMethod = findQueryMethod(copilotChatServiceClass);
                if (queryMethod == null) {
                    return false;
                }

                // Use Kotlin Function1 since the Copilot API is written in Kotlin
                Function1<Object, Unit> queryBuilder = builder -> {
                    try {
                        builder.getClass().getMethod("withInput", String.class).invoke(builder, prompt);
                        builder.getClass().getMethod("withNewSession").invoke(builder);
                        withLocalAgentProvider(builder, copilotClassLoader);
                        withModelCompatibility(builder, DEFAULT_MODEL_NAME);
                        Method withSessionIdReceiverMethod = findMethodByName(builder.getClass(), "withSessionIdReceiver");
                        if (withSessionIdReceiverMethod != null) {
                            Function1<String, Unit> sessionIdReceiver = sessionId -> Unit.INSTANCE;
                            withSessionIdReceiverMethod.invoke(builder, sessionIdReceiver);
                        }
                        applyAgentMode(builder, project, copilotClassLoader, customAgentName, preResolvedAgentUri);
                    } catch (Exception ex) {
                        // Error configuring query builder via reflection
                        log.error("Error configuring Copilot query via reflection: " + ex.getMessage());
                    }
                    return Unit.INSTANCE;
                };
                queryMethod.invoke(service, DataContext.EMPTY_CONTEXT, queryBuilder);
                AppModUtils.logTelemetryEvent("openCopilotChatForJavaUpgrade", Map.of("javaupgrade.prompt", prompt));
                return true;
            }
        } catch (Exception e) {
            // Reflection call failed
            log.error("Reflection Copilot call failed: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Switches the builder into Agent Mode, optionally selecting a custom agent by URI. Mirrors:
     * <pre>{@code
     * val uri = project.service<ChatModeService>().chatModes.value.firstOrNull { it.name == name }?.uri
     * if (uri != null) withAgentMode(uri) else withAgentMode()
     * }</pre>
     * Falls back to no-arg {@code withAgentMode()} on any failure so the chat still opens.
     */
    private static void applyAgentMode(@Nonnull Object builder, @Nonnull Project project,
                                       @Nonnull ClassLoader copilotClassLoader,
                                       @Nullable String customAgentName,
                                       @Nullable Object preResolvedAgentUri) {
        Object agentUri = preResolvedAgentUri;
        if (agentUri == null && customAgentName != null && !customAgentName.isBlank()) {
            // Caller didn't pre-resolve (or the off-EDT lookup returned null); try once here.
            agentUri = resolveCustomAgentUri(project, copilotClassLoader, customAgentName);
        }
        try {
            if (agentUri != null) {
                final Method withAgentMode = findAccessibleMethod(builder.getClass(), "withAgentMode", 0);
                if (withAgentMode != null) {
                    withAgentMode.invoke(builder);
                }
                final Method withAgentModeUri = findAccessibleMethod(builder.getClass(), "withAgentMode", 1);
                if (withAgentModeUri != null) {
                    // Copilot may declare withAgentMode as taking URI or String depending on version;
                    // coerce our agent URI to whichever the installed plugin expects.
                    final Object coerced = coerceToParameterType(agentUri, withAgentModeUri.getParameterTypes()[0]);
                    if (coerced != null) {
                        withAgentModeUri.invoke(builder, coerced);
                        log.info("applyAgentMode: selected Copilot custom agent '{}' via withAgentMode(uri) — uri={}", customAgentName, agentUri);
                        return;
                    }
                    log.warn("Resolved Copilot agent '{}' uri={} but cannot coerce to withAgentMode parameter type {}; using default Agent Mode.",
                            customAgentName, agentUri, withAgentModeUri.getParameterTypes()[0].getName());
                } else {
                    log.warn("Resolved Copilot agent '{}' but withAgentMode(uri) is not exposed by this Copilot version; using default Agent Mode.",
                            customAgentName);
                }
            } else {
                // agentUri == null: fallback to default Agent Mode
                if (customAgentName != null && !customAgentName.isBlank()) {
                    log.info("applyAgentMode: Copilot custom agent '{}' was not resolvable (not in chatModes and no on-disk file found); falling back to default Agent Mode.",
                            customAgentName);
                }
                final Method withAgentMode = findAccessibleMethod(builder.getClass(), "withAgentMode", 0);
                if (withAgentMode != null) {
                    withAgentMode.invoke(builder);
                    log.info("applyAgentMode: activated default Agent Mode");
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to apply Agent Mode via reflection: " + ex.getMessage(), ex);
        }
    }

    /**
     * Probes the well-known on-disk locations of {@code <name>.agent.md} and returns a {@link java.net.URI}
     * if found. Used as a fallback when {@link #resolveCustomAgentUri} can't see the agent yet because
     * Copilot's {@code ChatModeService} populates the list lazily on first chat-panel open.
     *
     * <p>Search order:
     * <ol>
     *   <li>Project-scope: {@code <project>/.github/agents/<name>.agent.md}</li>
     *   <li>Plugin-scope:  {@code <appmod-plugin>/mcp-server/dist/entrypoints/agents/<name>.agent.md}</li>
     * </ol>
     */
    @Nullable
    private static java.net.URI tryConstructFileBasedAgentUri(@Nonnull Project project, @Nonnull String customAgentName) {
        try {
            final String basePath = project.getBasePath();
            if (basePath != null) {
                final java.nio.file.Path projectFile = java.nio.file.Paths.get(
                        basePath, ".github", "agents", customAgentName + ".agent.md");
                if (java.nio.file.Files.isRegularFile(projectFile)) {
                    return toCopilotUri(projectFile);
                }
            }
            final IdeaPluginDescriptor appmod = PluginManagerCore.getPlugin(PluginId.getId(APPMOD_PLUGIN_ID));
            if (appmod != null && appmod.getPluginPath() != null) {
                final java.nio.file.Path pluginFile = appmod.getPluginPath()
                        .resolve(java.nio.file.Paths.get("mcp-server", "dist", "entrypoints", "agents",
                                customAgentName + ".agent.md"));
                if (java.nio.file.Files.isRegularFile(pluginFile)) {
                    return toCopilotUri(pluginFile);
                }
            }
        } catch (Exception ex) {
            log.warn("tryConstructFileBasedAgentUri failed for '" + customAgentName + "': " + ex.getMessage());
        }
        return null;
    }

    /**
     * Returns a {@link java.net.URI} for {@code path} in the exact form Copilot uses for its chat-mode
     * registry (VS Code convention: lowercase drive letter, percent-encoded colon on Windows). Copilot
     * does string-equality matching against this registry; a Java-standard {@code file:///C:/...} URI
     * is silently ignored even when it points to the same file. Non-Windows paths are returned as-is.
     *
     * <p>Limitation: only local drive-letter paths ({@code C:\...}) are rewritten. A Windows UNC path
     * ({@code \\server\share\...}) maps to {@code file://server/share/...} (no drive letter), doesn't
     * match the rewrite shape, and is returned as the Java-standard URI. If Copilot stored that agent
     * under a different UNC spelling, the string-equality match would miss and we'd fall back to plain
     * Agent Mode. This only affects projects/plugins hosted on a network share and is left unhandled
     * because Copilot's canonical UNC form isn't verifiable here; the file-based fallback simply no-ops.
     */
    @Nonnull
    private static java.net.URI toCopilotUri(@Nonnull java.nio.file.Path path) throws java.net.URISyntaxException {
        final java.net.URI standard = path.toUri();
        final String s = standard.toString();
        // Match file:///<DRIVE>:/... and rewrite to file:///<drive>%3A/...
        if (s.length() >= 11 && s.startsWith("file:///") && s.charAt(9) == ':' && Character.isLetter(s.charAt(8))) {
            return new java.net.URI("file:///" + Character.toLowerCase(s.charAt(8)) + "%3A" + s.substring(10));
        }
        // Non-Windows paths and Windows UNC paths (file://server/share/...) fall through unchanged; see Javadoc.
        return standard;
    }

    /**
     * Best-effort coercion of {@code value} into an instance of {@code paramType}. Returns {@code null}
     * when no safe conversion is possible. Handles the cases that actually occur in practice for
     * Copilot's {@code withAgentMode(...)}: {@link java.net.URI} ↔ {@link String}.
     */
    @Nullable
    private static Object coerceToParameterType(@Nullable Object value, @Nonnull Class<?> paramType) {
        if (value == null) {
            return null;
        }
        if (paramType.isInstance(value)) {
            return value;
        }
        if (paramType == String.class) {
            return value.toString();
        }
        if (paramType == java.net.URI.class && value instanceof String) {
            try {
                return new java.net.URI((String) value);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }


    /**
     * Reflectively calls {@code ChatModeService.refreshChatModes()} to make Copilot (re)scan custom
     * agents from {@code .github/agents/} and plugin-provided locations. Copilot populates its chat-mode
     * registry lazily — otherwise only after the chat panel is first opened — so without this trigger the
     * very first fix-action click finds an empty registry, the resolved agent URI isn't honored, and the
     * chat falls back to default Agent Mode. The refresh is asynchronous; callers poll {@code getChatModes}
     * afterward. Best-effort: older Copilot builds may not expose the method, in which case this no-ops.
     */
    public static void triggerChatModesRefresh(@Nonnull Project project, @Nonnull ClassLoader copilotClassLoader) {
        try {
            final Class<?> chatModeServiceClass = copilotClassLoader.loadClass(COPILOT_CHAT_MODE_SERVICE_CLASS);
            final Object chatModeService = project.getService(chatModeServiceClass);
            if (chatModeService == null) {
                return;
            }
            final Method refresh = findAccessibleMethod(chatModeService.getClass(), "refreshChatModes", 0);
            if (refresh != null) {
                refresh.invoke(chatModeService);
            } else {
                log.info("triggerChatModesRefresh: refreshChatModes() not exposed by this Copilot version.");
            }
        } catch (ClassNotFoundException ex) {
            // Older Copilot without ChatModeService; nothing to refresh.
            log.info("Older Copilot without ChatModeService");
        } catch (Exception ex) {
            log.info("triggerChatModesRefresh failed: " + ex.getMessage());
        }
    }

    /**
     * Reflectively resolves {@code chatModes.value.firstOrNull { it.name == name }?.uri} on
     * Copilot's {@code ChatModeService}. Returns {@code null} when the service is missing
     * (older Copilot), the agent is not yet registered, or any reflection step fails.
     */
    @Nullable
    private static Object resolveCustomAgentUri(@Nonnull Project project,
                                                @Nonnull ClassLoader copilotClassLoader,
                                                @Nonnull String customAgentName) {
        try {
            final Class<?> chatModeServiceClass = copilotClassLoader.loadClass(COPILOT_CHAT_MODE_SERVICE_CLASS);
            final Object chatModeService = project.getService(chatModeServiceClass);
            if (chatModeService == null) {
                return null;
            }
            final Method getChatModes = findAccessibleMethod(chatModeService.getClass(), "getChatModes", 0);
            if (getChatModes == null) {
                return null;
            }
            final Object flow = getChatModes.invoke(chatModeService);
            if (flow == null) {
                return null;
            }
            // StateFlow#getValue() may live on a package-private subclass (e.g. DerivedStateFlow);
            // findAccessibleMethod walks up to the public StateFlow interface.
            final Method getValue = findAccessibleMethod(flow.getClass(), "getValue", 0);
            final Object modes = getValue != null ? getValue.invoke(flow) : flow;
            if (!(modes instanceof Iterable<?>)) {
                return null;
            }
            for (Object mode : (Iterable<?>) modes) {
                if (mode == null) continue;
                final Method getName = findAccessibleMethod(mode.getClass(), "getName", 0);
                if (getName == null) continue;
                if (customAgentName.equals(String.valueOf(getName.invoke(mode)))) {
                    final Method getUri = findAccessibleMethod(mode.getClass(), "getUri", 0);
                    return getUri == null ? null : getUri.invoke(mode);
                }
            }
        } catch (ClassNotFoundException ex) {
            // Older Copilot without ChatModeService; caller will fall back to plain Agent Mode.
        } catch (Exception ex) {
            log.warn("Failed to resolve Copilot custom agent '" + customAgentName + "': " + ex.getMessage());
        }
        return null;
    }

    /**
     * Returns a publicly-invokable {@link Method} matching {@code name} and exact {@code parameterCount}.
     * Walks the runtime class, its superclasses and all interfaces (BFS) and returns the first match
     * whose <em>declaring class is public</em> — required because some Copilot return types are
     * package-private (e.g. Kotlin's {@code DerivedStateFlow}) and {@link Method#invoke} on a method
     * whose declaring class is non-public throws {@link IllegalAccessException}.
     */
    @Nullable
    public static Method findAccessibleMethod(@Nonnull Class<?> clazz, @Nonnull String name, int parameterCount) {
        final Deque<Class<?>> queue = new ArrayDeque<>();
        final Set<Class<?>> seen = new HashSet<>();
        queue.add(clazz);
        while (!queue.isEmpty()) {
            final Class<?> c = queue.poll();
            if (c == null || !seen.add(c)) continue;
            if (Modifier.isPublic(c.getModifiers())) {
                for (Method m : c.getDeclaredMethods()) {
                    if (!m.isSynthetic()
                            && name.equals(m.getName())
                            && m.getParameterCount() == parameterCount
                            && Modifier.isPublic(m.getModifiers())) {
                        return m;
                    }
                }
            }
            if (c.getSuperclass() != null) queue.add(c.getSuperclass());
            for (Class<?> iface : c.getInterfaces()) queue.add(iface);
        }
        return null;
    }

    /**
     * Shows generic guidance for upgrading when Copilot is not available.
     * @param project The project context
     * @param prompt The upgrade prompt that would be used
     */
    private void showGenericUpgradeGuidance(@Nonnull Project project, @Nonnull String prompt) {
        final String guidance = String.format(
            "Open GitHub Copilot chat and use the following prompt in agent mode:\n\n%s", prompt);
        
        final Notification guidanceNotification = new Notification(
            NOTIFICATION_GROUP_ID,
            "Upgrade Guidance",
            guidance,
            NotificationType.INFORMATION
        );
        
        Notifications.Bus.notify(guidanceNotification, project);
    }
    
    /**
     * Pins the query to the LOCAL agent provider via reflection.
     * This ensures the Java Upgrade prompt runs on the local (CLS) agent which hosts the
     * modernize-java-upgrade custom agent, instead of inheriting the home session's provider.
     * Requires Copilot plugin version that includes QueryOptionBuilder.withAgentProvider().
     * Silently no-ops on older versions.
     *
     * @param builder            the query option builder
     * @param copilotClassLoader the Copilot plugin's classloader
     */
    private static void withLocalAgentProvider(@Nonnull Object builder, @Nonnull ClassLoader copilotClassLoader) {
        try {
            final Class<?> typeClass = copilotClassLoader.loadClass("com.github.copilot.session.ChatTarget$Type");
            final Object localProvider = typeClass.getField("LOCAL").get(null);
            final Method withAgentProvider = findAccessibleMethod(builder.getClass(), "withAgentProvider", 1);
            if (withAgentProvider != null) {
                withAgentProvider.invoke(builder, localProvider);
                log.info("withLocalAgentProvider: pinned query to ChatTarget.Type.LOCAL");
            } else {
                log.info("withLocalAgentProvider: withAgentProvider() not exposed by this Copilot version; skipping.");
            }
        } catch (ClassNotFoundException ex) {
            // Older Copilot without ChatTarget.Type; silently skip.
            log.info("withLocalAgentProvider: ChatTarget.Type class not found; skipping.");
        } catch (NoSuchFieldException ex) {
            log.info("withLocalAgentProvider: ChatTarget.Type.LOCAL field not found; skipping.");
        } catch (Exception ex) {
            log.warn("withLocalAgentProvider failed: " + ex.getMessage());
        }
    }

    /**
     * Sets the model for the query builder using reflection for compatibility with older versions of GitHub Copilot.
     * Note: The API 'withModel' is supported starting from Copilot version '1.5.63'.
     * @param builder   the query option builder
     * @param modelName the name of the model to set
     */
    private static void withModelCompatibility(Object builder, String modelName) {
        try {
            builder.getClass().getMethod("withModel", String.class).invoke(builder, modelName);
        } catch (NoSuchMethodException ex) {
            // Method withModel not found in QueryOptionBuilder, skipping
        } catch (Exception ex) {
            // Error calling withModel via reflection, can be ignored
            log.error("Error setting model via reflection: " + ex.getMessage());
        }
    }
    
    /**
     * Finds the 'query' method in CopilotChatService dynamically.
     * The method signature may vary across Copilot versions.
     * @param serviceClass The CopilotChatService class
     * @return The query method, or null if not found
     */
    private Method findQueryMethod(Class<?> serviceClass) {
        for (Method method : serviceClass.getMethods()) {
            if ("query".equals(method.getName()) && method.getParameterCount() == 2) {
                Class<?>[] paramTypes = method.getParameterTypes();
                // Look for query(DataContext, Function/Consumer/etc)
                if (DataContext.class.isAssignableFrom(paramTypes[0])) {
                    return method;
                }
            }
        }
        return null;
    }
    
    /**
     * Finds a method by name in a class (first match).
     * @param clazz The class to search
     * @param methodName The method name to find
     * @return The method, or null if not found
     */
    private Method findMethodByName(Class<?> clazz, String methodName) {
        for (Method method : clazz.getMethods()) {
            if (methodName.equals(method.getName())) {
                return method;
            }
        }
        return null;
    }
    
    /**
     * Builds the upgrade prompt for Copilot based on the issue type.
     * @param issue The upgrade issue
     * @return The prompt string for Copilot
     */
    private String buildUpgradePrompt(@Nonnull JavaUpgradeIssue issue) {
        if (issue.getUpgradeReason() == JavaUpgradeIssue.UpgradeReason.CVE){
            return String.format(FIX_VULNERABLE_DEPENDENCY_WITH_COPILOT_PROMPT, issue.getPackageId());
        }
        return String.format(UPGRADE_JAVA_FRAMEWORK_PROMPT, issue.getPackageDisplayName(), issue.getCurrentVersion(), issue.getSuggestedVersion());
    }
    
    /**
     * Shows guidance for upgrading the project when Copilot is not available.
     * @param project The project context
     * @param issue The upgrade issue
     * @param prompt The upgrade prompt that would be used
     */
    private void showUpgradeGuidance(@Nonnull Project project, @Nonnull JavaUpgradeIssue issue, @Nonnull String prompt) {
        showGenericUpgradeGuidance(project, prompt);
    }
    
    /**
     * Resets notification settings (useful for testing or reset).
     * Re-enables notifications and clears the deferral.
     */
    public void resetNotificationSettings() {
        final PropertiesComponent properties = PropertiesComponent.getInstance();
        properties.unsetValue(NOTIFICATIONS_ENABLED_KEY);
        properties.unsetValue(DEFERRED_UNTIL_KEY);
    }
    
    /**
     * Resets notification settings for a project (for backwards compatibility).
     * @param project The project (not used, settings are application-level)
     */
    public void resetNotificationSettings(@Nonnull Project project) {
        resetNotificationSettings();
    }
}
