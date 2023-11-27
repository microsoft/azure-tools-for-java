/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsContexts;
import com.microsoft.azure.toolkit.intellij.common.AzureFormInputComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AzureTextFieldWithCopyButton extends TextFieldWithBrowseButton implements AzureFormInputComponent<String> {
    public AzureTextFieldWithCopyButton(){
        super();
        this.addActionListener(new CopyActionListener());
        this.setButtonIcon(AllIcons.General.InlineCopy);
    }

    @Override
    public String getValue() {
        return this.getText();
    }

    @Override
    public void setValue(String val) {
        this.setText(val);
    }

    @Override
    protected @NotNull @NlsContexts.Tooltip String getIconTooltip() {
        return "Copy";
    }

    @NotNull
    @Override
    protected Icon getDefaultIcon() {
        return AllIcons.General.InlineCopy;
    }

    @NotNull
    @Override
    protected Icon getHoveredIcon() {
        return AllIcons.General.InlineCopyHover;
    }

    private class CopyActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            final String text = AzureTextFieldWithCopyButton.this.getText();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        }
    }
}
