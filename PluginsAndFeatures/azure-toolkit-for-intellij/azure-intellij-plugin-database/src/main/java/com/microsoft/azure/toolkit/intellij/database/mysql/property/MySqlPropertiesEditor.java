/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.mysql.property;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.AzureHideableTitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.properties.AzResourcePropertiesEditor;
import com.microsoft.azure.toolkit.intellij.database.component.ConnectionSecurityPanel;
import com.microsoft.azure.toolkit.intellij.database.component.ConnectionStringsOutputPanel;
import com.microsoft.azure.toolkit.intellij.database.component.DatabaseComboBox;
import com.microsoft.azure.toolkit.intellij.database.component.DatabaseServerPropertyActionPanel;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import com.microsoft.azure.toolkit.lib.mysql.MySqlDatabase;
import com.microsoft.azure.toolkit.lib.mysql.MySqlServer;
import com.microsoft.azure.toolkit.lib.mysql.MySqlServerDraft;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import java.awt.event.ItemEvent;

public class MySqlPropertiesEditor extends AzResourcePropertiesEditor<MySqlServer> {

    public static final String ID = "com.microsoft.azure.toolkit.intellij.mysql.property.MySqlPropertiesEditor";
    private final Project project;

    @Nonnull
    private final MySqlServer server;
    @Nonnull
    private final MySqlServerDraft draft;

    private AzureHideableTitledSeparator overviewSeparator;
    private MySqlPropertyOverviewPanel overview;
    private AzureHideableTitledSeparator connectionSecuritySeparator;
    private ConnectionSecurityPanel connectionSecurity;
    private AzureHideableTitledSeparator connectionStringsSeparator;
    private ConnectionStringsOutputPanel connectionStringsJDBC;
    private ConnectionStringsOutputPanel connectionStringsSpring;
    private JPanel rootPanel;
    private JPanel contextPanel;
    private JScrollPane scrollPane;
    private DatabaseServerPropertyActionPanel propertyActionPanel;
    private DatabaseComboBox<MySqlDatabase> databaseComboBox;
    private JLabel databaseLabel;
    public static final String MYSQL_OUTPUT_TEXT_PATTERN_SPRING =
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver" + System.lineSeparator() +
            "spring.datasource.url=jdbc:mysql://%s:3306/%s?serverTimezone=UTC&useSSL=true&requireSSL=false" + System.lineSeparator() +
            "spring.datasource.username=%s" + System.lineSeparator() + "spring.datasource.password={your_password}";

    public static final String MYSQL_OUTPUT_TEXT_PATTERN_JDBC =
        "String url =\"jdbc:mysql://%s:3306/%s?serverTimezone=UTC&useSSL=true&requireSSL=false\";" + System.lineSeparator() +
            "myDbConn = DriverManager.getConnection(url, \"%s\", {your_password});";

    @Override
    protected void rerender() {
        AzureTaskManager.getInstance().runLater(() -> this.setData(this.draft));
    }

    MySqlPropertiesEditor(@Nonnull Project project, @Nonnull MySqlServer server, @Nonnull final VirtualFile virtualFile) {
        super(virtualFile, server, project);
        this.project = project;
        this.server = server;
        this.draft = (MySqlServerDraft) server.update();

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
        this.rerender();
        this.initListeners();
    }

    private String getConnectionString(final String pattern, final String hostname, final String database, final String username) {
        final String newHostname = StringUtils.isNotBlank(hostname) ? hostname : "{your_hostname}";
        final String newDatabase = StringUtils.isNotBlank(database) ? database : "{your_database}";
        final String newUsername = StringUtils.isNotBlank(username) ? username : "{your_username}";
        return String.format(pattern, newHostname, newDatabase, newUsername);
    }

