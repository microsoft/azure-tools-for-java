/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.containerapps;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.Revision;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider.DEFAULT_AZURE_RESOURCE_ICON_PROVIDER;
import static com.microsoft.azure.toolkit.lib.Azure.az;

public class ContainerAppsNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Container Apps";
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
            final AzureContainerApps service = ((AzureContainerApps) data);
            final Function<AzureContainerApps, List<ContainerAppsEnvironment>> registries = asc -> asc.list().stream()
                    .flatMap(m -> m.environments().list().stream())
                    .collect(Collectors.toList());
            return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                    .actions(ContainerAppsActionsContributor.SERVICE_ACTIONS)
                    .addChildren(registries, (server, serviceNode) -> this.createNode(server, serviceNode, manager));
        } else if (data instanceof ContainerAppsEnvironment) {
            final ContainerAppsEnvironment environment = (ContainerAppsEnvironment) data;
            return new Node<>(environment)
                    .view(new AzureResourceLabelView<>(environment))
                    .inlineAction(ResourceCommonActionsContributor.PIN)
                    .addChildren(env -> env.listContainerApps(), (app, envNode) -> this.createNode(app, envNode, manager))
                    .actions(ContainerAppsActionsContributor.ENVIRONMENT_ACTIONS);
        } else if (data instanceof ContainerApp) {
            final ContainerApp app = (ContainerApp) data;
            return new Node<>(app)
                    .view(new AzureResourceLabelView<>(app))
                    .inlineAction(ResourceCommonActionsContributor.PIN)
                    .addChildren(containerApp -> containerApp.revisions().list(), (revision, appNode) -> this.createNode(revision, appNode, manager))
                    .actions(ContainerAppsActionsContributor.CONTAINER_APP_ACTIONS);
        } else if (data instanceof Revision) {
            final Revision revision = (Revision) data;
            return new Node<>(revision)
                    .view(new AzureResourceLabelView<>(revision, this::getRevisionDescription, DEFAULT_AZURE_RESOURCE_ICON_PROVIDER))
                    .inlineAction(ResourceCommonActionsContributor.PIN)
                    .actions(ContainerAppsActionsContributor.REVISION_ACTIONS);
        }
        return null;
    }

    private String getRevisionDescription(@Nonnull final Revision revision) {
        final Revision latestRevision = revision.getParent().getLatestRevision();
        return String.format(Objects.equals(latestRevision, revision) ? "%s (Latest)" : "%s", revision.isActive() ? "Active" : "Inactive");
    }
}
