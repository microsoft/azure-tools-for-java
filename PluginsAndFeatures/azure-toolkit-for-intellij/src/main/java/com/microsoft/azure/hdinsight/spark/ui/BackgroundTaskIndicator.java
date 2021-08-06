/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
