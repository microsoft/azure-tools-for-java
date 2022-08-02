/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.containerservice;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesCluster;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class ContainerServiceActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.kubernetes.service";
    public static final String CLUSTER_ACTIONS = "actions.kubernetes.cluster";

    public static final String AGENT_POOL_ACTIONS = "actions.kubernetes.agent_pool";

    public static final Action.Id<KubernetesCluster> DOWNLOAD_CONFIG_ADMIN = Action.Id.of("kubernetes.kubu_config_admin");
    public static final Action.Id<KubernetesCluster> DOWNLOAD_CONFIG_USER = Action.Id.of("kubernetes.kubu_config_user");
    public static final Action.Id<KubernetesCluster> GET_CREDENTIAL_ADMIN = Action.Id.of("kubernetes.get_credential_admin");
    public static final Action.Id<KubernetesCluster> GET_CREDENTIAL_USER = Action.Id.of("kubernetes.get_credential_user");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_KUBERNETES_SERVICE = Action.Id.of("group.create_kubernetes");
    @Override
    public void registerActions(AzureActionManager am) {
        final ActionView.Builder createClusterView = new ActionView.Builder("Kubernetes service")
                .title(s -> Optional.ofNullable(s).map(r ->
                        description("group.create_kubernetes.group", ((ResourceGroup) r).getName())).orElse(null))
                .enabled(s -> s instanceof ResourceGroup);
        am.registerAction(GROUP_CREATE_KUBERNETES_SERVICE, new Action<>(GROUP_CREATE_KUBERNETES_SERVICE, createClusterView));

        final ActionView.Builder adminConfigView = new ActionView.Builder("Download Kubeconfig (Admin)")
                .title(s -> Optional.ofNullable(s).map(r -> description("kubernetes.kubu_config_admin.kubernetes", ((KubernetesCluster) r).getName())).orElse(null))
                .enabled(s -> s instanceof KubernetesCluster && ((KubernetesCluster) s).getFormalStatus().isConnected());
        am.registerAction(DOWNLOAD_CONFIG_ADMIN, new Action<>(DOWNLOAD_CONFIG_ADMIN, adminConfigView));

        final ActionView.Builder userConfigView = new ActionView.Builder("Download Kubeconfig (User)")
                .title(s -> Optional.ofNullable(s).map(r -> description("kubernetes.kubu_config_user.kubernetes", ((KubernetesCluster) r).getName())).orElse(null))
                .enabled(s -> s instanceof KubernetesCluster && ((KubernetesCluster) s).getFormalStatus().isConnected());
        am.registerAction(DOWNLOAD_CONFIG_USER, new Action<>(DOWNLOAD_CONFIG_USER, userConfigView));

        final ActionView.Builder getAdminCredentialView = new ActionView.Builder("Set as Current Cluster (Admin)")
                .title(s -> Optional.ofNullable(s).map(r -> description("kubernetes.get_credential_admin.kubernetes", ((KubernetesCluster) r).getName())).orElse(null))
                .enabled(s -> s instanceof KubernetesCluster && ((KubernetesCluster) s).getFormalStatus().isConnected());
        am.registerAction(GET_CREDENTIAL_ADMIN, new Action<>(GET_CREDENTIAL_ADMIN, getAdminCredentialView));

        final ActionView.Builder getUserCredentialView = new ActionView.Builder("Set as Current Cluster (User)")
                .title(s -> Optional.ofNullable(s).map(r -> description("kubernetes.get_credential_user.kubernetes", ((KubernetesCluster) r).getName())).orElse(null))
                .enabled(s -> s instanceof KubernetesCluster && ((KubernetesCluster) s).getFormalStatus().isConnected());
        am.registerAction(GET_CREDENTIAL_USER, new Action<>(GET_CREDENTIAL_USER, getUserCredentialView));
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                "---",
                ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup registryActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
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
                ContainerServiceActionsContributor.GET_CREDENTIAL_USER
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

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
