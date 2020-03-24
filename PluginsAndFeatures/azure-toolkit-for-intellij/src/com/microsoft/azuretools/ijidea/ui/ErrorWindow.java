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

package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
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
