/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
        // TODO MERGE UPSTREAM METHOD HERE, COMMENTED FOR RIDER NOW
    }
}
