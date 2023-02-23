/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.messages.MessageBusConnection;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.component.Tree;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics.CONNECTIONS_REFRESHED;
import static com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics.CONNECTION_CHANGED;

public class ResourceConnectionExplorer extends Tree {

    private final Project project;

    public ResourceConnectionExplorer(Project project) {
        super();
        this.project = project;
        this.root = buildRoot();
        this.init(this.root);
        this.setRootVisible(false);
    }

    private Node<Project> buildRoot() {
        final ConnectionManager cm = this.project.getService(ConnectionManager.class);
        return new RootNode(project).lazy(false)
            .view(new NodeView.Static("Resource Connections", AzureIcons.Common.AZURE.getIconPath()))
            .addChildren(project -> Arrays.asList(ModuleManager.getInstance(project).getModules().clone()), (m, n) -> new ModuleNode(m).lazy(false)
                .view(new NodeView.Static(m.getName(), "/icons/module"))
                .actions(ResourceConnectionActionsContributor.MODULE_ACTIONS)
                .addChildren(module -> cm.getConnectionsByConsumerId(module.getName()), (c, mn) -> new Node<>(c).lazy(false)
                    .view(new NodeView.Static(c.getResource().getName(), c.getResource().getDefinition().getIcon()))
                    .actions(ResourceConnectionActionsContributor.CONNECTION_ACTIONS)));
    }

    private static class ModuleNode extends Node<Module> {
        public ModuleNode(@Nonnull Module module) {
            super(module);
        }
    }

    private static class RootNode extends Node<Project> {
        public RootNode(@Nonnull Project project) {
            super(project);
            final MessageBusConnection connection = project.getMessageBus().connect();
            connection.subscribe(CONNECTIONS_REFRESHED, (ConnectionTopics.ConnectionsRefreshed) () -> RootNode.this.view().refreshChildren());
            connection.subscribe(CONNECTION_CHANGED, (ConnectionTopics.ConnectionChanged) (p, conn, action) -> this.view().refreshChildren());
        }
    }

    public static class ToolWindow extends SimpleToolWindowPanel {
        private final com.intellij.ui.treeStructure.Tree tree;

        public ToolWindow(final Project project) {
            super(true);
            this.tree = new ResourceConnectionExplorer(project);
            final ActionToolbarImpl actionToolbar = this.initToolbar();
            actionToolbar.setTargetComponent(this.tree);
            actionToolbar.setForceMinimumSize(true);
            this.setContent(this.tree);
            this.setToolbar(actionToolbar);
        }

        private ActionToolbarImpl initToolbar() {
            final DefaultActionGroup group = new DefaultActionGroup();
            final ActionManager am = ActionManager.getInstance();
            final CommonActionsManager manager = CommonActionsManager.getInstance();
            group.add(am.getAction(ResourceConnectionActionsContributor.REFRESH_CONNECTIONS.getId()));
            group.add(am.getAction(ResourceConnectionActionsContributor.ADD_CONNECTION.getId()));
            group.add(am.getAction(ResourceConnectionActionsContributor.REMOVE_CONNECTION.getId()));
            group.addSeparator();
            // expand and collapse
            final DefaultTreeExpander expander = new DefaultTreeExpander(this.tree);
            group.add(manager.createExpandAllAction(expander, this.tree));
            group.add(manager.createCollapseAllAction(expander, this.tree));
            return new ActionToolbarImpl(ActionPlaces.TOOLBAR, group, true);
        }
    }

    public static class ToolWindowFactory implements com.intellij.openapi.wm.ToolWindowFactory {
        public static final String ID = "Resource Connections";

        @Override
        public boolean shouldBeAvailable(@NotNull Project project) {
            final ConnectionManager cm = project.getService(ConnectionManager.class);
            return cm.getConnections().size() > 0;
        }

        @Override
        @ExceptionNotification
        @AzureOperation(name = "platform/connector.initialize_explorer")
        public void createToolWindowContent(final Project project, final com.intellij.openapi.wm.ToolWindow toolWindow) {
            final ToolWindow myToolWindow = new ToolWindow(project);
            final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            final Content content = contentFactory.createContent(myToolWindow, "", false);
            toolWindow.getContentManager().addContent(content);
        }
    }

    public static class ToolWindowOpener implements ConnectionTopics.ConnectionChanged {
        @Override
        @ExceptionNotification
        @AzureOperation(name = "user/connector.open_explorer")
        public void connectionChanged(Project project, Connection<?, ?> connection, ConnectionTopics.Action change) {
            final com.intellij.openapi.wm.ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowFactory.ID);
            assert toolWindow != null;
            toolWindow.setAvailable(true);
            AzureTaskManager.getInstance().runLater(() -> toolWindow.activate(null));
        }
    }
}
