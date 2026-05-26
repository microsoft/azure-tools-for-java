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

import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
     * Shows a notification for the first outdated version issue.
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
        
        // Only show notification for the first issue
        final JavaUpgradeIssue firstIssue = issues.get(0);
        showNotification(project, firstIssue);
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
        openCopilotChatWithPrompt(project, prompt);
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
                if (tryReflectionCopilotCall(project, prompt)) {
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
     * @return true if successful, false if an error occurred
     */
    private boolean tryReflectionCopilotCall(@Nonnull Project project, @Nonnull String prompt) {
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
                        builder.getClass().getMethod("withAgentMode").invoke(builder);
                        builder.getClass().getMethod("withNewSession").invoke(builder);
                        withModelCompatibility(builder, DEFAULT_MODEL_NAME);
                        Method withSessionIdReceiverMethod = findMethodByName(builder.getClass(), "withSessionIdReceiver");
                        if (withSessionIdReceiverMethod != null) {
                            Function1<String, Unit> sessionIdReceiver = sessionId -> Unit.INSTANCE;
                            withSessionIdReceiverMethod.invoke(builder, sessionIdReceiver);
                        }
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
