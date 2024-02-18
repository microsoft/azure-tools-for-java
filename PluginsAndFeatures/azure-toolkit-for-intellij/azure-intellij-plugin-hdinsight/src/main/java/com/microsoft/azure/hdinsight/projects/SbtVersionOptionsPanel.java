/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.projects;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.ComboBox;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import org.jetbrains.plugins.scala.project.Versions;
import scala.reflect.ClassTag;

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
            versions[0] = (String[]) Versions.SBT$.MODULE$.loadVersionsWithProgress(null).versions().toArray(ClassTag.apply(String.class));
        }, null);

        for (String version : versions[0]) {
            this.sbtVersionComboBox.addItem(version);
        }
    }
}
