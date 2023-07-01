/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor.CONNECT_TO_MODULE;
import static com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor.REFRESH_MODULE_CONNECTIONS;

@Slf4j
public class ConnectionsNode extends AbstractAzureFacetNode<ConnectionManager> {

    private final Action<?> editAction;

    public ConnectionsNode(@Nonnull final Project project, @Nonnull ConnectionManager manager) {
        super(project, manager);
        this.editAction = new Action<>(Action.Id.of("user/connector.edit_connections_in_editor"))
            .withLabel("Open In Editor")
            .withIcon(AzureIcons.Action.EDIT.getIconPath())
            .withHandler(ignore -> AzureTaskManager.getInstance().runLater(() -> this.navigate(true)))
            .withAuthRequired(false);
    }

    @Override
    @Nonnull
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        final ArrayList<AbstractTreeNode<?>> nodes = new ArrayList<>();
        if (this.isDisposed()) {
            return nodes;
        }
        try {
            final List<ConnectionNode> children = Optional.ofNullable(this.getValue()).stream()
                .flatMap(p -> p.getConnections().stream())
                .map(r -> new ConnectionNode(this.getProject(), r))
                .toList();
            if (CollectionUtils.isNotEmpty(children)) {
                return children;
            }
            nodes.add(new ActionNode<>(this.getProject(), CONNECT_TO_MODULE, this.getValue().getProfile().getModule()));
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
            nodes.add(toExceptionNode(e));
        }
        return nodes;
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        try {
            final List<Connection<?, ?>> connections = Optional.ofNullable(getValue())
                .map(ConnectionManager::getConnections).orElse(Collections.emptyList());
            final boolean isConnectionValid = connections.stream().allMatch(Connection::isValidConnection);
            presentation.addText("Resource connections", AzureFacetRootNode.getTextAttributes(isConnectionValid));
            presentation.setIcon(AllIcons.Nodes.HomeFolder);
            presentation.setTooltip("The dependent/connected resources.");
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    @Nullable
    @Override
    public IActionGroup getActionGroup() {
        return new ActionGroup(
            REFRESH_MODULE_CONNECTIONS,
            "---",
            editAction,
            CONNECT_TO_MODULE
        );
    }

    @Override
    public void navigate(boolean requestFocus) {
        Optional.ofNullable(getConnectionsFile())
            .map(f -> PsiManager.getInstance(getProject()).findFile(f))
            .map(f -> NavigationUtil.openFileWithPsiElement(f, requestFocus, requestFocus));
    }

    @Override
    public boolean canNavigateToSource() {
        return Objects.nonNull(getConnectionsFile());
    }

    @Nullable
    private VirtualFile getConnectionsFile() {
        return Optional.ofNullable(getValue())
            .map(ConnectionManager::getConnectionsFile)
            .orElse(null);
    }

    @Override
    public String toString() {
        return "Resource Connections";
    }
}