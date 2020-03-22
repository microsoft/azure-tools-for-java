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
package com.microsoft.azure.hdinsight.projects;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.plugins.scala.project.Versions;

import com.microsoft.azure.hdinsight.common.logger.ILogger;

import javax.swing.*;
import java.awt.*;

public class SbtVersionOptionsPanel extends JPanel implements ILogger {
    private ComboBox sbtVersionComboBox;

    public String apply() {
        return (String) this.sbtVersionComboBox.getSelectedItem();
    }

    public SbtVersionOptionsPanel() {
        sbtVersionComboBox = new ComboBox();
        add(sbtVersionComboBox);

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(sbtVersionComboBox, constraints);
        setLayout(layout);
    }

    public void updateSbtVersions() {
        final String[][] versions = new String[1][1];
        ProgressManager.getInstance().runProcess(() -> {
            versions[0] = Versions.SBT$.MODULE$.apply().versions();
        }, null);

        for (String version : versions[0]) {
            this.sbtVersionComboBox.addItem(version);
        }
    }
}
