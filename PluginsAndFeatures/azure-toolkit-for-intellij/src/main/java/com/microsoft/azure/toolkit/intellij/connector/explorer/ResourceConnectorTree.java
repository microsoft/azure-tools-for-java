/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.explorer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.connector.AzureResource;
import com.microsoft.azure.toolkit.intellij.connector.AzureResourceConnectorBusNotifier;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.mysql.MySQLDatabaseResource;
import com.microsoft.azure.toolkit.intellij.connector.sql.SqlServerDatabaseResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.intellij.helpers.AzureIconLoader;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ResourceConnectorTree extends AzureTree {

    private final Project project;
    private static final Map<Class<? extends Resource>, Icon> resourceIconMap = new HashMap<>();

    static {
        resourceIconMap.put(MySQLDatabaseResource.class, AzureIconLoader.loadIcon(AzureIconSymbol.MySQL.MODULE));
        resourceIconMap.put(SqlServerDatabaseResource.class, AzureIconLoader.loadIcon(AzureIconSymbol.SqlServer.MODULE));
    }

    public ResourceConnectorTree(Project project) {
        this.project = project;
        this.loadNodes();
        this.initModuleNodeLoadListener();
    }

    private void initModuleNodeLoadListener() {
        project.getMessageBus().connect().subscribe(AzureResourceConnectorBusNotifier.AZURE_RESOURCE_CONNECTOR_TOPIC, connection -> {
            Resource consumer = connection.getConsumer();
            if (!(consumer instanceof ModuleResource)) {
                return;
            }
            Enumeration<TreeNode> e = ResourceConnectorTree.this.getRootNode().children();
            while (e.hasMoreElements()) {
                TreeNode node = e.nextElement();
                if (node instanceof ModuleNode) {
                    ModuleNode moduleNode = (ModuleNode) node;
                    if (StringUtils.equals(consumer.getId(), moduleNode.getData().getModuleName())) {
                        moduleNode.loadResourceNodes();
                        getModel().reload(moduleNode);
                    }
                }
            }
        });
    }

    @Override
    public void loadNodes() {
        DefaultMutableTreeNode rootNode = getRootNode();
        rootNode.removeAllChildren();
        Module[] modules = ModuleManager.getInstance(project).getModules().clone();
        Arrays.sort(modules, Comparator.comparing(Module::getName));
        for (Module module : modules) {
            ModuleNode moduleNode = new ModuleNode(new ModuleResource(module.getName()));
            getModel().insertNodeInto(moduleNode, rootNode, rootNode.getChildCount());
            moduleNode.loadResourceNodes();
        }
        getModel().reload();
    }

    public class ModuleNode extends AzureNode<ModuleResource> {

        private ModuleNode(@Nonnull ModuleResource module) {
            super(module, ModuleResource::getModuleName, AllIcons.Actions.ModuleDirectory);
            getActionGroup().add(new AddAction());
        }

        private void loadResourceNodes() {
            this.removeAllChildren();
            List<Connection<? extends Resource, ? extends Resource>> moduleConnections = ResourceConnectorTree.this.project
                .getService(ConnectionManager.class).getConnectionsByConsumerId(getData().getModuleName());
            for (Connection<? extends Resource, ? extends Resource> connection : moduleConnections) {
                Optional.ofNullable(connection.getResource()).filter(resource -> resource instanceof AzureResource).ifPresent(resource ->
                    getModel().insertNodeInto(new ResourceNode((AzureResource) resource, resourceIconMap.get(resource.getClass())),
                            this, this.getChildCount()));
            }
        }

        private class AddAction extends AnAction {

            AddAction() {
                super("Add", "Add new Azure resource connector", AllIcons.General.Add);
            }

            @Override
            @AzureOperation(name = "connector|explorer.add_connection", type = AzureOperation.Type.ACTION)
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                final ConnectorDialog<? extends Resource, ModuleResource> dialog = new ConnectorDialog<>(project);
                dialog.setConsumer(ModuleNode.this.getData());
                dialog.show();
            }
        }
    }

    public class ResourceNode extends AzureNode<AzureResource> {

        public ResourceNode(@Nonnull AzureResource resource, Icon icon) {
            super(resource, icon);
            getActionGroup().add(new EditAction());
            getActionGroup().add(new ShowPropertiesAction());
        }

        private class EditAction extends AnAction {

            private EditAction() {
                super("Edit", "Edit desc", AllIcons.Actions.Edit);
            }

            @Override
            @AzureOperation(name = "connector|explorer.edit_connection", type = AzureOperation.Type.ACTION)
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                final ConnectorDialog<Resource, ModuleResource> dialog = new ConnectorDialog<>(project);
                dialog.setConsumer(new ModuleResource(((ModuleNode) ResourceNode.this.getParent()).getData().getModuleName()));
                dialog.setResource(ResourceNode.this.getData());
                dialog.show();
            }
        }

        private class ShowPropertiesAction extends AnAction {

            private ShowPropertiesAction() {
                super("Show Properties", "Show properties desc", AzureIconLoader.loadIcon(AzureIconSymbol.Common.SHOW_PROPERTIES));
            }

            @Override
            @AzureOperation(name = "connector|explorer.show_resource_properties", type = AzureOperation.Type.ACTION)
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                Optional.ofNullable(ResourceNode.this.getData()).ifPresent(resource -> {
                    if (MySQLDatabaseResource.Definition.AZURE_MYSQL.getType().equals(resource.getType())) {
                        DefaultLoader.getUIHelper().openMySQLPropertyView(resource.getServerId().id(), anActionEvent.getProject());
                    } else if (SqlServerDatabaseResource.Definition.SQL_SERVER.getType().equals(resource.getType())) {
                        DefaultLoader.getUIHelper().openSqlServerPropertyView(resource.getServerId().id(), anActionEvent.getProject());
                    }
                });
            }
        }
    }

}
