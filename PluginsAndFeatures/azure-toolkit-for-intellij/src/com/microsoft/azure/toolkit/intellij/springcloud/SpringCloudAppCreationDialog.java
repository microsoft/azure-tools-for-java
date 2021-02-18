/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.ValidationUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;

public class SpringCloudAppCreationDialog extends AzureDialogWrapper {
    private static final String INVALID_APP_NAME = "The name '%s' is invalid. It can contain only lowercase letters, numbers and hyphens. " +
            "The first character must be a letter. " +
            "The last character must be a letter or number. The value must be between 4 and 32 characters long.";
    private JPanel contentPane;
    private JTextField textAppName;

    private AzureResourceWrapper appWrapper;

    public AzureResourceWrapper getNewAppWrapper() {
        return appWrapper;
    }

    public SpringCloudAppCreationDialog(Project project, Component parent, String title) {
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
