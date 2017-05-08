/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
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

    public static void show(@Nullable Project project, String message, String title) {
        ErrorWindow w = new ErrorWindow(project, message, title);
        w.show();
    }

    private ErrorWindow(@Nullable Project project,String message, String title) {
        super(project, true, IdeModalityType.PROJECT);
        setModal(true);
        if (title != null && !title.isEmpty()) {
            setTitle(title);
        } else {
            setTitle("Error Notification");
        }

        setCancelButtonText("Close");
        textPane.setText(message);

        Font labelFont = UIManager.getFont("Label.font");
        textPane.setFont(labelFont);

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{this.getCancelAction()};
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "ErrorWindow";
    }
}
