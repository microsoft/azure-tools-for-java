/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.sqlserver.creation;

import com.google.common.base.Preconditions;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.sqlserver.SqlServerConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;


public class SqlServerCreationDialog extends AzureDialog<SqlServerConfig> {
    private static final String DIALOG_TITLE = "Create SQL Server";
    private JPanel rootPanel;
    private SqlServerCreationBasicPanel basic;
    private SqlServerCreationAdvancedPanel advanced;

    private boolean advancedMode;
    private JCheckBox checkboxMode;

    public SqlServerCreationDialog(@Nullable Project project) {
        super(project);
        init();
    }

    @Override
    protected void init() {
        super.init();
        advanced.setVisible(false);
    }

    @Override
    public AzureForm<SqlServerConfig> getForm() {
        return this.advancedMode ? advanced : basic;
    }

    @Override
    protected String getDialogTitle() {
        return DIALOG_TITLE;
    }

    @Override
    protected JComponent createDoNotAskCheckbox() {
        this.checkboxMode = new JCheckBox(AzureMessageBundle.message("common.moreSetting").toString());
        this.checkboxMode.setVisible(true);
        this.checkboxMode.setSelected(false);
        this.checkboxMode.addActionListener(e -> this.toggleAdvancedMode(this.checkboxMode.isSelected()));
        return this.checkboxMode;
    }

    protected void toggleAdvancedMode(boolean advancedMode) {
        this.advancedMode = advancedMode;
        if (advancedMode) {
            advanced.setValue(basic.getValue());
        } else {
            basic.setValue(advanced.getValue());
        }
        advanced.setVisible(advancedMode);
        basic.setVisible(!advancedMode);
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return advancedMode ? advanced.getServerNameTextField() : basic.getServerNameTextField();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return rootPanel;
    }

    private void createUIComponents() {
        List<Subscription> selectedSubscriptions = Azure.az(AzureAccount.class).account().getSelectedSubscriptions();
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(selectedSubscriptions), "There is no subscription in your account.");
        SqlServerConfig config = SqlServerConfig.getDefaultConfig(selectedSubscriptions.get(0));
        basic = new SqlServerCreationBasicPanel(config);
        advanced = new SqlServerCreationAdvancedPanel(config);
    }
}
