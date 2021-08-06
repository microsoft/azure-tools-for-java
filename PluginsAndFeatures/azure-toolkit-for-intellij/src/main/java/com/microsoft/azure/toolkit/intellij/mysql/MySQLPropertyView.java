/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.mysql;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.AzureHideableTitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.BaseEditor;
import com.microsoft.azure.toolkit.intellij.database.ui.ConnectionSecurityPanel;
import com.microsoft.azure.toolkit.intellij.database.ui.ConnectionStringsOutputPanel;
import com.microsoft.azure.toolkit.intellij.database.ui.MySQLPropertyActionPanel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.mysql.model.MySqlDatabaseEntity;
import com.microsoft.azure.toolkit.lib.mysql.service.AzureMySql;
import com.microsoft.azure.toolkit.lib.mysql.service.MySqlServer;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLProperty;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLPropertyMvpView;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class MySQLPropertyView extends BaseEditor implements MySQLPropertyMvpView {

    public static final String ID = "com.microsoft.intellij.helpers.mysql.MySQLPropertyView";

    private AzureHideableTitledSeparator overviewSeparator;
    private MySQLPropertryOverviewPanel overview;
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
    public static final String MYSQL_OUTPUT_TEXT_PATTERN_SPRING =
            "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver" + System.lineSeparator() +
            "spring.datasource.url=jdbc:mysql://%s:3306/%s?useSSL=true&requireSSL=false" + System.lineSeparator() +
            "spring.datasource.username=%s" + System.lineSeparator() + "spring.datasource.password={your_password}";

    public static final String MYSQL_OUTPUT_TEXT_PATTERN_JDBC =
            "String url =\"jdbc:mysql://%s:3306/%s?useSSL=true&requireSSL=false\";" + System.lineSeparator() +
            "myDbConn = DriverManager.getConnection(url, \"%s\", {your_password});";

    private MySQLProperty property;

    private Boolean originalAllowAccessToAzureServices;
    private Boolean originalAllowAccessToLocal;
    private final Project project;
    private final VirtualFile virtualFile;

    MySQLPropertyView(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        super();
        this.project = project;
        this.virtualFile = virtualFile;
        overviewSeparator.addContentComponent(overview);
        connectionSecuritySeparator.addContentComponent(connectionSecurity);
        connectionStringsSeparator.addContentComponent(databaseLabel);
        connectionStringsSeparator.addContentComponent(databaseComboBox);
        connectionStringsSeparator.addContentComponent(connectionStringsJDBC);
        connectionStringsSeparator.addContentComponent(connectionStringsSpring);
        connectionStringsJDBC.getTitleLabel().setText("JDBC");
        connectionStringsJDBC.getOutputTextArea().setText(getConnectionString(MYSQL_OUTPUT_TEXT_PATTERN_JDBC, null, null, null));
        connectionStringsSpring.getTitleLabel().setText("Spring");
        connectionStringsSpring.getOutputTextArea().setText(getConnectionString(MYSQL_OUTPUT_TEXT_PATTERN_SPRING, null, null, null));
        init();
        initListeners();

        AzureEventBus.after("mysql|server.delete", (MySqlServer server) -> {
            if (StringUtils.equalsIgnoreCase(this.property.getServer().id(), server.id())) {
                this.closeEditor();
            }
        });

        AzureEventBus.after("mysql|server.restart", this::onMySqlServerStatusChanged);
        AzureEventBus.after("mysql|server.stop", this::onMySqlServerStatusChanged);
        AzureEventBus.before("mysql|server.start", this::onMySqlServerStatusChanging);
        AzureEventBus.before("mysql|server.stop", this::onMySqlServerStatusChanging);
        AzureEventBus.before("mysql|server.restart", this::onMySqlServerStatusChanging);
        AzureEventBus.before("mysql|server.delete", this::onMySqlServerStatusDeleting);
    }

    private void onMySqlServerStatusChanged(MySqlServer server) {
        if (StringUtils.equalsIgnoreCase(this.property.getServer().id(), server.id())) {
            AzureTaskManager.getInstance().runOnPooledThread(() -> {
                this.property.getServer().refresh();
                AzureTaskManager.getInstance().runLater(() -> {
                    showProperty(property);
                });
            });
        }
    }

    private void onMySqlServerStatusChanging(MySqlServer server) {
        if (StringUtils.equalsIgnoreCase(this.property.getServer().id(), server.id())) {
            AzureTaskManager.getInstance().runLater(() -> overview.getStatusTextField().setText("Updating..."));
        }
    }

    private void onMySqlServerStatusDeleting(MySqlServer server) {
        if (StringUtils.equalsIgnoreCase(this.property.getServer().id(), server.id())) {
            AzureTaskManager.getInstance().runLater(() -> overview.getStatusTextField().setText("Deleting..."));
        }
    }

    private void closeEditor() {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        AzureTaskManager.getInstance().runLater(() -> fileEditorManager.closeFile(virtualFile));
        PluginUtil.showInfoNotificationProject(project,
            String.format("The editor for MySQL server '%s' is closed.", this.property.getServer().name()),
            String.format("The MySQL server with name '%s' is deleted.",
                this.property.getServer().name()));
    }

    private String getConnectionString(final String pattern, final String hostname, final String database, final String username) {
        final String newHostname = StringUtils.isNotBlank(hostname) ? hostname : "{your_hostname}";
        final String newDatabase = StringUtils.isNotBlank(database) ? database : "{your_database}";
        final String newUsername = StringUtils.isNotBlank(username) ? username : "{your_username}";
        return String.format(pattern, newHostname, newDatabase, newUsername);
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
            final boolean changed = MySQLPropertyView.this.changed();
            MySQLPropertyView.this.propertyActionPanel.getSaveButton().setEnabled(changed);
            MySQLPropertyView.this.propertyActionPanel.getDiscardButton().setEnabled(changed);
        }
    }

    private void onJDBCCopyButtonClicked(ActionEvent e) {
        try {
            Utils.copyToSystemClipboard(MySQLPropertyView.this.connectionStringsJDBC.getOutputTextArea().getText());
        } catch (final Exception exception) {
            final String error = "copy JDBC connection strings";
            final String action = "try again later.";
            throw new AzureToolkitRuntimeException(error, action);
        }
    }

    private void onSpringCopyButtonClicked(ActionEvent e) {
        try {
            Utils.copyToSystemClipboard(MySQLPropertyView.this.connectionStringsSpring.getOutputTextArea().getText());
        } catch (final Exception exception) {
            final String error = "copy Spring connection strings";
            final String action = "try again later.";
            throw new AzureToolkitRuntimeException(error, action);
        }
    }

    private void onSaveButtonClicked(ActionEvent e) {
        final String actionName = "Saving";
        final String originalText = MySQLPropertyView.this.propertyActionPanel.getSaveButton().getText();
        MySQLPropertyView.this.propertyActionPanel.getSaveButton().setText(actionName);
        MySQLPropertyView.this.propertyActionPanel.getSaveButton().setEnabled(false);
        final Runnable runnable = () -> {
            final String subscriptionId = property.getSubscriptionId();
            // refresh property
            refreshProperty(subscriptionId, property.getServer().entity().getResourceGroup(), property.getServer().name());
            final boolean allowAccessToAzureServices = connectionSecurity.getAllowAccessFromAzureServicesCheckBox().getModel().isSelected();
            if (!originalAllowAccessToAzureServices.equals(allowAccessToAzureServices)) {
                if (allowAccessToAzureServices) {
                    property.getServer().firewallRules().enableAzureAccessRule();
                } else {
                    property.getServer().firewallRules().disableAzureAccessRule();
                }
                originalAllowAccessToAzureServices = allowAccessToAzureServices;
            }
            final boolean allowAccessToLocal = connectionSecurity.getAllowAccessFromLocalMachineCheckBox().getModel().isSelected();
            if (!originalAllowAccessToLocal.equals(allowAccessToLocal)) {
                if (allowAccessToLocal) {
                    property.getServer().firewallRules().enableLocalMachineAccessRule(property.getServer().getPublicIpForLocalMachine());
                } else {
                    property.getServer().firewallRules().disableLocalMachineAccessRule();
                }
                originalAllowAccessToLocal = allowAccessToLocal;
            }
            MySQLPropertyView.this.propertyActionPanel.getSaveButton().setText(originalText);
            final boolean changed = MySQLPropertyView.this.changed();
            MySQLPropertyView.this.propertyActionPanel.getSaveButton().setEnabled(changed);
            MySQLPropertyView.this.propertyActionPanel.getDiscardButton().setEnabled(changed);
            final Map<String, String> properties = new HashMap<>();
            properties.put(TelemetryConstants.SUBSCRIPTIONID, subscriptionId);
            properties.put("allowAccessToLocal", String.valueOf(allowAccessToLocal));
            properties.put("allowAccessToAzureServices", String.valueOf(allowAccessToAzureServices));
            EventUtil.logEvent(EventType.info, ActionConstants.parse(ActionConstants.MySQL.SAVE).getServiceName(),
                               ActionConstants.parse(ActionConstants.MySQL.SAVE).getOperationName(), properties);
        };
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(this.project, String.format("%s...", actionName), false, runnable));
    }

    private void onDiscardButtonClicked(ActionEvent e) {
        MySQLPropertyView.this.propertyActionPanel.getSaveButton().setEnabled(false);
        MySQLPropertyView.this.propertyActionPanel.getDiscardButton().setEnabled(false);
        connectionSecurity.getAllowAccessFromAzureServicesCheckBox().setSelected(originalAllowAccessToAzureServices);
        connectionSecurity.getAllowAccessFromLocalMachineCheckBox().setSelected(originalAllowAccessToLocal);
    }

    private void onDatabaseComboBoxChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof MySqlDatabaseEntity) {
            final MySqlDatabaseEntity database = (MySqlDatabaseEntity) e.getItem();
            connectionStringsJDBC.getOutputTextArea().setText(getConnectionString(MYSQL_OUTPUT_TEXT_PATTERN_JDBC,
                    property.getServer().entity().getFullyQualifiedDomainName(), database.getName(), overview.getServerAdminLoginNameTextField().getText()));
            connectionStringsSpring.getOutputTextArea().setText(getConnectionString(MYSQL_OUTPUT_TEXT_PATTERN_SPRING,
                property.getServer().entity().getFullyQualifiedDomainName(), database.getName(), overview.getServerAdminLoginNameTextField().getText()));
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

    @Override
    public void onReadProperty(String sid, String resourceGroup, String name) {
        final String actionName = "Opening Property of";
        final Runnable runnable = () -> {
            // refresh property
            this.refreshProperty(sid, resourceGroup, name);
            // show property
            this.showProperty(this.property);
        };
        // show property in background
        final String taskTitle = Node.getProgressMessage(actionName, MySQLModule.MODULE_NAME, name);
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(this.project, taskTitle, false, runnable));
    }

    private void refreshProperty(String sid, String resourceGroup, String name) {
        final MySQLProperty newProperty = new MySQLProperty();
        newProperty.setSubscriptionId(sid);
        // find server
        try {
            newProperty.setServer(Azure.az(AzureMySql.class).subscription(sid).get(resourceGroup, name));
        } catch (final Exception ex) {
            final String error = "find Azure Database for MySQL server information";
            final String action = "confirm your network is available and your server actually exists.";
            throw new AzureToolkitRuntimeException(error, action);
        }
        if (StringUtils.equalsIgnoreCase("READY", newProperty.getServer().entity().getState())) {
            // find firewalls
            newProperty.setFirewallRules(newProperty.getServer().firewallRules().list());
        }
        this.property = newProperty;
    }

    @Override
    public void showProperty(MySQLProperty property) {
        final MySqlServer server = property.getServer();
        final String sid = server.entity().getSubscriptionId();
        if (!server.exists()) {
            this.closeEditor();
            return;
        }
        final Subscription subscription = az(AzureAccount.class).account().getSubscription(sid);
        if (subscription != null) {
            overview.getSubscriptionTextField().setText(subscription.getName());
            databaseComboBox.setSubscription(subscription);
            databaseComboBox.setServer(server);
        }
        overview.getResourceGroupTextField().setText(server.entity().getResourceGroup());
        overview.getStatusTextField().setText(server.entity().getState());
        overview.getLocationTextField().setText(server.entity().getRegion().getLabel());
        overview.getSubscriptionIDTextField().setText(sid);
        overview.getServerNameTextField().setText(server.entity().getFullyQualifiedDomainName());
        overview.getServerNameTextField().setCaretPosition(0);
        overview.getServerAdminLoginNameTextField().setText(server.entity().getAdministratorLoginName() + "@" + server.name());
        overview.getServerAdminLoginNameTextField().setCaretPosition(0);
        overview.getMysqlVersionTextField().setText(server.entity().getVersion());
        final String skuTier = server.entity().getSkuTier();
        final int skuCapacity = server.entity().getVCore();
        final int storageGB = server.entity().getStorageInMB() / 1024;
        final String performanceConfigurations = skuTier + ", " + skuCapacity + " vCore(s), " + storageGB + " GB";
        overview.getPerformanceConfigurationsTextField().setText(performanceConfigurations);
        overview.getSslEnforceStatusTextField().setText(server.entity().getSslEnforceStatus());
        if (StringUtils.equalsIgnoreCase("READY", server.entity().getState())) {
            originalAllowAccessToAzureServices = server.firewallRules().isAzureAccessRuleEnabled();
            connectionSecurity.getAllowAccessFromAzureServicesCheckBox().setSelected(originalAllowAccessToAzureServices);
            originalAllowAccessToLocal = server.firewallRules().isLocalMachineAccessRuleEnabled();
            connectionSecurity.getAllowAccessFromLocalMachineCheckBox().setSelected(originalAllowAccessToLocal);
        } else {
            connectionSecuritySeparator.collapse();
            connectionSecuritySeparator.setEnabled(false);
            connectionStringsSeparator.collapse();
            connectionStringsSeparator.setEnabled(false);
        }
    }
}
