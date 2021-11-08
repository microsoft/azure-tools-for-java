/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.mysql.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySQLConfig;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class MySQLCreationDialog extends AzureDialog<AzureMySQLConfig> {
    private static final String DIALOG_TITLE = "Create Azure Database for MySQL";
    private JPanel rootPanel;
    private MySQLCreationBasicPanel basic;
    private MySQLCreationAdvancedPanel advanced;

    private boolean advancedMode;
    private JCheckBox checkboxMode;

    public MySQLCreationDialog(@Nullable Project project) {
        super(project);
        init();
    }

    @Override
    protected void init() {
        super.init();
        advanced.setVisible(false);
    }

    @Override
    public AzureForm<AzureMySQLConfig> getForm() {
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
        AzureMySQLConfig config = AzureMySQLConfig.getDefaultAzureMySQLConfig();
        basic = new MySQLCreationBasicPanel(config);
        advanced = new MySQLCreationAdvancedPanel(config);
    }
}
