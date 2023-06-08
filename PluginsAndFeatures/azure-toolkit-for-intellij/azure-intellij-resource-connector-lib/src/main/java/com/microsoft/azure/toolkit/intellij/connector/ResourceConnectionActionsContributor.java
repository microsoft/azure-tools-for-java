/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class ResourceConnectionActionsContributor implements IActionsContributor {
    public static final Action.Id<Object> REFRESH_CONNECTIONS = Action.Id.of("user/connector.refresh_connections");
    public static final Action.Id<AzureModule> ADD_CONNECTION = Action.Id.of("user/connector.add_connection");
    public static final Action.Id<Connection<?, ?>> EDIT_CONNECTION = Action.Id.of("user/connector.edit_connection");
    public static final Action.Id<Connection<?, ?>> REMOVE_CONNECTION = Action.Id.of("user/connector.remove_connection");

    public static final Action.Id<AzureModule> CONNECT_TO_MODULE = Action.Id.of("user/connector.connect_to_module");
    public static final Action.Id<AzureModule> REFRESH_MODULE = Action.Id.of("user/connector.refresh_module");
    public static final Action.Id<AzureModule> REFRESH_MODULE_CONNECTIONS = Action.Id.of("user/connector.refresh_module_connections");
    public static final String MODULE_ACTIONS = "actions.connector.module";
    public static final String CONNECTION_ACTIONS = "actions.connector.connection";
    public static final String EXPLORER_MODULE_ROOT_ACTIONS = "actions.connector.explorer_module_root";
    public static final String EXPLORER_MODULE_LOCAL_CONNECTIONS_ACTIONS = "actions.connector.explorer_local_connections";

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(REFRESH_MODULE)
                .withLabel("Refresh")
                .withIcon(AzureIcons.Action.REFRESH.getIconPath())
                .withHandler((module, e) -> refreshModule(module))
                .withShortcut(am.getIDEDefaultShortcuts().refresh())
                .withAuthRequired(false)
                .register(am);

        new Action<>(REFRESH_MODULE_CONNECTIONS)
                .withLabel("Refresh")
                .withIcon(AzureIcons.Action.REFRESH.getIconPath())
                .withHandler((module, e) -> refreshModuleConnections(module))
                .withShortcut(am.getIDEDefaultShortcuts().refresh())
                .withAuthRequired(false)
                .register(am);

        new Action<>(CONNECT_TO_MODULE)
                .withLabel("Connect to Azure Resource")
                .withIcon(AzureIcons.Connector.CONNECT.getIconPath())
                .withHandler((module, e) -> AzureTaskManager.getInstance().runLater(() -> ModuleConnectorAction.connectModuleToAzureResource(module.getModule())))
                .withShortcut(am.getIDEDefaultShortcuts().refresh())
                .withAuthRequired(false)
                .register(am);

        new Action<>(REFRESH_CONNECTIONS)
            .withLabel("Refresh")
            .withIcon(AzureIcons.Action.REFRESH.getIconPath())
            .withHandler((project, e) -> refreshConnections((AnActionEvent) e))
            .withShortcut(am.getIDEDefaultShortcuts().refresh())
            .withAuthRequired(false)
            .register(am);

        new Action<>(ADD_CONNECTION)
            .withLabel("Add")
            .withIcon(AzureIcons.Action.ADD.getIconPath())
            .visibleWhen(m -> m instanceof AzureModule)
            .withHandler((m) -> openDialog(null, new ModuleResource(m.getName()), m.getProject()))
            .withShortcut(am.getIDEDefaultShortcuts().add())
            .withAuthRequired(false)
            .register(am);

        new Action<>(EDIT_CONNECTION)
            .withLabel("Edit")
            .withIcon(AzureIcons.Action.EDIT.getIconPath())
            .visibleWhen(m -> m instanceof Connection<?, ?>)
            .withHandler((c, e) -> openDialog(c, ((AnActionEvent) e).getProject()))
            .withShortcut(am.getIDEDefaultShortcuts().edit())
            .withAuthRequired(false)
            .register(am);

        new Action<>(REMOVE_CONNECTION)
            .withLabel("Remove")
            .withIcon(AzureIcons.Action.REMOVE.getIconPath())
            .visibleWhen(m -> m instanceof Connection<?, ?>)
            .withHandler((c, e) -> ResourceConnectionActionsContributor.removeConnection(c, (AnActionEvent) e))
            .withShortcut(am.getIDEDefaultShortcuts().delete())
            .withAuthRequired(false)
            .register(am);
    }

    private void refreshModuleConnections(AzureModule module) {
//        AzureEventBus.emit("");
        AzureEventBus.emit("connector.refreshed.module_connections", module);

    }

    private void refreshModule(AzureModule module) {
//        AzureEventBus.emit("");
        AzureEventBus.emit("connector.refreshed.module_root", module);
    }

    @AzureOperation(value = "user/connector.remove_connection.resource", params = "connection.getResource()")
    private static void removeConnection(Connection<?, ?> connection, AnActionEvent e) {
        final Project project = Objects.requireNonNull(e.getProject());
        final Module module = ModuleManager.getInstance(project).findModuleByName(connection.getConsumer().getName());
        AzureTaskManager.getInstance().runLater(() -> Optional.ofNullable(module).map(AzureModule::from)
            .map(AzureModule::getDefaultProfile)
            .map(env -> env.removeConnection(connection))
            .ifPresent(Profile::save));
    }

    @AzureOperation("user/connector.refresh_connections")
    private static void refreshConnections(AnActionEvent e) {
        Objects.requireNonNull(e.getProject())
            .getMessageBus().syncPublisher(ConnectionTopics.CONNECTIONS_REFRESHED)
            .connectionsRefreshed();
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup explorerModuleRootActions = new ActionGroup(
                REFRESH_MODULE,
                "---",
                CONNECT_TO_MODULE
        );
        am.registerGroup(EXPLORER_MODULE_ROOT_ACTIONS, explorerModuleRootActions);

        final ActionGroup explorerLocalConnectionsActions = new ActionGroup(
                REFRESH_MODULE_CONNECTIONS,
                "---",
                CONNECT_TO_MODULE
        );
        am.registerGroup(EXPLORER_MODULE_LOCAL_CONNECTIONS_ACTIONS, explorerModuleRootActions);

        final ActionGroup moduleActions = new ActionGroup(
            ADD_CONNECTION
        );
        am.registerGroup(MODULE_ACTIONS, moduleActions);

        final ActionGroup connectionActions = new ActionGroup("",
            EDIT_CONNECTION,
            REMOVE_CONNECTION
        );
        am.registerGroup(CONNECTION_ACTIONS, connectionActions);
    }

    @Override
    public int getOrder() {
        return ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    }

    private void openDialog(@Nullable Resource<?> r, @Nullable Resource<?> c, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final ConnectorDialog dialog = new ConnectorDialog(project);
            dialog.setConsumer(c);
            dialog.setResource(r);
            dialog.show();
        });
    }

    private void openDialog(Connection<?, ?> c, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final ConnectorDialog dialog = new ConnectorDialog(project);
            dialog.setValue(c);
            dialog.show();
        });
    }
}
