/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.containerapps;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzModuleNode;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.component.ServiceLinkerNode;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerAppsServiceSubscription;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.Revision;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.RevisionModule;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinker;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinkerModule;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider.DEFAULT_AZURE_RESOURCE_ICON_PROVIDER;
import static com.microsoft.azure.toolkit.lib.Azure.az;

public class ContainerAppsNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Container Apps Environment";
    private static final String ICON = AzureIcons.ContainerApps.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureContainerApps.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureContainerApps || data instanceof ContainerAppsEnvironment ||
            data instanceof ContainerApp || data instanceof Revision;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull IExplorerNodeProvider.Manager manager) {
        if (data instanceof AzureContainerApps) {
            final Function<AzureContainerApps, List<ContainerAppsEnvironment>> registries = asc -> asc.list().stream()
                .flatMap(m -> m.environments().list().stream())
                .collect(Collectors.toList());
            return new AzServiceNode<>((AzureContainerApps) data)
                .withIcon(ICON)
                .withLabel(NAME)
                .withActions(ContainerAppsActionsContributor.SERVICE_ACTIONS)
                .addChildren(registries, (server, serviceNode) -> this.createNode(server, serviceNode, manager));
        } else if (data instanceof ContainerAppsEnvironment) {
            return new ContainerAppsEnvironmentNode((ContainerAppsEnvironment) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .addChildren(env -> env.listContainerApps().stream()
                                       .filter(app -> StringUtils.isBlank(app.getManagedBy()) || Objects.nonNull(app.getManagedByResource()))
                                       .collect(Collectors.toList()),
                             (app, envNode) -> StringUtils.isBlank(app.getManagedBy()) ? this.createNode(app, envNode, manager) :
                                               manager.createNode(Objects.requireNonNull(app.getManagedByResource()), envNode, null))
                .withActions(ContainerAppsActionsContributor.ENVIRONMENT_ACTIONS);
        } else if (data instanceof ContainerApp) {
            return new AzResourceNode<>((ContainerApp) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .onDoubleClicked(ResourceCommonActionsContributor.SHOW_PROPERTIES)
                .addChildren(ContainerApp::getSubModules, (revision, appNode) -> this.createNode(revision, appNode, manager))
                .withActions(ContainerAppsActionsContributor.CONTAINER_APP_ACTIONS)
                .withMoreChildren(a -> a.revisions().hasMoreResources(), a -> a.revisions().loadMoreResources());
        } else if (data instanceof RevisionModule) {
            return new AzModuleNode<>((RevisionModule) data)
                .withIcon(AzureIcons.ContainerApps.REVISION_MODULE)
                .withLabel("Revisions")
                .withActions(ContainerAppsActionsContributor.REVISION_MODULE_ACTIONS)
                .addChildren(RevisionModule::list, (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof Revision) {
            return new AzResourceNode<>((Revision) data)
                .withIcon(DEFAULT_AZURE_RESOURCE_ICON_PROVIDER::getIcon)
                .withDescription(this::getRevisionDescription)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .withActions(ContainerAppsActionsContributor.REVISION_ACTIONS);
        } else if (data instanceof ServiceLinkerModule) {
            return new AzModuleNode<>((ServiceLinkerModule) data)
                .withIcon(AzureIcons.Connector.SERVICE_LINKER_MODULE)
                .withLabel("Service Connectors")
                .withActions(ResourceCommonActionsContributor.SERVICE_LINKER_MODULE_ACTIONS)
                .addChildren(ServiceLinkerModule::list, (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof ServiceLinker) {
            return new ServiceLinkerNode((ServiceLinker) data);
        }
        return null;
    }

    private String getRevisionDescription(@Nonnull final Revision revision) {
        final AzResource.FormalStatus formalStatus = revision.getFormalStatus();
        if (!formalStatus.isRunning() && !formalStatus.isStopped()) {
            return revision.getStatus();
        }
        final Revision latestRevision = revision.getParent().getLatestRevision();
        final boolean latest = Objects.equals(latestRevision, revision);
        return String.format(latest ? "%s (Latest)" : "%s", revision.isActive() ? "Active" : "Inactive");
    }

    static class ContainerAppsEnvironmentNode extends AzResourceNode<ContainerAppsEnvironment> {
        public ContainerAppsEnvironmentNode(@NotNull ContainerAppsEnvironment resource) {
            super(resource);
        }

        @Override
        public void onEvent(AzureEvent event) {
            final String type = event.getType();
            final Object source = event.getSource();
            if (source instanceof AzureContainerAppsServiceSubscription
                && StringUtils.equals(((AzureContainerAppsServiceSubscription) source).getSubscriptionId(), this.getValue().getSubscriptionId())
                && StringUtils.equals(type, "resource.children_changed.resource")) {
                this.refreshChildrenLater(true);
            }
            super.onEvent(event);
        }
    }
}
