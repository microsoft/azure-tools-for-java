/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.common;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.PluginsAdvertiser;
import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModUtils;
import com.microsoft.azure.toolkit.intellij.appmod.utils.Constants;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for managing App Modernization plugin installation.
 * This centralizes all plugin detection and installation logic to avoid code duplication
 * between MigrateToAzureNode and MigrateToAzureAction.
 */
@Slf4j
public class AppModPluginInstaller {
    private static final String PLUGIN_ID = "com.github.copilot.appmod";
    private static final String COPILOT_PLUGIN_ID = "com.github.copilot";
    public static final String TO_INSTALL_APP_MODE_PLUGIN = " (Install " + Constants.APPMOD_NAME + ")";
    private AppModPluginInstaller() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Checks if the App Modernization plugin is installed and enabled.
     */
    public static boolean isAppModPluginInstalled() {
        try {
            final PluginId pluginId = PluginId.getId(PLUGIN_ID);
            final IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(pluginId);
            final boolean result = plugin != null && plugin.isEnabled();
            log.debug("[AppModPluginInstaller] isAppModPluginInstalled: {}", result);
            return result;
        } catch (Exception e) {
            log.error("[AppModPluginInstaller] Failed to check AppMod plugin status", e);
            return false;
        }
    }
    
    /**
     * Checks if GitHub Copilot plugin is installed and enabled.
     */
    public static boolean isCopilotInstalled() {
        try {
            final PluginId pluginId = PluginId.getId(COPILOT_PLUGIN_ID);
            final IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(pluginId);
            final boolean result = plugin != null && plugin.isEnabled();
            log.debug("[AppModPluginInstaller] isCopilotInstalled: {}", result);
            return result;
        } catch (Exception e) {
            log.error("[AppModPluginInstaller] Failed to check Copilot plugin status", e);
            return false;
        }
    }
    
    /**
     * Detects if running in development mode (runIde task).
     * This helps determine whether to show restart options or dev-mode instructions.
     */
    public static boolean isRunningInDevMode() {
        try {
            final PluginId azureToolkitId = PluginId.getId("com.microsoft.tooling.msservices.intellij.azure");
            final IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(azureToolkitId);
            if (descriptor != null) {
                final String path = descriptor.getPluginPath().toString();
                final boolean result = path.contains("build") || path.contains("sandbox") || path.contains("out");
                log.debug("[AppModPluginInstaller] isRunningInDevMode: {}, path: {}", result, path);
                return result;
            }
        } catch (Exception ex) {
            log.error("[AppModPluginInstaller] Failed to check dev mode status", ex);
        }
        return false;
    }
    
    /**
     * Shows a confirmation dialog for plugin installation.
     * 
     * @param project The current project
     * @param forUpgrade true for "upgrade" scenario, false for "migrate to Azure" scenario
     * @param onConfirm Callback to execute when user confirms installation (if null, calls installPlugin directly)
     */
    public static void showInstallConfirmation(@Nonnull Project project, boolean forUpgrade, @Nonnull Runnable onConfirm) {
        final boolean copilotInstalled = isCopilotInstalled();
        final String action = forUpgrade ? "upgrade" : "migration";
        log.debug("[AppModPluginInstaller] showInstallConfirmation - forUpgrade: {}, copilotInstalled: {}", forUpgrade, copilotInstalled);

        final String title = "Install " + Constants.APPMOD_NAME;

        final String message;
        if (copilotInstalled) {
            message = forUpgrade
                    ? "Install this plugin to upgrade your apps with Copilot."
                    : "Install this plugin to automate migrating your apps to Azure with Copilot.";
        } else {
            message = forUpgrade
                    ? "To upgrade your apps, you'll need to install GitHub Copilot modernization."
                    : "To migrate to Azure, you'll need to install GitHub Copilot modernization.";
        }
        AppModUtils.logTelemetryEvent("plugin." + action + ".install-prompt-shown", Map.of("copilotInstalled", String.valueOf(copilotInstalled)));
        if (Messages.showOkCancelDialog(project, message, title, "Install", "Cancel", Messages.getQuestionIcon()) == Messages.OK) {
            log.info("[AppModPluginInstaller] User confirmed plugin installation for {}", action);
            AppModUtils.logTelemetryEvent("plugin." + action + ".install-confirmed");
            onConfirm.run();
        } else {
            log.info("[AppModPluginInstaller] User cancelled plugin installation for {}", action);
            AppModUtils.logTelemetryEvent("plugin." + action + ".install-cancelled");
        }
    }
    

    /**
     * Installs the App Modernization plugin.
     * IntelliJ platform will automatically install Copilot as a dependency if AppMod declares it.
     * 
     * @param project The current project
     * @param forUpgrade true for "upgrade" scenario, false for "migrate to Azure" scenario
     */
    public static void installPlugin(@Nonnull Project project, boolean forUpgrade) {
        log.debug("[AppModPluginInstaller] installPlugin - forUpgrade: {}", forUpgrade);
        if (isAppModPluginInstalled()) {
            log.debug("[AppModPluginInstaller] installPlugin - skipping, already installed");
            return;
        }
        
        // Only pass AppMod ID - IntelliJ will automatically install Copilot as dependency
        final Set<PluginId> pluginsToInstall = new LinkedHashSet<>();
        pluginsToInstall.add(PluginId.getId(PLUGIN_ID));
        
        final String source = forUpgrade ? "upgrade" : "migration";
        log.info("[AppModPluginInstaller] Starting plugin installation via PluginsAdvertiser");
        PluginsAdvertiser.installAndEnable(
            project,
            pluginsToInstall,
            true,   // showDialog
            true,   // selectAllInDialog - pre-select all plugins
            null,   // modalityState
            () -> {
                log.info("[AppModPluginInstaller] Plugin installation completed");
                AppModUtils.logTelemetryEvent("plugin." + source + ".install-complete");
            }
        );
    }
}
