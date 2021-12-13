/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.sqlserver.properties;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.AzureHideableTitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.BaseEditor;
import com.microsoft.azure.toolkit.intellij.database.DatabaseComboBox;
import com.microsoft.azure.toolkit.intellij.database.ui.ConnectionSecurityPanel;
import com.microsoft.azure.toolkit.intellij.database.ui.ConnectionStringsOutputPanel;
import com.microsoft.azure.toolkit.intellij.database.ui.MySQLPropertyActionPanel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.database.entity.FirewallRuleEntity;
import com.microsoft.azure.toolkit.lib.database.utils.DatabaseTemplateUtils;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.SqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlDatabaseEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlServerEntity;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.ui.base.MvpView;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.sqlserver.SqlServerModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.sqlserver.SqlServerProperty;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlServerPropertyView extends BaseEditor implements MvpView {

    public static final String ID = "com.microsoft.azure.toolkit.intellij.sqlserver.properties.SqlServerPropertyView";
    private static final String SQLSERVER_DRIVER_CLASS_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    private AzureHideableTitledSeparator overviewSeparator;
    private SqlServerPropertryOverviewPanel overview;
    private AzureHideableTitledSeparator connectionSecuritySeparator;
    private ConnectionSecurityPanel connectionSecurity;
    private AzureHideableTitledSeparator connectionStringsSeparator;
    private ConnectionStringsOutputPanel connectionStringsJDBC;
    private ConnectionStringsOutputPanel connectionStringsSpring;
    private JPanel rootPanel;
    private JPanel contextPanel;
    private JScrollPane scrollPane;
    private MySQLPropertyActionPanel propertyActionPanel;
    private DatabaseComboBox databaseComboBox;
    private JLabel databaseLabel;

    private final SqlServerProperty property = new SqlServerProperty();

    private Boolean originalAllowAccessToAzureServices;
    private Boolean originalAllowAccessToLocal;
    private final Project project;
    private final VirtualFile virtualFile;

    public SqlServerPropertyView(@Nonnull final Project project, @Nonnull final VirtualFile virtualFile) {
        super(virtualFile);
        this.project = project;
        this.virtualFile = virtualFile;
        overviewSeparator.addContentComponent(overview);
        connectionSecuritySeparator.addContentComponent(connectionSecurity);
        connectionStringsSeparator.addContentComponent(databaseLabel);
        connectionStringsSeparator.addContentComponent(databaseComboBox);
        connectionStringsSeparator.addContentComponent(connectionStringsJDBC);
        connectionStringsSeparator.addContentComponent(connectionStringsSpring);
        connectionStringsJDBC.getTitleLabel().setText("JDBC");
        connectionStringsSpring.getTitleLabel().setText("Spring");
        final JdbcUrl jdbcUrl = this.getJdbcUrl(null, null, null);
        connectionStringsJDBC.getOutputTextArea().setText(DatabaseTemplateUtils.toJdbcTemplate(jdbcUrl));
        connectionStringsSpring.getOutputTextArea().setText(DatabaseTemplateUtils.toSpringTemplate(jdbcUrl, SQLSERVER_DRIVER_CLASS_NAME));
        init();
        initListeners();

        AzureEventBus.after("sqlserver.delete_server.server", this::onMySqlServerStatusDeleted);
        AzureEventBus.before("sqlserver.delete_server.server", this::onMySqlServerStatusDeleting);
    }

    private void onMySqlServerStatusDeleted(SqlServer server) {
        if (StringUtils.equalsIgnoreCase(this.property.getServer().entity().getId(), server.entity().getId())) {
            AzureTaskManager.getInstance().runLater(() -> FileEditorManager.getInstance(this.project).closeFile(this.virtualFile));
        }
    }

    private void onMySqlServerStatusDeleting(SqlServer server) {
        if (StringUtils.equalsIgnoreCase(this.property.getServer().entity().getId(), server.entity().getId())) {
            AzureTaskManager.getInstance().runLater(() -> overview.getStatusTextField().setText("Deleting..."));
        }
    }

    private JdbcUrl getJdbcUrl(final String hostname, final String database, final String username) {
        final String realHostname = StringUtils.isNotBlank(hostname) ? hostname : "${your_hostname}";
        final String realDatabase = StringUtils.isNotBlank(database) ? database : "${your_database}";
        final String realUsername = StringUtils.isNotBlank(username) ? username : "${your_username}";
        return JdbcUrl.sqlserver(realHostname, realDatabase).setUsername(realUsername).setPassword("${your_password}");
    }

    private void init() {
        originalAllowAccessToAzureServices = connectionSecurity.getAllowAccessFromAzureServicesCheckBox().getModel().isSelected();
        originalAllowAccessToLocal = connectionSecurity.getAllowAccessFromLocalMachineCheckBox().getModel().isSelected();
    }

    private void initListeners() {
        // update to trigger save/discard buttons
        connectionSecurity.getAllowAccessFromAzureServicesCheckBox().addItemListener(this::onCheckBoxChanged);
        connectionSecurity.getAllowAccessFromLocalMachineCheckBox().addItemListener(this::onCheckBoxChanged);
        // actions of copy buttons
        connectionStringsJDBC.getCopyButton().addActionListener(this::onJDBCCopyButtonClicked);
        connectionStringsSpring.getCopyButton().addActionListener(this::onSpringCopyButtonClicked);
        // save/discard buttons
        propertyActionPanel.getSaveButton().addActionListener(this::onSaveButtonClicked);
        propertyActionPanel.getDiscardButton().addActionListener(this::onDiscardButtonClicked);
        // database combox changed
        databaseComboBox.addItemListener(this::onDatabaseComboBoxChanged);
    }

    private void onCheckBoxChanged(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED || itemEvent.getStateChange() == ItemEvent.DESELECTED) {
            final boolean changed = SqlServerPropertyView.this.changed();
            SqlServerPropertyView.this.propertyActionPanel.getSaveButton().setEnabled(changed);
            SqlServerPropertyView.this.propertyActionPanel.getDiscardButton().setEnabled(changed);
        }
    }

    private void onJDBCCopyButtonClicked(ActionEvent e) {
        Utils.copyToSystemClipboard(SqlServerPropertyView.this.connectionStringsJDBC.getOutputTextArea().getText());
    }

    private void onSpringCopyButtonClicked(ActionEvent e) {
        Utils.copyToSystemClipboard(SqlServerPropertyView.this.connectionStringsSpring.getOutputTextArea().getText());
    }

    private void onSaveButtonClicked(ActionEvent e) {
        final String actionName = "Saving";
        final String originalText = SqlServerPropertyView.this.propertyActionPanel.getSaveButton().getText();
        SqlServerPropertyView.this.propertyActionPanel.getSaveButton().setText(actionName);
        SqlServerPropertyView.this.propertyActionPanel.getSaveButton().setEnabled(false);
        final Runnable runnable = () -> {
            // refresh property
            final SqlServerEntity entity = this.property.getServer().entity();
            refreshProperty(entity.getSubscriptionId(), entity.getResourceGroupName(), entity.getName());
            final boolean allowAccessToAzureServices = connectionSecurity.getAllowAccessFromAzureServicesCheckBox().getModel().isSelected();
            final boolean allowAccessToLocal = connectionSecurity.getAllowAccessFromLocalMachineCheckBox().getModel().isSelected();
            if (!originalAllowAccessToAzureServices.equals(allowAccessToAzureServices) || !originalAllowAccessToLocal.equals(allowAccessToLocal)) {
                // update
                this.property.getServer().update()
                        .withEnableAccessFromAzureServices(allowAccessToAzureServices)
                        .withEnableAccessFromLocalMachine(allowAccessToLocal)
                        .commit();
                originalAllowAccessToAzureServices = allowAccessToAzureServices;
                originalAllowAccessToLocal = allowAccessToLocal;
            }
            SqlServerPropertyView.this.propertyActionPanel.getSaveButton().setText(originalText);
            final boolean changed = SqlServerPropertyView.this.changed();
            SqlServerPropertyView.this.propertyActionPanel.getSaveButton().setEnabled(changed);
            SqlServerPropertyView.this.propertyActionPanel.getDiscardButton().setEnabled(changed);
            final Map<String, String> properties = new HashMap<>();
            properties.put(TelemetryConstants.SUBSCRIPTIONID, entity.getSubscriptionId());
            properties.put("allowAccessToLocal", String.valueOf(allowAccessToLocal));
            properties.put("allowAccessToAzureServices", String.valueOf(allowAccessToAzureServices));
            EventUtil.logEvent(EventType.info, ActionConstants.parse(ActionConstants.MySQL.SAVE).getServiceName(),
                               ActionConstants.parse(ActionConstants.MySQL.SAVE).getOperationName(), properties);
        };
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(null, String.format("%s...", actionName), false, runnable));
    }

    private void onDiscardButtonClicked(ActionEvent e) {
        SqlServerPropertyView.this.propertyActionPanel.getSaveButton().setEnabled(false);
        SqlServerPropertyView.this.propertyActionPanel.getDiscardButton().setEnabled(false);
        connectionSecurity.getAllowAccessFromAzureServicesCheckBox().setSelected(originalAllowAccessToAzureServices);
        connectionSecurity.getAllowAccessFromLocalMachineCheckBox().setSelected(originalAllowAccessToLocal);
    }

    private void onDatabaseComboBoxChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof SqlDatabaseEntity) {
            final SqlDatabaseEntity database = (SqlDatabaseEntity) e.getItem();
            final SqlServerEntity entity = this.property.getServer().entity();
            final JdbcUrl jdbcUrl = this.getJdbcUrl(entity.getFullyQualifiedDomainName(),
                    database.getName(), overview.getServerAdminLoginNameTextField().getText());
            connectionStringsJDBC.getOutputTextArea().setText(DatabaseTemplateUtils.toJdbcTemplate(jdbcUrl));
            connectionStringsSpring.getOutputTextArea().setText(DatabaseTemplateUtils.toSpringTemplate(jdbcUrl, SQLSERVER_DRIVER_CLASS_NAME));
        }
    }

    private boolean changed() {
        return originalAllowAccessToAzureServices != connectionSecurity.getAllowAccessFromAzureServicesCheckBox().getModel().isSelected()
                || originalAllowAccessToLocal != connectionSecurity.getAllowAccessFromLocalMachineCheckBox().getModel().isSelected();
    }

    @Override
    public @NotNull JComponent getComponent() {
        return rootPanel;
    }

    @Override
    public @NotNull String getName() {
        return ID;
    }

    @Override
    public void dispose() {

    }

    // @Override
    public void onReadProperty(String sid, String resourceGroup, String name) {
        final String actionName = "Opening Property of";
        final Runnable runnable = () -> {
            // refresh property
            this.refreshProperty(sid, resourceGroup, name);
            // show property
            this.showProperty(this.property);
        };
        // show property in background
        final String taskTitle = Node.getProgressMessage(actionName, SqlServerModule.MODULE_NAME, name);
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(null, taskTitle, false, runnable));
    }

    @AzureOperation(name = "sqlserver.refresh_properties_view.server", params = {"name"}, type = AzureOperation.Type.ACTION)
    private void refreshProperty(String sid, String resourceGroup, String name) {
        // find server
        final SqlServer server = Azure.az(AzureSqlServer.class).sqlServer(sid, resourceGroup, name);
        this.property.setServer(server);
        final SqlServerEntity entity = property.getServer().entity();
        if ("Ready".equals(entity.getState())) {
            // find firewalls
            final List<FirewallRuleEntity> firewallRules = Azure.az(AzureSqlServer.class).sqlServer(entity.getId()).firewallRules();
            this.property.setFirewallRules(firewallRules);
        }
    }

    // @Override
    public void showProperty(SqlServerProperty property) {
        final SqlServerEntity entity = property.getServer().entity();
        final Subscription subscription = Azure.az(AzureAccount.class).account().getSubscription(entity.getSubscriptionId());
        if (subscription != null) {
            overview.getSubscriptionTextField().setText(subscription.getName());
        }
        databaseComboBox.setServer(property.getServer());
        overview.getResourceGroupTextField().setText(entity.getResourceGroupName());
        overview.getStatusTextField().setText(entity.getState());
        overview.getLocationTextField().setText(entity.getRegion().getLabel());
        overview.getSubscriptionIDTextField().setText(entity.getSubscriptionId());
        overview.getServerNameTextField().setText(entity.getFullyQualifiedDomainName());
        overview.getServerNameTextField().setCaretPosition(0);
        overview.getServerAdminLoginNameTextField().setText(entity.getAdministratorLoginName() + "@" + entity.getName());
        overview.getServerAdminLoginNameTextField().setCaretPosition(0);
        overview.getVersionTextField().setText(entity.getVersion());
        if ("Ready".equals(entity.getState())) {
            final List<FirewallRuleEntity> firewallRules = property.getFirewallRules();
            originalAllowAccessToAzureServices = firewallRules.stream()
                    .anyMatch(e -> FirewallRuleEntity.ACCESS_FROM_AZURE_SERVICES_FIREWALL_RULE_NAME.equalsIgnoreCase(e.getName()));
            connectionSecurity.getAllowAccessFromAzureServicesCheckBox().setSelected(originalAllowAccessToAzureServices);
            originalAllowAccessToLocal = firewallRules.stream()
                    .anyMatch(e -> StringUtils.equalsIgnoreCase(FirewallRuleEntity.getAccessFromLocalFirewallRuleName(), e.getName()));
            connectionSecurity.getAllowAccessFromLocalMachineCheckBox().setSelected(originalAllowAccessToLocal);
        } else {
            connectionSecuritySeparator.collapse();
            connectionSecuritySeparator.setEnabled(false);
            connectionStringsSeparator.collapse();
            connectionStringsSeparator.setEnabled(false);
        }

    }
}
