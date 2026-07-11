/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.projects;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.ComboBox;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import org.jetbrains.plugins.scala.project.Versions$;
import org.jetbrains.sbt.SbtVersion;
import scala.collection.immutable.Seq;

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
            try {
                // Access Scala companion object directly: Versions$.MODULE$.loadSbtVersions()
                final Seq<SbtVersion> sbtVersions = Versions$.MODULE$.loadSbtVersions(false, (ProgressIndicator) null);
                final int size = sbtVersions.size();
                final String[] result = new String[size];
                for (int i = 0; i < size; i++) {
                    result[i] = sbtVersions.apply(i).toString();
                }
                versions[0] = result;
            } catch (final Exception e) {
                log().warn("Failed to get SBT versions from scala plugin.", e);
                versions[0] = new String[0];
            }
        }, null);

        this.sbtVersionComboBox.removeAllItems();
        for (String version : versions[0]) {
            this.sbtVersionComboBox.addItem(version);
        }
    }
}
