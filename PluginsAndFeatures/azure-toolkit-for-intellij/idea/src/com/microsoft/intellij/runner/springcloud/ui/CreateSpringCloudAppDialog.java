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

package com.microsoft.intellij.runner.springcloud.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.intellij.common.AzureResourceWrapper;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.ValidationUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;

public class CreateSpringCloudAppDialog extends AzureDialogWrapper {
    private static final String INVALID_APP_NAME = "The name '%s' is invalid. It can contain only lowercase letters, numbers and hyphens. " +
            "The first character must be a letter. " +
            "The last character must be a letter or number. The value must be between 4 and 32 characters long.";
    private JPanel contentPane;
    private JTextField textAppName;

    private AzureResourceWrapper appWrapper;

    public AzureResourceWrapper getNewAppWrapper() {
        return appWrapper;
    }

    public CreateSpringCloudAppDialog(Project project, Component parent, String title) {
        super(project, parent, false, IdeModalityType.IDE);
        setModal(true);
        setTitle(title);
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    protected ValidationInfo doValidate() {
        if (StringUtils.isNotEmpty(this.textAppName.getText())) {
            if (!ValidationUtils.isValidSpringCloudAppName(this.textAppName.getText().trim())) {
                return new ValidationInfo(String.format(INVALID_APP_NAME, this.textAppName.getText()),
                        this.textAppName);
            }
        } else {
            return new ValidationInfo("Please input the app name.",
                    this.textAppName);
        }
        return null;

    }

    @Override
    protected void doOKAction() {
        if (StringUtils.isNotEmpty(this.textAppName.getText())) {
            appWrapper = new AzureResourceWrapper(this.textAppName.getText().trim(), false);
        }
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        appWrapper = null;
        super.doCancelAction();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return this.textAppName;
    }
}
