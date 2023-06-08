/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.explorer.node;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.util.messages.MessageBusConnection;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics.CONNECTION_CHANGED;

public class LocalConnectionsNode extends AbstractTreeNode<AzureModule> implements IAzureProjectExplorerNode {
    private final AzureEventBus.EventListener listener;
    private final MessageBusConnection connection;

    public LocalConnectionsNode(final AzureModule module) {
        super(module.getProject(), module);
        this.listener = new AzureEventBus.EventListener(this::onEvent);
        this.connection = module.getProject().getMessageBus().connect();
        this.connection.subscribe(CONNECTION_CHANGED, (ConnectionTopics.ConnectionChanged) (p, conn, action) -> {
            if (conn.getConsumer().getId().equalsIgnoreCase(module.getName())) {
                refresh();
            }
        });
        AzureEventBus.on("connector.refreshed.module_connections", listener);
    }

    private void onEvent(AzureEvent azureEvent) {
        final Object payload = azureEvent.getSource();
        if (payload instanceof AzureModule && Objects.equals(payload, getValue())) {
            refresh();
        }
    }

    private void refresh() {
        final AbstractProjectViewPane currentProjectViewPane = ProjectView.getInstance(getProject()).getCurrentProjectViewPane();
        currentProjectViewPane.updateFromRoot(true);
    }

    @Override
    @Nonnull
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        final AzureModule module = this.getValue();
        final Profile profile = module.getDefaultProfile();
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            return Collections.emptyList();
        }
        return Optional.ofNullable(this.getValue()).stream()
                .map(AzureModule::getDefaultProfile).filter(Objects::nonNull)
                .flatMap(p -> p.getConnections().stream())
                .map(Connection::getResource)
                .map(Resource::getData)
                .filter(Objects::nonNull)
                .map(d -> AzureExplorer.getManager().createNode(d, null, IExplorerNodeProvider.ViewType.APP_CENTRIC))
                .map(r -> new ResourceNode(module.getProject(), r))
                .toList();
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        presentation.setPresentableText("Local Resource Connections");
        presentation.setTooltip("consumed by project locally");
        presentation.setIcon(AllIcons.Nodes.HomeFolder);
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        return StringUtils.equalsIgnoreCase(dataId, "ACTION_SOURCE") ? this.getValue() : null;
    }

    @javax.annotation.Nullable
    @Override
    public IActionGroup getActionGroup() {
        return AzureActionManager.getInstance().getGroup(ResourceConnectionActionsContributor.EXPLORER_MODULE_LOCAL_CONNECTIONS_ACTIONS);
    }
}
