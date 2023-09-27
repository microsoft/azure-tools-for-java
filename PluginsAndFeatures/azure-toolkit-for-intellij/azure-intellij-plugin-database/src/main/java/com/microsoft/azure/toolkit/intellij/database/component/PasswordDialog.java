/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnimatedIcon;
import com.intellij.util.IconUtil;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.database.connection.Database;
import com.microsoft.azure.toolkit.intellij.database.connection.DatabaseConnectionUtils;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabase;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import java.util.List;

public class PasswordDialog extends AzureDialog<char[]> implements AzureForm<char[]> {

    private static final String TITLE = "Credential for \"%s\"";
    private static final String HEADER_PATTERN = "Please provide credential for user (%s) to access database (%s) on server (%s).";
    private final Database database;

    private JPanel root;
    private JLabel headerIconLabel;
    private JTextPane headerTextPane;
    private JTextPane testResultTextPane;
    private AzureActionButton<Database> testConnectionButton;
    private TestConnectionActionPanel testConnectionActionPanel;
    private JPasswordField passwordField;

    public PasswordDialog(Project project, Database database) {
        super(project);
        this.database = database;
        setTitle(String.format(TITLE, database.getName()));
        headerTextPane.setText(String.format(HEADER_PATTERN, this.database.getUsername(), this.database.getJdbcUrl().getDatabase(),
                this.database.getJdbcUrl().getServerHost()));
        testConnectionButton.setEnabled(false);
        testConnectionActionPanel.setVisible(false);
        testResultTextPane.setVisible(false);
        testResultTextPane.setEditable(false);
        testResultTextPane.setText(StringUtils.EMPTY);
        this.setValue(this.database.getPassword());
        this.init();
        this.initListener();
        this.headerIconLabel.setIcon(IconUtil.scale(AllIcons.General.BalloonWarning, headerIconLabel, 2.0f));
    }

    private void initListener() {
        this.passwordField.addKeyListener(this.onInputPasswordFieldChanged());
        final Action<Database> testConnectionAction = new Action<Database>(Action.Id.of("user/$database.test_connection.database"))
                .withAuthRequired(false)
                .withSource(this.database)
                .withIdParam(this.database.getName())
                .withHandler(ignore -> this.onTestConnectionButtonClicked());
        this.testConnectionButton.setAction(testConnectionAction);
        final Action<IDatabase> copyAction = new Action<IDatabase>(Action.Id.of("user/$database.copy_connection_string.database"))
                .withAuthRequired(false)
                .withSource(this.database)
                .withIdParam(this.database.getName())
                .withHandler(ignore -> onCopyButtonClicked());
    }

    private KeyListener onInputPasswordFieldChanged() {
        return new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                testConnectionButton.setEnabled(ArrayUtils.isNotEmpty(passwordField.getPassword()));
            }
        };
    }

    private void onTestConnectionButtonClicked() {
        testConnectionButton.setEnabled(false);
        testConnectionButton.setIcon(new AnimatedIcon.Default());
        testConnectionButton.setDisabledIcon(new AnimatedIcon.Default());
        final String password = String.valueOf(passwordField.getPassword());
        final Runnable runnable = () -> {
            final DatabaseConnectionUtils.ConnectResult connectResult = DatabaseConnectionUtils
                    .connectWithPing(this.database.getJdbcUrl(), this.database.getUsername(), password);
            testConnectionActionPanel.setVisible(true);
            testResultTextPane.setVisible(true);
            testResultTextPane.setText(getConnectResultMessage(connectResult));
            final Icon icon = connectResult.isConnected() ? AllIcons.General.InspectionsOK : AllIcons.General.BalloonError;
            testConnectionActionPanel.getIconLabel().setIcon(icon);
            testConnectionButton.setIcon(null);
            testConnectionButton.setEnabled(true);
        };
        final String title = AzureMessageBundle.message("azure.mysql.link.connection.title", this.database.getJdbcUrl().getServerHost()).toString();
        final AzureTask<Void> task = new AzureTask<>(null, title, false, runnable);
        AzureTaskManager.getInstance().runInBackground(task);
    }

    private String getConnectResultMessage(DatabaseConnectionUtils.ConnectResult result) {
        final StringBuilder messageBuilder = new StringBuilder();
        if (result.isConnected()) {
            messageBuilder.append("Connected successfully.").append(System.lineSeparator());
            messageBuilder.append("Version: ").append(result.getServerVersion()).append(System.lineSeparator());
            messageBuilder.append("Ping cost: ").append(result.getPingCost()).append("ms");
        } else {
            messageBuilder.append("Failed to connect with above parameters.").append(System.lineSeparator());
            messageBuilder.append("Message: ").append(result.getMessage());
        }
        return messageBuilder.toString();
    }

    private void onCopyButtonClicked() {
        try {
            CopyPasteManager.getInstance().setContents(new StringSelection(testResultTextPane.getText()));
        } catch (final Exception exception) {
            final String error = "copy test result error";
            final String action = "try again later.";
            throw new AzureToolkitRuntimeException(error, action);
        }
    }

    @Override
    public AzureForm<char[]> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return TITLE;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return root;
    }

    @Override
    public char[] getValue() {
        return passwordField.getPassword();
    }

    @Override
    public void setValue(char[] data) {
        this.passwordField.setText(String.valueOf(data));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.emptyList();
    }
}
