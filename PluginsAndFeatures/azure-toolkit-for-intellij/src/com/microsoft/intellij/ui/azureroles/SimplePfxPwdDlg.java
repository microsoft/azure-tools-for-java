/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
