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

package com.microsoft.azure.hdinsight.spark.ui;

import com.intellij.util.ui.AsyncProcessIcon;

import javax.swing.*;

public class BackgroundTaskIndicator extends JPanel{
    private AsyncProcessIcon inProcessIcon;
    private JTextField textField;
    private String runningText;

    public BackgroundTaskIndicator(String runningText) {
        this.runningText = runningText;

        this.textField = new JTextField();
        this.textField.setEnabled(false);
        this.textField.setBorder(BorderFactory.createEmptyBorder());
        this.inProcessIcon = new AsyncProcessIcon(runningText + "-icon");
        this.inProcessIcon.setVisible(false);

        add(inProcessIcon);
        add(textField);
    }

    public void stop(String stopText) {
        this.inProcessIcon.setVisible(false);
        this.textField.setText(stopText);
    }

    public void start() {
        this.inProcessIcon.setVisible(true);
        this.textField.setText(runningText);
    }

    public String getText() {
        return textField.getText();
    }

    public void setTextAndStatus(String text, boolean isRunning) {
        this.inProcessIcon.setVisible(isRunning);
        this.textField.setText(text);
    }
}
