/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui.azureroles;

import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azuretools.azurecommons.util.CerPfxUtil;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class SimplePfxPwdDlg extends AzureDialogWrapper {
    private JPanel contentPane;
    private JPasswordField txtPwd;

    private String pfxPath;

    public SimplePfxPwdDlg(String path) {
        super(true);
        this.pfxPath = path;
        init();
    }

    protected void init() {
        setTitle(message("certPwd"));
        super.init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        if (CerPfxUtil.validatePfxPwd(pfxPath, new String(txtPwd.getPassword()).trim())) {
            super.doOKAction();
        } else {
            PluginUtil.displayErrorDialog(message("error"), message("invalidPfxPwdMsg"));
        }
    }

    protected ValidationInfo doValidate() {
        if (new String(txtPwd.getPassword()).trim().isEmpty()) {
            return new ValidationInfo("", txtPwd);
        }
        return null;
    }

    public String getPwd() {
        return new String(txtPwd.getPassword());
    }
}
