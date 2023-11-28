/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.table;

import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class AddAppSettingsDialog extends AzureDialog<Pair<String, String>>
    implements AzureForm<Pair<String, String>> {

    private static final Pattern APP_SETTINGS_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\.]+$");

    private JPanel pnlRoot;
    private AzureTextInput txtName;
    private AzureTextInput txtValue;
    private JLabel lblName;
    private JLabel lblValue;

    private final AppSettingsTable table;

    public AddAppSettingsDialog(@Nonnull final AppSettingsTable table) {
        super();
        this.table = table;
        $$$setupUI$$$();
        init();
    }

    @Override
    protected void init() {
        super.init();
        txtName.setRequired(true);
        txtName.addValidator(this::validateName);
        txtValue.setRequired(false);

        lblName.setLabelFor(txtName);
        lblValue.setLabelFor(txtValue);
    }

    private AzureValidationInfo validateName() {
        final String value = txtName.getValue();
        if (StringUtils.isEmpty(value)) {
            return AzureValidationInfo.error("Name is a required property", txtName);
        } else if (!APP_SETTINGS_NAME_PATTERN.matcher(value).matches()) {
            return AzureValidationInfo.error("App setting names can only contain letters, numbers (0-9), periods (\".\"), and underscores (\"_\")", txtName);
        } else if (table.getAppSettings().containsKey(value)) {
            return AzureValidationInfo.error("App setting names must be unique", txtName);
        }
        return AzureValidationInfo.success(txtName);
    }

    @Override
    public AzureForm<Pair<String, String>> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Add App Setting";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public Pair<String, String> getValue() {
        return Pair.of(txtName.getValue(), txtValue.getValue());
    }

    @Override
    public void setValue(Pair<String, String> data) {
        txtName.setValue(data.getKey());
        txtValue.setValue(data.getValue());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtName, txtValue);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}
