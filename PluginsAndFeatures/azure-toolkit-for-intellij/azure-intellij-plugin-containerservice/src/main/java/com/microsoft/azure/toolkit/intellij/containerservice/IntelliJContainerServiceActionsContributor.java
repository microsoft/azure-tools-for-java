/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerservice;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.util.PlatformUtils;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.containerservice.ContainerServiceActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijActionsContributor;
import com.microsoft.azure.toolkit.intellij.containerservice.actions.DownloadKubuConfigAction;
import com.microsoft.azure.toolkit.intellij.containerservice.actions.GetKubuCredentialAction;
import com.microsoft.azure.toolkit.intellij.containerservice.actions.KubernetesUtils;
import com.microsoft.azure.toolkit.intellij.containerservice.creation.CreateKubernetesServiceAction;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.containerservice.AzureContainerService;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesCluster;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import static com.microsoft.azure.toolkit.intellij.containerservice.creation.CreateKubernetesServiceAction.getDefaultConfig;

public class IntelliJContainerServiceActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> serviceCondition = (r, e) -> r instanceof AzureContainerService;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) ->
                CreateKubernetesServiceAction.create(e.getProject(), getDefaultConfig(null));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, serviceCondition, handler);

        final BiConsumer<ResourceGroup, AnActionEvent> groupCreateHandler = (r, e) ->
                CreateKubernetesServiceAction.create(e.getProject(), getDefaultConfig(r));
        am.registerHandler(ContainerServiceActionsContributor.GROUP_CREATE_KUBERNETES_SERVICE, (r, e) -> true, groupCreateHandler);

        final BiPredicate<KubernetesCluster, AnActionEvent> clusterCondition = (r, e) -> r != null;
        am.registerHandler(ContainerServiceActionsContributor.GET_CREDENTIAL_USER, clusterCondition, (c, e) ->
                GetKubuCredentialAction.getKubuCredential(c, e.getProject(), false));
        am.registerHandler(ContainerServiceActionsContributor.GET_CREDENTIAL_ADMIN, clusterCondition, (c, e) ->
                GetKubuCredentialAction.getKubuCredential(c, e.getProject(), true));
        am.registerHandler(ContainerServiceActionsContributor.DOWNLOAD_CONFIG_USER, clusterCondition, (c, e) ->
                DownloadKubuConfigAction.downloadKubuConfig(c, e.getProject(), false));
        am.registerHandler(ContainerServiceActionsContributor.DOWNLOAD_CONFIG_ADMIN, clusterCondition, (c, e) ->
                DownloadKubuConfigAction.downloadKubuConfig(c, e.getProject(), true));

        if (!KubernetesUtils.isKubernetesPluginEnabled()) {
            am.registerHandler(ContainerServiceActionsContributor.OPEN_KUBERNETES_PLUGIN, clusterCondition,
                    (c, e) -> showKubernetesPluginNotification(c, e.getProject()));
        }
    }

    private void showKubernetesPluginNotification(@Nonnull KubernetesCluster cluster, @Nullable Project project) {
        final String KUBERNETES_PLUGIN_NOT_INSTALLED = "\"Kubernetes\" plugin is not installed.";
        final String NOT_SUPPORT_ERROR_ACTION = "\"Kubernetes\" plugin is only provided in IntelliJ Ultimate edition.";
        if (!KubernetesUtils.isKubernetesPluginEnabled()) {
            final Action<Object> tryUltimate = AzureActionManager.getInstance().getAction(IntellijActionsContributor.TRY_ULTIMATE);
            final Action<?> installPlugin = KubernetesUtils.getInstallKubernetesPluginAction();
            throw PlatformUtils.isIdeaCommunity() ?
                    new AzureToolkitRuntimeException(KUBERNETES_PLUGIN_NOT_INSTALLED, NOT_SUPPORT_ERROR_ACTION, tryUltimate) :
                    new AzureToolkitRuntimeException(KUBERNETES_PLUGIN_NOT_INSTALLED, installPlugin) ;
        } else {
            AzureMessager.getMessager().info("Please restart the IDE to enable 'Open with Kubernetes Plugin' action.");
        }
    }

    @Override
    public int getOrder() {
        return ContainerServiceActionsContributor.INITIALIZE_ORDER + 1;
    }

}
