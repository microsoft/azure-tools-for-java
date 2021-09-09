/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.base;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.plugins.PluginStateListener;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.PermanentInstallationID;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.PlatformUtils;
import com.microsoft.azure.toolkit.intellij.common.action.WhatsNewAction;
import com.microsoft.azure.toolkit.intellij.common.settings.AzureConfigurations;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.EventListenerList;
import java.util.Objects;

public class AzurePlugin implements StartupActivity.DumbAware {
    private static final Logger LOG = Logger.getInstance("#com.microsoft.intellij.AzurePlugin");
    public static final String PLUGIN_ID = "com.microsoft.tooling.msservices.intellij.azure";
    public static final String PLUGIN_NAME = "azure-toolkit-for-intellij";
    public static final String PLUGIN_VERSION = Objects.requireNonNull(PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))).getVersion();
    public static final String AZURE_LIBRARIES_VERSION = "1.0.0";
    public static final String JDBC_LIBRARIES_VERSION = "6.1.0.jre8";
    public static final int REST_SERVICE_MAX_RETRY_COUNT = 7;
    private static final PluginStateListener pluginStateListener = null;
    private static final int POP_UP_DELAY = 30;

    // User-agent header for Azure SDK calls
    public static final String USER_AGENT = "Azure Toolkit for IntelliJ, v%s, machineid:%s";

    public static boolean IS_WINDOWS = SystemInfo.isWindows;

    public static boolean IS_ANDROID_STUDIO = "AndroidStudio".equals(PlatformUtils.getPlatformPrefix());

    private static final EventListenerList DEPLOYMENT_EVENT_LISTENERS = new EventListenerList();

    private Boolean firstInstallationByVersion;

    @Override
    public void runActivity(@NotNull Project project) {

        // read legacy settings from old data.xml
        final String installationId = "wangmi-azure-intellij-plugin-base";
        final String pluginVersion = "0.0.1-SNAPSHOT";
        // check non-empty for valid data.xml
        if (StringUtils.isNoneBlank(installationId, pluginVersion)) {
            final AzureConfigurations.AzureConfigurationData config = AzureConfigurations.getInstance().getState();
            AzureConfigurations.getInstance().loadState(config);
        }
        final AzureConfigurations.AzureConfigurationData config = AzureConfigurations.getInstance().getState();
        String installationID = InstallationIdUtils.getHashMac();
        if (StringUtils.isBlank(installationID)) {
            installationID = StringUtils.firstNonBlank(InstallationIdUtils.getHashMac(), InstallationIdUtils.hash(PermanentInstallationID.get()));
        }

        final String userAgent = String.format(USER_AGENT, PLUGIN_VERSION, installationID);
        Azure.az().config().setLogLevel("NONE");
        Azure.az().config().setUserAgent(userAgent);
        final AnAction action = ActionManager.getInstance().getAction(WhatsNewAction.ID);
        final DataContext context = dataId -> CommonDataKeys.PROJECT.getName().equals(dataId) ? project : null;
        AzureTaskManager.getInstance().runLater(() -> ActionUtil.invokeAction(action, context, "AzurePluginStartupActivity", null, null));
    }
}
