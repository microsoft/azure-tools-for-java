/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureIntegerInput;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseConfig;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CosmosDatabaseCreationDialog extends AzureDialog<DatabaseConfig> implements AzureForm<DatabaseConfig> {
    private JPanel pnlRoot;
    private AzureTextInput txtName;
    private JRadioButton autoscaleRadioButton;
    private JRadioButton manualRadioButton;
    private JLabel lblThroughputRu;
    private JLabel lblMaxThroughput;
    private AzureIntegerInput txtThroughputRu;
    private AzureIntegerInput txtMaxThroughput;

    private final Project project;

    public CosmosDatabaseCreationDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        $$$setupUI$$$();
        this.init();
    }

    protected void init() {
        super.init();

        final ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(autoscaleRadioButton);
        buttonGroup.add(manualRadioButton);
        autoscaleRadioButton.addItemListener(e -> toggleThroughputStatus());
        manualRadioButton.addItemListener(e -> toggleThroughputStatus());
        txtName.addValidator(() -> validateDatabaseName());
        txtThroughputRu.setMinValue(400);
        txtThroughputRu.setMaxValue(1000000);
        txtThroughputRu.setValue(400);
        txtThroughputRu.addValidator(() -> validateThroughputIncrements(txtThroughputRu));
        txtMaxThroughput.setMinValue(1000);
        txtMaxThroughput.setValue(4000);
        txtMaxThroughput.addValidator(() -> validateThroughputIncrements(txtThroughputRu));

        autoscaleRadioButton.setSelected(true);
    }

    private AzureValidationInfo validateDatabaseName() {
            final String value = txtName.getValue();
            return StringUtils.endsWith(value, StringUtils.SPACE) || StringUtils.containsAny(value, "\\", "/","#", "?", "%") ?
                    AzureValidationInfo.error("Database name not end with space nor contains characters '\\', '/', '#', '?', '%'", txtName) : AzureValidationInfo.success(txtName);
    }

    private AzureValidationInfo validateThroughputIncrements(@Nonnull AzureIntegerInput input) {
        final Integer value = input.getValue();
        return Objects.nonNull(value) && value % 100 == 0 ? AzureValidationInfo.success(input) : AzureValidationInfo.error("Throughput must be in multiples of 100", input);
    }

    private void toggleThroughputStatus() {
        lblMaxThroughput.setVisible(autoscaleRadioButton.isSelected());
        txtMaxThroughput.setVisible(autoscaleRadioButton.isSelected());
        txtMaxThroughput.setRequired(autoscaleRadioButton.isSelected());
        lblThroughputRu.setVisible(manualRadioButton.isSelected());
        txtThroughputRu.setVisible(manualRadioButton.isSelected());
        txtThroughputRu.setRequired(manualRadioButton.isSelected());
    }

    @Override
    public AzureForm<DatabaseConfig> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Database";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public DatabaseConfig getValue() {
        final DatabaseConfig result = new DatabaseConfig();
        result.setName(txtName.getValue());
        if (autoscaleRadioButton.isSelected()) {
            result.setMaxThroughput(txtMaxThroughput.getValue());
        } else if (manualRadioButton.isSelected()) {
            result.setThroughput(txtThroughputRu.getValue());
        }
        return result;
    }

    @Override
    public void setValue(@Nonnull DatabaseConfig data) {
        txtName.setValue(data.getName());
        if (Objects.nonNull(data.getThroughput())) {
            manualRadioButton.setSelected(true);
            txtThroughputRu.setValue(data.getThroughput());
        } else {
            autoscaleRadioButton.setSelected(true);
            Optional.ofNullable(data.getMaxThroughput()).ifPresent(value -> txtMaxThroughput.setValue(value));
        }
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtName, txtMaxThroughput, txtThroughputRu);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}
