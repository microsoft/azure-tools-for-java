/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.creation.key;

import com.azure.security.keyvault.keys.models.KeyCurveName;
import com.azure.security.keyvault.keys.models.KeyType;
import com.intellij.icons.AllIcons;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.keyvault.KeyVaultCredentialActions;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.keyvault.key.KeyDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class KeyCreationDialog extends AzureDialog<KeyDraft.Config> implements AzureFormPanel<KeyDraft.Config> {
    private final String title;
    private JPanel pnlRoot;
    private JLabel lblName;
    private AzureTextInput txtName;
    private JLabel lblKeyType;
    private JRadioButton rdoRsa;
    private JRadioButton rdoEc;
    private JRadioButton rsa2048;
    private JRadioButton rsa3072;
    private JRadioButton rsa4096;
    private JRadioButton p256;
    private JLabel lblEnabled;
    private JRadioButton rdoYes;
    private JRadioButton rdoNo;
    private JRadioButton p384;
    private JRadioButton p521;
    private JRadioButton p256k;
    private JLabel lblCurveName;
    private JLabel lblRsaKeySize;
    private TitledSeparator titleInstance;
    private TitledSeparator titleSettings;

    private final ButtonGroup typeGroup = new ButtonGroup();
    private final ButtonGroup rsaGroup = new ButtonGroup();
    private final ButtonGroup ecGroup = new ButtonGroup();
    private final ButtonGroup enableGroup = new ButtonGroup();

    private static final String KEY_SIZE = "key size";
    private static final String CURVE_NAME = "curve name";

    public KeyCreationDialog(String title) {
        super();
        this.title = title;
        setTitle(this.title);
        $$$setupUI$$$();
        init();
    }

    @Override
    protected void init() {
        typeGroup.add(rdoRsa);
        typeGroup.add(rdoEc);
        txtName.setRequired(true);
        txtName.addValidator(() -> KeyVaultCredentialActions.validateCredentialName(txtName));
        lblName.setIcon(AllIcons.General.ContextHelp);
        lblKeyType.setIcon(AllIcons.General.ContextHelp);
        rdoRsa.addActionListener(ignore -> changeToRsa());
        rdoEc.addActionListener(ignore -> changeToEc());
        // rsa size
        rsaGroup.add(rsa2048);
        rsa2048.putClientProperty(KEY_SIZE, 2048);
        rsaGroup.add(rsa3072);
        rsa3072.putClientProperty(KEY_SIZE, 3072);
        rsaGroup.add(rsa4096);
        rsa4096.putClientProperty(KEY_SIZE, 4096);
        // ec curve name
        ecGroup.add(p256);
        p256.putClientProperty(CURVE_NAME, KeyCurveName.P_256);
        ecGroup.add(p384);
        p384.putClientProperty(CURVE_NAME, KeyCurveName.P_384);
        ecGroup.add(p521);
        p521.putClientProperty(CURVE_NAME, KeyCurveName.P_521);
        ecGroup.add(p256k);
        p256k.putClientProperty(CURVE_NAME, KeyCurveName.P_256K);
        // enable group
        enableGroup.add(rdoYes);
        enableGroup.add(rdoNo);
        this.lblName.setLabelFor(txtName);
        super.init();
    }

    private void changeToEc() {
        lblRsaKeySize.setVisible(false);
        Collections.list(rsaGroup.getElements()).forEach(button -> button.setVisible(false));
        lblCurveName.setVisible(true);
        Collections.list(ecGroup.getElements()).forEach(button -> button.setVisible(true));
    }

    private void changeToRsa() {
        lblRsaKeySize.setVisible(true);
        Collections.list(rsaGroup.getElements()).forEach(button -> button.setVisible(true));
        lblCurveName.setVisible(false);
        Collections.list(ecGroup.getElements()).forEach(button -> button.setVisible(false));
    }

    @Override
    public AzureForm<KeyDraft.Config> getForm() {
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
    public KeyDraft.Config getValue() {
        final KeyDraft.Config config = new KeyDraft.Config();
        config.setName(txtName.getValue());
        config.setKeyType(rdoRsa.isSelected() ? KeyType.RSA : KeyType.EC);
        if (rdoRsa.isSelected()) {
            final Integer rsaSize = Collections.list(rsaGroup.getElements()).stream()
                    .filter(AbstractButton::isSelected)
                    .map(button -> (Integer)button.getClientProperty(KEY_SIZE))
                    .findFirst().orElse(null);
            config.setRasKeySize(rsaSize);
        } else {
            final KeyCurveName keyCurveName = Collections.list(ecGroup.getElements()).stream()
                    .filter(AbstractButton::isSelected)
                    .map(button -> (KeyCurveName) button.getClientProperty(CURVE_NAME))
                    .findFirst().orElse(null);
            config.setCurveName(keyCurveName);
        }
        config.setEnabled(rdoYes.isSelected());
        return config;
    }

    @Override
    public void setValue(@Nonnull final KeyDraft.Config data) {
        txtName.setValue(data.getName());
        rdoRsa.setSelected(Objects.equals(data.getKeyType(), KeyType.RSA));
        rdoEc.setSelected(Objects.equals(data.getKeyType(), KeyType.EC));
        Collections.list(rsaGroup.getElements()).forEach(button -> button.setSelected(Objects.equals(data.getRasKeySize(), button.getClientProperty(KEY_SIZE))));
        Collections.list(ecGroup.getElements()).forEach(button -> button.setSelected(Objects.equals(data.getCurveName(), button.getClientProperty(CURVE_NAME))));
        rdoYes.setSelected(data.getEnabled());
        rdoNo.setSelected(!data.getEnabled());
    }

    public void setFixedName(final String name) {
        txtName.setValue(name);
        txtName.setEnabled(false);
        txtName.setEditable(false);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtName);
    }
}
