/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.containerservice.actions;

import com.intellij.ide.plugins.PluginManagerConfigurable;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import java.util.Optional;

public class KubernetesUtils {
    public static final String KUBERNETES_PLUGIN_ID = "com.intellij.kubernetes";
    public static final String REDHAT_KUBERNETES_PLUGIN_ID = "com.redhat.devtools.intellij.kubernetes";

    public static boolean isKubernetesPluginEnabled() {
        return Optional.ofNullable(PluginManagerCore.getPlugin(PluginId.findId(KUBERNETES_PLUGIN_ID)))
                .map(plugin -> plugin.isEnabled()).orElse(false);
    }

    public static Action<?> getInstallKubernetesPluginAction() {
        return new Action<>(Action.Id.of("user/kubernetes.install_kubernetes_plugin"))
                .withLabel("Install kubernetes plugin")
                .withHandler(ignore -> AzureTaskManager.getInstance().runLater(KubernetesUtils::searchK8sPlugin));
    }

    @AzureOperation(name = "boundary/kubernetes.search_k8s_plugin")
    private static void searchK8sPlugin() {
        ShowSettingsUtil.getInstance().editConfigurable(null, new PluginManagerConfigurable(), it ->
                it.openMarketplaceTab("/tag: \"Cloud\" Kubernetes")
        );
    }
}
