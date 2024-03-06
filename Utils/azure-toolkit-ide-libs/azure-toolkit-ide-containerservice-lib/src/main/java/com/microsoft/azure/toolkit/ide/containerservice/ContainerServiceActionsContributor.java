/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.containerservice;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesCluster;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import io.kubernetes.client.util.KubeConfig;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.INVOKE_COMMAND_IN_TERMINAL;

public class ContainerServiceActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.kubernetes.service";
    public static final String CLUSTER_ACTIONS = "actions.kubernetes.cluster";

    public static final String AGENT_POOL_ACTIONS = "actions.kubernetes.agent_pool";

    public static final Action.Id<KubernetesCluster> DOWNLOAD_CONFIG_ADMIN = KubernetesCluster.DOWNLOAD_CONFIG_ADMIN;
    public static final Action.Id<KubernetesCluster> DOWNLOAD_CONFIG_USER = KubernetesCluster.DOWNLOAD_CONFIG_USER;
    public static final Action.Id<KubernetesCluster> GET_CREDENTIAL_ADMIN = Action.Id.of("user/kubernetes.get_credential_admin.kubernetes");
    public static final Action.Id<KubernetesCluster> GET_CREDENTIAL_USER = Action.Id.of("user/kubernetes.get_credential_user.kubernetes");
    public static final Action.Id<KubernetesCluster> OPEN_KUBERNETES_PLUGIN = Action.Id.of("user/kubernetes.open_kubernetes_plugin.kubernetes");
    public static final Action.Id<KubernetesCluster> LIST_NODES = Action.Id.of("user/kubernetes.list_nodes.kubernetes");
    public static final Action.Id<KubernetesCluster> LIST_SERVICES = Action.Id.of("user/kubernetes.list_services.kubernetes");

    public static final Action.Id<ResourceGroup> GROUP_CREATE_KUBERNETES_SERVICE = Action.Id.of("user/kubernetes.create_kubernetes.group");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(GROUP_CREATE_KUBERNETES_SERVICE)
            .withLabel("Kubernetes service")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceGroup)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);
        new Action<>(DOWNLOAD_CONFIG_ADMIN)
            .withLabel("Download Kubeconfig (Admin)")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof KubernetesCluster)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(DOWNLOAD_CONFIG_USER)
            .withLabel("Download Kubeconfig (User)")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof KubernetesCluster)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(GET_CREDENTIAL_ADMIN)
            .withLabel("Set as Current Cluster (Admin)")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof KubernetesCluster)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(GET_CREDENTIAL_USER)
            .withLabel("Set as Current Cluster (User)")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof KubernetesCluster)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(OPEN_KUBERNETES_PLUGIN)
            .withLabel("Open with Kubernetes Plugin")
            .withIdParam(AzResource::getName)
            .withIcon(AzureIcons.Kubernetes.MODULE.getIconPath())
            .visibleWhen(s -> s instanceof KubernetesCluster)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(LIST_NODES)
            .withLabel("List Nodes")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof KubernetesCluster)
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .withHandler((cluster, event) -> executeKubectlCommand(cluster, "kubectl get node --all-namespaces=true", event))
            .register(am);

        new Action<>(LIST_SERVICES)
            .withLabel("List Services")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof KubernetesCluster)
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .withHandler((cluster, event) -> executeKubectlCommand(cluster, "kubectl get service --all-namespaces=true", event))
            .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.BROWSE_SERVICE_AZURE_SAMPLES,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup registryActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.BROWSE_SERVICE_AZURE_SAMPLES,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            "---",
            ResourceCommonActionsContributor.START,
            ResourceCommonActionsContributor.STOP,
            ResourceCommonActionsContributor.DELETE,
            "---",
            ContainerServiceActionsContributor.DOWNLOAD_CONFIG_ADMIN,
            ContainerServiceActionsContributor.DOWNLOAD_CONFIG_USER,
            ContainerServiceActionsContributor.GET_CREDENTIAL_ADMIN,
            ContainerServiceActionsContributor.GET_CREDENTIAL_USER,
            "---",
            ContainerServiceActionsContributor.LIST_NODES,
            ContainerServiceActionsContributor.LIST_SERVICES,
            "---",
            ContainerServiceActionsContributor.OPEN_KUBERNETES_PLUGIN
        );
        am.registerGroup(CLUSTER_ACTIONS, registryActionGroup);

        final ActionGroup agentPoolGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(AGENT_POOL_ACTIONS, agentPoolGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_KUBERNETES_SERVICE);
    }

    public static void executeKubectlCommand(@Nonnull final KubernetesCluster cluster,
                                             @Nonnull final String command, final Object event) {
        final String currentContext = getCurrentContext();
        final String defaultContextName = cluster.getName();
        final String adminContextName = cluster.getName() + "-admin";
        if (!StringUtils.equalsAnyIgnoreCase(currentContext, defaultContextName, adminContextName)) {
            AzureActionManager.getInstance().getAction(GET_CREDENTIAL_USER).instantiate(cluster, event).perform();
        }
        AzureActionManager.getInstance().getAction(INVOKE_COMMAND_IN_TERMINAL).handle(command, event);
    }

    @Nullable
    public static String getCurrentContext() {
        final File configFile = Path.of(System.getProperty("user.home"), KubeConfig.KUBEDIR, KubeConfig.KUBECONFIG).toFile();
        if (!configFile.exists() || configFile.getTotalSpace() == 0) {
            return null;
        }
        try (final FileReader reader = new FileReader(configFile)) {
            final KubeConfig origin = KubeConfig.loadKubeConfig(reader);
            return origin.getCurrentContext();
        } catch (final IOException e) {
            return null;
        }
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
