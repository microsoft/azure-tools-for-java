/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.creation.secret;

import com.intellij.icons.AllIcons;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBPasswordField;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.AzurePasswordFieldInput;
import com.microsoft.azure.toolkit.intellij.keyvault.KeyVaultCredentialActions;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.keyvault.secret.SecretDraft;
import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class SecretCreationDialog extends AzureDialog<SecretDraft.Config> implements AzureFormPanel<SecretDraft.Config> {
    private final String title;
    private JPanel pnlRoot;
    private JLabel lblName;
    private AzureTextInput txtName;
    private JLabel lblValue;
    private TitledSeparator titleInstance;
    private TitledSeparator titleSettings;
    private JLabel lblEnabled;
    private JRadioButton rdoYes;
    private JRadioButton rdoNo;
    private AzureTextInput txtContentType;
    private JLabel lblContentType;
    private JBPasswordField passwordField;
    private AzurePasswordFieldInput txtValue;

    public SecretCreationDialog(String title) {
        super();
        this.title = title;
        setTitle(this.title);
        $$$setupUI$$$();
        init();
    }

    @Override
    protected void init() {
        super.init();
        final ButtonGroup group = new ButtonGroup();
        group.add(rdoYes);
        group.add(rdoNo);
        this.txtName.setRequired(true);
        this.txtName.addValidator(() -> KeyVaultCredentialActions.validateCredentialName(txtName));
        this.txtValue.setRequired(true);
        this.txtContentType.setRequired(false);

        this.lblName.setIcon(AllIcons.General.ContextHelp);
        this.lblName.setLabelFor(txtName);
        this.lblValue.setIcon(AllIcons.General.ContextHelp);
        this.lblValue.setLabelFor(passwordField);
        this.lblContentType.setLabelFor(txtContentType);
    }

    @Override
    public AzureForm<SecretDraft.Config> getForm() {
        return this;
    }

    @Nonnull
    @Override
    protected String getDialogTitle() {
        return this.title;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public SecretDraft.Config getValue() {
        final SecretDraft.Config config = new SecretDraft.Config();
        config.setName(txtName.getValue());
        config.setValue(txtValue.getValue());
        config.setEnabled(rdoYes.isSelected());
        config.setContentType(txtContentType.getValue());
        return config;
    }

    @Override
    public void setValue(@Nonnull final SecretDraft.Config data) {
        Optional.ofNullable(data.getName()).ifPresent(txtName::setValue);
        Optional.ofNullable(data.getValue()).ifPresent(txtValue::setValue);
        rdoYes.setSelected(BooleanUtils.isNotFalse(data.getEnabled()));
        rdoNo.setSelected(BooleanUtils.isFalse(data.getEnabled()));
        Optional.ofNullable(data.getContentType()).ifPresent(txtContentType::setValue);
    }

    public void setFixedName(final String name) {
        txtName.setValue(name);
        txtName.setEnabled(false);
        txtName.setEditable(false);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtName, txtValue);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.passwordField = new JBPasswordField();
        this.txtValue = new AzurePasswordFieldInput(this.passwordField);
    }
}
