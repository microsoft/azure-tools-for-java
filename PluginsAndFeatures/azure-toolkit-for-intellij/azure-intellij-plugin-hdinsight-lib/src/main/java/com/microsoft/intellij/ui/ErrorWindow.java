/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.component.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ErrorWindow extends AzureDialogWrapper {
    private JPanel contentPane;
    private JTextPane textPane;
    private Runnable okAction;

    public static void show(@Nullable Project project, String message, String title) {
        show(project, message, title, null, null);
    }

    public static void show(@Nullable Project project, String message, String title, String okButtonText, Runnable okAction){
        ErrorWindow w = new ErrorWindow(project, message, title, okButtonText, okAction);
        w.show();

    }

    protected ErrorWindow(@Nullable Project project, String message, String title){
        this(project, message, title, null, null);
    }

    protected ErrorWindow(@Nullable Project project, String message, String title, String okButtonText, Runnable okAction) {
        super(project, true, IdeModalityType.PROJECT);
        setModal(true);
        if (title != null && !title.isEmpty()) {
            setTitle(title);
        } else {
            setTitle("Error Notification");
        }
        if (okButtonText != null) {
            setOKButtonText(okButtonText);
            this.okAction = okAction;
        }
        setCancelButtonText("Close");
        textPane.setText(message);

        Font labelFont = UIManager.getFont("Label.font");
        textPane.setFont(labelFont);

        // To fix accessibility focus issue: https://github.com/microsoft/azure-tools-for-java/issues/3606
        textPane.setFocusable(false);

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{this.getCancelAction(), this.okAction != null ? this.getOKAction() : null};
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "ErrorWindow";
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        if (this.okAction != null) {
            this.okAction.run();
        }
    }
}