    private void setData(MySqlServer server) {
        this.overview.setFormData(this.server);
        this.databaseComboBox.setServer(this.server);
        this.refreshButtons();
        final boolean ready = StringUtils.equalsIgnoreCase("READY", this.server.getStatus());
        connectionSecuritySeparator.expand();
        connectionStringsSeparator.expand();
        connectionSecuritySeparator.setEnabled(ready);
        connectionStringsSeparator.setEnabled(ready);
        connectionSecurity.getAllowAccessFromAzureServicesCheckBox().setEnabled(ready);
        connectionSecurity.getAllowAccessFromLocalMachineCheckBox().setEnabled(ready);
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            final boolean originalAllowAccessToAzureServices = this.draft.isAzureServiceAccessAllowed();
            final boolean originalAllowAccessToLocal = this.draft.isLocalMachineAccessAllowed();
            AzureTaskManager.getInstance().runLater(() -> {
                connectionSecurity.getAllowAccessFromAzureServicesCheckBox().setSelected(originalAllowAccessToAzureServices);
                connectionSecurity.getAllowAccessFromLocalMachineCheckBox().setSelected(originalAllowAccessToLocal);
            });
        });
    }

    private void initListeners() {
        // update to trigger save/discard buttons
        connectionSecurity.getAllowAccessFromAzureServicesCheckBox().addItemListener(this::onCheckBoxChanged);
        connectionSecurity.getAllowAccessFromLocalMachineCheckBox().addItemListener(this::onCheckBoxChanged);
        // save/discard buttons
        final Action<MySqlServer> applyAction = new Action<MySqlServer>(Action.Id.of("user/mysql.update_server.server"))
                .withAuthRequired(true)
                .withSource(this.server)
                .withIdParam(this.server.getName())
                .withHandler(server -> this.apply());
        propertyActionPanel.getSaveButton().setAction(applyAction);
        final Action<MySqlServer> resetAction = new Action<MySqlServer>(Action.Id.of("user/mysql.refresh.server"))
                .withAuthRequired(true)
                .withSource(this.server)
                .withIdParam(this.server.getName())
                .withHandler(server -> this.reset());
        propertyActionPanel.getDiscardButton().setAction(resetAction);
        // database combobox changed
        databaseComboBox.addItemListener(this::onDatabaseComboBoxChanged);
    }

    private void onCheckBoxChanged(ItemEvent itemEvent) {
        this.refreshButtons();
    }

    private void apply() {
        this.propertyActionPanel.getSaveButton().setEnabled(false);
        this.propertyActionPanel.getDiscardButton().setEnabled(false);
        final Runnable runnable = () -> {
            final String subscriptionId = draft.getSubscriptionId();
            final boolean allowAccessToAzureServices = connectionSecurity.getAllowAccessFromAzureServicesCheckBox().getModel().isSelected();
            final boolean allowAccessToLocal = connectionSecurity.getAllowAccessFromLocalMachineCheckBox().getModel().isSelected();
            this.draft.setAzureServiceAccessAllowed(allowAccessToAzureServices);
            this.draft.setLocalMachineAccessAllowed(allowAccessToLocal);
            final OperationContext context = OperationContext.action();
            context.setTelemetryProperty("subscriptionId", subscriptionId);
            context.setTelemetryProperty("allowAccessToLocal", String.valueOf(allowAccessToLocal));
            context.setTelemetryProperty("allowAccessToAzureServices", String.valueOf(allowAccessToAzureServices));
            this.draft.commit();
            this.refreshButtons();
        };
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(this.project, "Saving updates", false, runnable));
    }

    private void refreshButtons() {
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            final boolean modified = this.isModified();
            AzureTaskManager.getInstance().runLater(() -> {
                this.propertyActionPanel.getSaveButton().setEnabled(modified);
                this.propertyActionPanel.getDiscardButton().setEnabled(modified);
            });
        });
    }

    private void reset() {
        this.draft.reset();
        this.rerender();
    }

    private void onDatabaseComboBoxChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof MySqlDatabase) {
            final MySqlDatabase database = (MySqlDatabase) e.getItem();
            final String username = this.draft.getFullAdminName();
            connectionStringsJDBC.getOutputTextArea().setText(getConnectionString(MYSQL_OUTPUT_TEXT_PATTERN_JDBC,
                draft.getFullyQualifiedDomainName(), database.getName(), username));
            connectionStringsSpring.getOutputTextArea().setText(getConnectionString(MYSQL_OUTPUT_TEXT_PATTERN_SPRING,
                draft.getFullyQualifiedDomainName(), database.getName(), username));
        }
    }

    @Override
    public boolean isModified() {
        return this.draft.isAzureServiceAccessAllowed() != connectionSecurity.getAllowAccessFromAzureServicesCheckBox().getModel().isSelected() ||
            this.draft.isLocalMachineAccessAllowed() != connectionSecurity.getAllowAccessFromLocalMachineCheckBox().getModel().isSelected();
    }

    @Override
    public @Nonnull
    JComponent getComponent() {
        return rootPanel;
    }

    protected void refresh() {
        this.propertyActionPanel.getDiscardButton().setEnabled(false);
        this.propertyActionPanel.getSaveButton().setEnabled(false);
        final String refreshTitle = String.format("Refreshing MySQL server(%s)...", this.draft.getName());
        AzureTaskManager.getInstance().runInBackground(refreshTitle, () -> {
            this.draft.reset();
            this.draft.refresh();
            this.rerender();
        });
    }
}
