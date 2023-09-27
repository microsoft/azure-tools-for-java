/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.dbtools;

import com.intellij.database.dataSource.DataSourceConfigurable;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.url.DataInterchange;
import com.intellij.database.dataSource.url.FieldSize;
import com.intellij.database.dataSource.url.template.UrlEditorModel;
import com.intellij.database.dataSource.url.ui.ParamEditorBase;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.database.mysql.creation.CreateMySqlAction;
import com.microsoft.azure.toolkit.intellij.database.postgre.creation.CreatePostgreSqlAction;
import com.microsoft.azure.toolkit.intellij.database.sqlserver.creation.CreateSqlServerAction;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySql;
import com.microsoft.azure.toolkit.lib.mysql.MySqlServer;
import com.microsoft.azure.toolkit.lib.postgre.AzurePostgreSql;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.MicrosoftSqlServer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DatabaseServerParamEditor extends ParamEditorBase<DatabaseServerParamEditor.SqlDbServerComboBox> {
    public static final String KEY_DB_SERVER_ID = "AZURE_SQL_DB_SERVER";
    public static final String NONE = "<NONE>";
    public static final String NO_SERVERS_TIPS = "<html>No existing %s servers in Azure. You can <a href=''>create one</a> first.</html>";
    public static final String NOT_SIGNIN_TIPS = "<html><a href=\"\">Sign in</a> to select an existing %s server in Azure.</html>";
    private final Class<? extends IDatabaseServer<?>> clazz;
    @Getter
    @Setter
    private String text = "";
    private boolean updating;

    public DatabaseServerParamEditor(@Nonnull Class<? extends IDatabaseServer<?>> clazz, @Nonnull String label, @Nonnull DataInterchange interchange) {
        super(new SqlDbServerComboBox(clazz), interchange, FieldSize.LARGE, label);
        this.clazz = clazz;
        final LocalDataSource dataSource = getDataSourceConfigurable().getDataSource();
        final SqlDbServerComboBox combox = this.getEditorComponent();
        combox.addValueChangedListener(this::setServer);
        interchange.addPersistentProperty(KEY_DB_SERVER_ID);
        final String serverId = interchange.getProperty(KEY_DB_SERVER_ID);
        final boolean isModifying = StringUtils.isNotBlank(dataSource.getUsername());
        if (isModifying && Objects.nonNull(serverId)) {
            combox.setValue(new AzureComboBox.ItemReference<>(i -> i.getId().equals(serverId)));
        } else if (Objects.isNull(serverId)) {
            combox.setValue((IDatabaseServer<?>) null);
        }

        interchange.addPropertyChangeListener((evt -> onPropertiesChanged(evt.getPropertyName(), evt.getNewValue())), this);
    }

    @Override
    protected @Nonnull JComponent createComponent(SqlDbServerComboBox combox) {
        final JPanel container = new JPanel();
        final BoxLayout layout = new BoxLayout(container, BoxLayout.Y_AXIS);
        container.setLayout(layout);

        combox.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(combox);

        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            final HyperlinkLabel notSignInTips = initNotSignInTipsLabel(combox);
            container.add(notSignInTips);
        }

        final HyperlinkLabel noServersTips = initNoServerTipsLabel(combox);
        container.add(noServersTips);
        return container;
    }

    @AzureOperation(name = "user/$database.signin_from_dbtools")
    private void signInAndReloadItems(SqlDbServerComboBox combox, HyperlinkLabel notSignInTips) {
        OperationContext.current().setTelemetryProperty("serviceName", getServiceName());
        AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH).handle((a) -> {
            notSignInTips.setVisible(false);
            combox.reloadItems();
        });
    }

    @Nonnull
    private HyperlinkLabel initNotSignInTipsLabel(SqlDbServerComboBox combox) {
        final HyperlinkLabel label = new HyperlinkLabel();
        label.setHtmlText(String.format(NOT_SIGNIN_TIPS, getName(combox.getClazz())));
        label.setIcon(AllIcons.General.Information);
        label.addHyperlinkListener(e -> signInAndReloadItems(combox, label));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private HyperlinkLabel initNoServerTipsLabel(SqlDbServerComboBox combox) {
        final HyperlinkLabel label = new HyperlinkLabel();
        label.setHtmlText(String.format(NO_SERVERS_TIPS, getName(combox.getClazz())));
        label.setIcon(AllIcons.General.Information);
        label.addHyperlinkListener(e -> createServerInIde(e.getInputEvent()));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        combox.setServerListener(label::setVisible);
        return label;
    }

    @AzureOperation(name = "user/$database.create_server_from_dbtools")
    private void createServerInIde(InputEvent e) {
        OperationContext.current().setTelemetryProperty("serviceName", getServiceName());
        final DataContext context = DataManager.getInstance().getDataContext(e.getComponent());
        final Project project = context.getData(CommonDataKeys.PROJECT);
        final Window window = ComponentUtil.getActiveWindow();
        window.setVisible(false);
        window.dispose();
        final ToolWindow explorer = ToolWindowManager.getInstance(Objects.requireNonNull(project)).getToolWindow("Azure Explorer");
        Objects.requireNonNull(explorer).activate(() -> {
            final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), e, "database.dbtools", context);
            if (MySqlServer.class.isAssignableFrom(clazz)) {
                AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.HIGHLIGHT_RESOURCE_IN_EXPLORER).handle(Azure.az(AzureMySql.class), event);
                CreateMySqlAction.create(project, null);
            } else if (PostgreSqlServer.class.isAssignableFrom(clazz)) {
                AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.HIGHLIGHT_RESOURCE_IN_EXPLORER).handle(Azure.az(AzurePostgreSql.class), event);
                CreatePostgreSqlAction.create(project, null);
            } else if (MicrosoftSqlServer.class.isAssignableFrom(clazz)) {
                AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.HIGHLIGHT_RESOURCE_IN_EXPLORER).handle(Azure.az(AzureSqlServer.class), event);
                CreateSqlServerAction.create(project, null);
            }
        });
    }

    private void onPropertiesChanged(String propertyName, Object newValue) {
        if (!this.updating && StringUtils.isNotEmpty((String) newValue) && StringUtils.equals(propertyName, "host")) {
            final SqlDbServerComboBox combox = this.getEditorComponent();
            final IDatabaseServer<?> server = combox.getValue();
            if (Objects.nonNull(server) && !Objects.equals(server.getJdbcUrl().getServerHost(), newValue)) {
                combox.setValue((IDatabaseServer<?>) null);
                this.setServer(null);
            }
        }
    }

    @AzureOperation(name = "user/$database.select_server_dbtools.server", params = {"server.getName()"}, source = "server")
    private void setServer(@Nullable IDatabaseServer<?> server) {
        final DataInterchange interchange = this.getInterchange();
        final String oldServerId = interchange.getProperty(KEY_DB_SERVER_ID);
        final String newServerId = Optional.ofNullable(server).map(IDatabaseServer::getId).orElse(null);
        this.updating = true;
        AzureTaskManager.getInstance().runLater(() -> {
            // null will not be saved at com/intellij/database/dataSource/url/ui/DynamicJdbcUrlEditor#storeProperties
            interchange.putProperty(KEY_DB_SERVER_ID, Optional.ofNullable(newServerId).orElse(NONE));
            if (Objects.isNull(server) || Objects.equals(oldServerId, newServerId)) {
                this.updating = false;
                return;
            }
            final String user = server.getFullAdminName();
            interchange.getDataSource().setUsername(user);
            this.setUsername(user);
            this.setJdbcUrl(server.getJdbcUrl().toString());
            this.updating = false;
        }, AzureTask.Modality.ANY);
    }

    private void setUsername(String user) {
        final UrlEditorModel model = this.getDataSourceConfigurable().getUrlEditor().getEditorModel();
        model.setParameter("user", user);
        model.commit(true);
    }

    private void setJdbcUrl(String url) {
        final UrlEditorModel model = this.getDataSourceConfigurable().getUrlEditor().getEditorModel();
        model.setUrl(url);
        model.commit(true);
    }

    @SneakyThrows
    private DataSourceConfigurable getDataSourceConfigurable() {
        return (DataSourceConfigurable) FieldUtils.readField(this.getInterchange(), "myConfigurable", true);
    }

    private String getName(Class<? extends IDatabaseServer<?>> clazz) {
        if (MySqlServer.class.isAssignableFrom(clazz)) {
            return "MySQL";
        } else if (PostgreSqlServer.class.isAssignableFrom(clazz)) {
            return "PostgreSQL";
        } else if (MicrosoftSqlServer.class.isAssignableFrom(clazz)) {
            return "Microsoft SQL";
        }
        return "Database";
    }

    @Getter
    @RequiredArgsConstructor
    static class SqlDbServerComboBox extends AzureComboBox<IDatabaseServer<?>> {
        private final Class<? extends IDatabaseServer<?>> clazz;
        private boolean noServers;
        private Consumer<Boolean> serverListener;

        @Nullable
        @Override
        protected IDatabaseServer<?> doGetDefaultValue() {
            return CacheManager.getUsageHistory(clazz).peek();
        }

        @Nonnull
        @Override
        protected List<IDatabaseServer<?>> loadItems() {
            if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                return Collections.emptyList();
            }
            List<IDatabaseServer<?>> servers = new ArrayList<>();
            if (MySqlServer.class.isAssignableFrom(clazz)) {
                servers = Azure.az(AzureMySql.class).servers().stream()
                    .filter(s -> s.getFormalStatus().isRunning()).collect(Collectors.toList());
            } else if (MicrosoftSqlServer.class.isAssignableFrom(clazz)) {
                servers = Azure.az(AzureSqlServer.class).servers().stream()
                    .filter(s -> s.getFormalStatus().isRunning()).collect(Collectors.toList());
            } else if (PostgreSqlServer.class.isAssignableFrom(clazz)) {
                servers = Azure.az(AzurePostgreSql.class).servers().stream()
                    .filter(s -> s.getFormalStatus().isRunning()).collect(Collectors.toList());
            }
            this.noServers = CollectionUtils.isEmpty(servers);
            Optional.ofNullable(this.serverListener).ifPresent(l -> l.accept(this.noServers));
            return servers;
        }

        public void setServerListener(@Nonnull Consumer<Boolean> serverListener) {
            this.serverListener = serverListener;
            serverListener.accept(this.noServers);
        }

        @Override
        protected String getItemText(Object item) {
            return Optional.ofNullable(item).map(i -> ((IDatabaseServer<?>) i)).map(AzResource::getName).orElse("");
        }
    }

    @Nullable
    private String getServiceName() {
        if (MySqlServer.class.isAssignableFrom(clazz)) {
            return "mysql";
        } else if (PostgreSqlServer.class.isAssignableFrom(clazz)) {
            return "postgre";
        } else if (MicrosoftSqlServer.class.isAssignableFrom(clazz)) {
            return "sqlserver";
        }
        return null;
    }
}
