/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.containerservice.actions;

import com.intellij.ide.plugins.PluginManagerConfigurable;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public class KubernetesUtils {
    public static final String KUBERNETES_PLUGIN_ID = "com.intellij.kubernetes";
    public static final String REDHAT_KUBERNETES_PLUGIN_ID = "com.redhat.devtools.intellij.kubernetes";
    private static final Map<String, String> KUBERNETES_TERMINAL_MAP = new LinkedHashMap<>() {{
        put(KUBERNETES_PLUGIN_ID, "Services");
        put(REDHAT_KUBERNETES_PLUGIN_ID, "Kubernetes");
    }};

    private static final Map<String, String> KUBERNETES_REFRESH_ACTION_MAP = new LinkedHashMap<>() {{
        put(KUBERNETES_PLUGIN_ID, "Kubernetes.RefreshConfiguration");
        put(REDHAT_KUBERNETES_PLUGIN_ID, "com.redhat.devtools.intellij.kubernetes.actions.RefreshAction");
    }};

    public static Action<?> getConnectKubernetesActions(@Nonnull final Project project) {
        final String installedPluginId = getInstalledKubernetesPlugin();
        if (StringUtils.isNoneBlank(installedPluginId)) {
            return getOpenInKubernetesPluginAction(project, installedPluginId);
        }
        return StringUtils.isNoneBlank(installedPluginId) ?
                getOpenInKubernetesPluginAction(project, installedPluginId) :
                getRecommendKubernetesPluginAction(project);
    }

    private static Action<?> getOpenInKubernetesPluginAction(@Nonnull final Project project, String installedPluginId) {
        return new Action<>(Action.Id.of("user/kubernetes.open_kubernetes_plugin"))
            .withLabel("Open in kubernetes plugin")
            .withHandler(ignore -> openInKubernetesPlugin(project, installedPluginId));
    }

    private static void openInKubernetesPlugin(@Nonnull final Project project, String pluginId) {
        final AnAction action = ActionManager.getInstance().getAction(KUBERNETES_REFRESH_ACTION_MAP.get(pluginId));
        final String windowId = KUBERNETES_TERMINAL_MAP.get(pluginId);
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(windowId);
        if (toolWindow == null || action == null) {
            AzureMessager.getMessager().warning(AzureString.format("Failed to get kubernetes plugin, please check whether plugin %s is enabled", pluginId));
            return;
        }
        final DataContext context = dataId -> CommonDataKeys.PROJECT.getName().equals(dataId) ? project : null;
        AzureTaskManager.getInstance().runLater(() -> doCallK8sPlugin(action, toolWindow, context));
    }

    @AzureOperation(name = "boundary/kubernetes.open_in_k8s_plugin")
    private static void doCallK8sPlugin(AnAction action, ToolWindow toolWindow, DataContext context) {
        ActionUtil.invokeAction(action, context, "KubernetesNotification", null, null);
        toolWindow.activate(null);
    }

    private static Action<?> getRecommendKubernetesPluginAction(@Nonnull final Project project) {
        return new Action<>(Action.Id.of("user/kubernetes.install_kubernetes_plugin"))
            .withLabel("Install kubernetes plugin")
            .withHandler(ignore -> AzureTaskManager.getInstance().runLater(()-> searchK8sPlugin(project)));
    }

    @AzureOperation(name = "boundary/kubernetes.search_k8s_plugin")
    private static void searchK8sPlugin(@Nonnull final Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, PluginManagerConfigurable.class, it ->
                it.openMarketplaceTab("/tag: \"Cloud\" Kubernetes")
        );
    }

    @Nullable
    private static String getInstalledKubernetesPlugin() {
        return KUBERNETES_TERMINAL_MAP.keySet().stream()
            .filter(id -> PluginManagerCore.isPluginInstalled(PluginId.getId(id)))
            .findFirst().orElse(null);
    }
}
