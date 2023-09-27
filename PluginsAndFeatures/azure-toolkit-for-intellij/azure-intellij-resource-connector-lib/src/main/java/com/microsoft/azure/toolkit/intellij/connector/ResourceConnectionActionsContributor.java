/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.google.common.util.concurrent.SettableFuture;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.DeploymentTargetManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceConnectionActionsContributor implements IActionsContributor {
    public static final Action.Id<Object> REFRESH_CONNECTIONS = Action.Id.of("user/connector.refresh_connections");
    public static final Action.Id<AzureModule> ADD_CONNECTION = Action.Id.of("boundary/connector.add_connection");
    public static final Action.Id<Connection<?, ?>> EDIT_CONNECTION = Action.Id.of("user/connector.edit_connection");
    public static final Action.Id<Connection<?, ?>> REMOVE_CONNECTION = Action.Id.of("boundary/connector.remove_connection");
    public static final Action.Id<Connection<?, ?>> FIX_CONNECTION = Action.Id.of("user/connector.fix_connection");

    public static final Action.Id<Object> CONNECT_TO_MODULE = Action.Id.of("user/connector.connect_to_module");
    public static final Action.Id<AzureModule> REFRESH_MODULE = Action.Id.of("user/connector.refresh_module");
    public static final Action.Id<ConnectionManager> REFRESH_MODULE_CONNECTIONS = Action.Id.of("user/connector.refresh_module_connections");
    public static final Action.Id<DeploymentTargetManager> REFRESH_MODULE_TARGETS = Action.Id.of("user/connector.refresh_module_targets");
    public static final Action.Id<Connection<?, ?>> REFRESH_ENVIRONMENT_VARIABLES = Action.Id.of("user/connector.refresh_environment_variables");
    public static final Action.Id<AzureModule> HIDE_AZURE = Action.Id.of("user/connector.hide_azure_root");

    public static final Action.Id<Pair<String, String>> COPY_ENV_PAIR = Action.Id.of("user/connector.copy_env_pair");
    public static final Action.Id<Pair<String, String>> COPY_ENV_KEY = Action.Id.of("user/connector.copy_env_key");
    public static final Action.Id<Connection<?, ?>> COPY_ENV_VARS = Action.Id.of("user/connector.copy_env_variables");
    public static final Action.Id<Pair<String, String>> EDIT_ENV_IN_EDITOR = Action.Id.of("user/connector.edit_env_in_editor");
    public static final Action.Id<Connection<?, ?>> EDIT_ENV_FILE_IN_EDITOR = Action.Id.of("user/connector.edit_env_file_in_editor");

    public static final String MODULE_ACTIONS = "actions.connector.module";
    public static final String CONNECTION_ACTIONS = "actions.connector.connection";
    public static final String EXPLORER_MODULE_ROOT_ACTIONS = "actions.connector.explorer_module_root";
    public static final String EXPLORER_MODULE_LOCAL_CONNECTIONS_ACTIONS = "actions.connector.explorer_local_connections";

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(REFRESH_MODULE)
            .withLabel("Refresh")
            .withIcon(AzureIcons.Action.REFRESH.getIconPath())
            .withHandler((module, e) -> {
                Optional.ofNullable(module.getDefaultProfile()).ifPresent(Profile::reload);
                AzureEventBus.emit("connector.refreshed.module_root", module);
            })
            .withShortcut(am.getIDEDefaultShortcuts().refresh())
            .withAuthRequired(false)
            .register(am);

        new Action<>(REFRESH_MODULE_CONNECTIONS)
            .withLabel("Refresh")
            .withIcon(AzureIcons.Action.REFRESH.getIconPath())
            .withHandler((connectionManager, e) -> {
                connectionManager.reload();
                AzureEventBus.emit("connector.module_connections_changed", connectionManager);
            })
            .withShortcut(am.getIDEDefaultShortcuts().refresh())
            .withAuthRequired(false)
            .register(am);

        new Action<>(REFRESH_MODULE_TARGETS)
            .withLabel("Refresh")
            .withIcon(AzureIcons.Action.REFRESH.getIconPath())
            .withHandler((targetManager, e) -> {
                targetManager.reload();
                AzureEventBus.emit("connector.module_targets_changed", targetManager);
            })
            .withShortcut(am.getIDEDefaultShortcuts().refresh())
            .withAuthRequired(false)
            .register(am);

        new Action<>(REFRESH_ENVIRONMENT_VARIABLES)
            .withLabel("Refresh")
            .withIcon(AzureIcons.Action.REFRESH.getIconPath())
            .withHandler((targetManager, e) -> AzureEventBus.emit("connector.connection_environment_variables_changed", targetManager))
            .withShortcut(am.getIDEDefaultShortcuts().refresh())
            .withAuthRequired(false)
            .register(am);

        new Action<>(HIDE_AZURE)
            .withLabel("Hide 'Azure' Node")
            .withIcon(AzureIcons.Common.HIDE.getIconPath())
            .withHandler((module, e) -> {
                if (module.getProject().isDisposed()) {
                    return;
                }
                final PropertiesComponent properties = PropertiesComponent.getInstance(module.getProject());
                properties.setValue(module.getModule().getName() + ".azure", "hide");
                Optional.of(module.getProject())
                    .map(ProjectView::getInstance)
                    .map(ProjectView::getCurrentProjectViewPane)
                    .ifPresent(p -> p.updateFromRoot(true));
            })
            .withAuthRequired(false)
            .register(am);

        new Action<>(CONNECT_TO_MODULE)
            .withLabel("Connect Azure Resource...")
            .withIcon(AzureIcons.Connector.CONNECT.getIconPath())
            .withHandler((target, e) -> {
                if (target instanceof AzureModule) {
                    AzureModule module = (AzureModule) target;
                    AzureTaskManager.getInstance().runLater(() -> ModuleConnectorAction.connectModuleToAzureResource(module.getModule()));
                } else if (target instanceof ConnectionManager) {
                    ConnectionManager cm = (ConnectionManager) target;
                    AzureTaskManager.getInstance().runLater(() -> ModuleConnectorAction.connectModuleToAzureResource(cm.getProfile().getModule().getModule()));
                }
            })
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

        new Action<>(FIX_CONNECTION)
                .withLabel("Edit Connection...")
                .withIcon(AzureIcons.Action.EDIT.getIconPath())
                .visibleWhen(m -> m instanceof Connection<?, ?>)
                .withHandler((c, e) -> fixResourceConnection(c, ((AnActionEvent) e).getProject()))
                .withAuthRequired(false)
                .register(am);

        new Action<>(REMOVE_CONNECTION)
            .withLabel("Remove Resource Connection")
            .withIcon(AzureIcons.Action.REMOVE.getIconPath())
            .visibleWhen(m -> m instanceof Connection<?, ?>)
            .withHandler((c, e) -> ResourceConnectionActionsContributor.removeConnection(c, (AnActionEvent) e))
            .withShortcut(am.getIDEDefaultShortcuts().delete())
            .withAuthRequired(false)
            .register(am);

        new Action<>(COPY_ENV_KEY)
            .withIcon(AzureIcons.Action.COPY.getIconPath())
            .withLabel("Copy Key")
            .withHandler(s -> {
                am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(s.getKey());
                AzureMessager.getMessager().success(AzureString.format("Environment variable key is copied into clipboard."));
            })
            .withAuthRequired(false)
            .register(am);
        new Action<>(COPY_ENV_PAIR)
            .withIcon(AzureIcons.Action.COPY.getIconPath())
            .withLabel("Copy")
            .withHandler(pair -> {
                am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(String.format("%s=%s", pair.getKey(), pair.getValue()));
                AzureMessager.getMessager().success(AzureString.format("Environment variable key/value pair is copied into clipboard."));
            })
            .withShortcut(am.getIDEDefaultShortcuts().copy())
            .withAuthRequired(false)
            .register(am);
        new Action<>(COPY_ENV_VARS)
            .withIcon(AzureIcons.Action.COPY.getIconPath())
            .withLabel("Copy All")
            .withHandler(c -> {
                final List<Pair<String, String>> variables = c.getGeneratedEnvironmentVariables();
                final String str = variables.stream().map(v -> String.format("%s=%s", v.getKey(), v.getValue())).collect(Collectors.joining(System.lineSeparator()));
                am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(str);
                AzureMessager.getMessager().success(AzureString.format("Environment variables are copied into clipboard."));
            })
            .withShortcut(am.getIDEDefaultShortcuts().copy())
            .withAuthRequired(false)
            .register(am);
    }

    public static Connection<?, ?> fixResourceConnection(Connection<?, ?> c, Project project) {
        final SettableFuture<Connection<?, ?>> result = SettableFuture.create();
        AzureTaskManager.getInstance().runLater(() -> {
            final String invalidResourceName = c.getResource().isValidResource() ? null : c.getResource().getDefinition().getTitle();
            final String invalidConsumerName = c.getConsumer().isValidResource() ? null : c.getConsumer().getDefinition().getTitle();
            final String invalidProperties = Stream.of(invalidResourceName, invalidConsumerName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
            final String promptMessage = String.format("Please set correct %s for connection %s", invalidProperties, c.getEnvPrefix());
            final ConnectorDialog dialog = new ConnectorDialog(project);
            dialog.setDescription(promptMessage);
            dialog.setFixedEnvPrefix(c.getEnvPrefix());
            dialog.setFixedConnectionDefinition(c.getDefinition());
            dialog.setValue(c);
            if (dialog.showAndGet()) {
                result.set(dialog.getValue());
            } else {
                result.set(null);
            }
        });
        try {
            return result.get();
        } catch (InterruptedException | ExecutionException | RuntimeException e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

    private static void removeConnection(Connection<?, ?> connection, AnActionEvent e) {
        final Project project = Objects.requireNonNull(e.getProject());
        final Module module = ModuleManager.getInstance(project).findModuleByName(connection.getConsumer().getName());
        final AzureTaskManager m = AzureTaskManager.getInstance();
        m.runLater(() -> Optional.ofNullable(module).map(AzureModule::from)
            .map(AzureModule::getDefaultProfile)
            .map(env -> env.removeConnection(connection))
            .ifPresent(p -> m.write(p::save)));
    }

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
            IntellijActionsContributor.ACTIONS_DEPLOY_TO_AZURE,
            CONNECT_TO_MODULE,
            "---",
            HIDE_AZURE,
            "RevealGroup"
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
