/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.forms;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.intellij.feedback.GithubIssue;
import com.microsoft.intellij.feedback.ReportableError;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
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

        buttonFireIssue.addActionListener(event -> {
            new GithubIssue<>(new ReportableError(title, errorMessageDetail)).withLabel("bug").report();
        });

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