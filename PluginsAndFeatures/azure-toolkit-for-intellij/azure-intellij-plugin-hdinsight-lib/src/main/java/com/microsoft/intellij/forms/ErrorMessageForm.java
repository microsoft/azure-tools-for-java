/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.forms;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azure.toolkit.intellij.common.component.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ErrorMessageForm extends AzureDialogWrapper {
    public static final String advancedInfoText = "Show advanced info";
    private JPanel contentPane;
    private JButton buttonOK;
    private JLabel lblError;
    private JCheckBox showAdvancedInfoCheckBox;
    private JTextArea detailTextArea;
    private JScrollPane detailScroll;
    private JButton buttonFireIssue;
    private String errorMessageDetail = "";

    public ErrorMessageForm(String title) {
        super((Project) null, true);

        setModal(true);
        setTitle(title);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close(DialogWrapper.OK_EXIT_CODE, true);
            }
        });
        showAdvancedInfoCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setDetailsVisible(showAdvancedInfoCheckBox.isSelected());
            }
        });

        showAdvancedInfoCheckBox.setText(advancedInfoText);

        init();
    }

    public void showErrorMessageForm(String errorMessage, String details) {
        this.errorMessageDetail = details;

        lblError.setText("<html><p>" + (errorMessage.length() > 260 ? errorMessage.substring(0, 260) + "..." : errorMessage) + "</p></html>");
        detailTextArea.setText(details);
        showAdvancedInfoCheckBox.setEnabled(!StringHelper.isNullOrWhiteSpace(details));
        this.setResizable(false);
    }

    private void setDetailsVisible(boolean visible) {
        detailScroll.setVisible(visible);

        if (visible) {
            Dimension dimension = new Dimension(detailScroll.getMinimumSize().width, detailScroll.getMinimumSize().height + 200);
            this.detailScroll.setMinimumSize(dimension);
            this.detailScroll.setPreferredSize(dimension);
            this.detailScroll.setMaximumSize(dimension);

            this.setSize(this.getSize().width, this.getSize().height + 220);
        } else {

            Dimension dimension = new Dimension(detailScroll.getMinimumSize().width, detailScroll.getMinimumSize().height - 200);
            this.detailScroll.setMinimumSize(dimension);
            this.detailScroll.setPreferredSize(dimension);
            this.detailScroll.setMaximumSize(dimension);
            this.setSize(this.getSize().width, this.getSize().height - 220);
        }

        detailScroll.repaint();

        JViewport jv = detailScroll.getViewport();
        jv.setViewPosition(new Point(0, 0));
    }

    @Nullable
    @Override
    protected JComponent createSouthPanel() {
        return null;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
