/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.explorer;

import com.intellij.icons.AllIcons;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.treeStructure.Tree;
import com.microsoft.azure.toolkit.intellij.connector.AzureResourceConnectorBusNotifier;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

public class ConnectorToolWindow extends SimpleToolWindowPanel {
    private final Tree tree;

    private final Project project;

    public ConnectorToolWindow(@NotNull final Project project) {
        super(true);
        this.project = project;
        this.tree = new ResourceConnectorTree(project);
        new TreeSpeedSearch(tree);
        final ActionToolbarImpl actionToolbar = this.initToolbar();
        actionToolbar.setTargetComponent(this.tree);
        actionToolbar.setForceMinimumSize(true);
        this.setContent(this.tree);
        this.setToolbar(actionToolbar);
        this.initShowToolWindowListener(project);
    }

    private ActionToolbarImpl initToolbar() {
        final DefaultActionGroup group = new DefaultActionGroup();
        final CommonActionsManager manager = CommonActionsManager.getInstance();
        group.add(new RefreshAction());
        group.add(new AddAction());
        group.add(new RemoveAction());
        group.addSeparator();
        // expand and collapse
        final DefaultTreeExpander expander = new DefaultTreeExpander(this.tree);
        group.add(manager.createExpandAllAction(expander, this.tree));
        group.add(manager.createCollapseAllAction(expander, this.tree));
        return new ActionToolbarImpl(ActionPlaces.TOOLBAR, group, true);
    }

    private void initShowToolWindowListener(@NotNull final Project project) {
        project.getMessageBus().connect().subscribe(AzureResourceConnectorBusNotifier.AZURE_RESOURCE_CONNECTOR_TOPIC, connection -> {
            final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Azure Resource Connector");
            assert toolWindow != null;
            if (!toolWindow.isVisible()) {
                toolWindow.show();
            }
        });
    }

    private class RefreshAction extends com.intellij.ide.actions.RefreshAction {
        private boolean loading = false;

        RefreshAction() {
            super(IdeBundle.messagePointer("action.refresh"), IdeBundle.messagePointer("action.refresh"), AllIcons.Actions.Refresh);
        }

        @Override
        @AzureOperation(name = "connector|explorer.refresh_connection", type = AzureOperation.Type.ACTION)
        public final void actionPerformed(@NotNull final AnActionEvent e) {
            this.loading = true;
            ((AzureTree) ConnectorToolWindow.this.tree).loadNodes();
            this.loading = false;
        }

        @Override
        public final void update(@NotNull final AnActionEvent event) {
            final Presentation presentation = event.getPresentation();
            final Icon icon = loading ? new AnimatedIcon.Default() : this.getTemplatePresentation().getIcon();
            presentation.setIcon(icon);
            presentation.setEnabled(!loading);
        }
    }

    private class AddAction extends AnAction {

        AddAction() {
            super("Add", "Add new Azure resource connector", AllIcons.General.Add);
        }

        @Override
        @AzureOperation(name = "connector|explorer.add_connection", type = AzureOperation.Type.ACTION)
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            final AzureNode<?> selectedNode = ((AzureTree) ConnectorToolWindow.this.tree).getSelectedAzureNode();
            if (Objects.nonNull(selectedNode) && selectedNode instanceof ResourceConnectorTree.ModuleNode) {
                final ConnectorDialog dialog = new ConnectorDialog(project);
                dialog.setConsumer(((ResourceConnectorTree.ModuleNode) selectedNode).getData());
                dialog.show();
            }
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
            final Presentation presentation = event.getPresentation();
            final AzureNode<?> selectedNode = ((AzureTree) ConnectorToolWindow.this.tree).getSelectedAzureNode();
            presentation.setEnabled(Objects.nonNull(selectedNode) && selectedNode instanceof ResourceConnectorTree.ModuleNode);
        }
    }

    private class RemoveAction extends AnAction {

        RemoveAction() {
            super("Remove", "Remove Azure resource connector", AllIcons.General.Remove);
        }

        @Override
        @AzureOperation(name = "connector|explorer.remove_connection", type = AzureOperation.Type.ACTION)
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            Optional.ofNullable(((AzureTree) ConnectorToolWindow.this.tree).getSelectedAzureNode()).ifPresent(selectedNode -> {
                if (selectedNode instanceof ResourceConnectorTree.ResourceNode && selectedNode.getParent() instanceof ResourceConnectorTree.ModuleNode) {
                    // remove node from connector manager
                    final ResourceConnectorTree.ResourceNode resourceNode = (ResourceConnectorTree.ResourceNode) selectedNode;
                    final ResourceConnectorTree.ModuleNode consumerNode = (ResourceConnectorTree.ModuleNode) resourceNode.getParent();
                    project.getService(ConnectionManager.class).removeConnection(resourceNode.getData().getId(), consumerNode.getData().getModuleName());
                    // remove node from tree
                    ((AzureTree) ConnectorToolWindow.this.tree).getModel().removeNodeFromParent(selectedNode);
                }
            });
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
            final Presentation presentation = event.getPresentation();
            final AzureNode<?> selectedNode = ((AzureTree) ConnectorToolWindow.this.tree).getSelectedAzureNode();
            presentation.setEnabled(Objects.nonNull(selectedNode) && selectedNode instanceof ResourceConnectorTree.ResourceNode);
        }

    }

}
