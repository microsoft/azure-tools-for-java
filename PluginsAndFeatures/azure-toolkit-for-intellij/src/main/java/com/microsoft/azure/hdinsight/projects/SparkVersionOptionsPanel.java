/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.projects;

import com.intellij.openapi.ui.ComboBox;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.ItemEvent;

public class SparkVersionOptionsPanel extends JPanel {
    private static final String SPARK_VERSION_KEY = "com.microsoft.azure.hdinsight.SparkVersion";
    private ComboBox sparkVersionComboBox;

    public SparkVersion apply() {
        return (SparkVersion) sparkVersionComboBox.getSelectedItem();
    }

    public SparkVersionOptionsPanel() {
        this(SparkVersion.class.getEnumConstants());
    }

    public SparkVersionOptionsPanel(SparkVersion sparkVersions[]) {
        sparkVersionComboBox = new ComboBox();

        for (SparkVersion sv : sparkVersions) {
            sparkVersionComboBox.addItem(sv);
        }

        sparkVersionComboBox.setSelectedIndex(0);
        String cachedSparkVersion = DefaultLoader.getIdeHelper().getApplicationProperty(SPARK_VERSION_KEY);
        if (cachedSparkVersion != null) {
            useCachedSparkVersion(cachedSparkVersion);
        }

        sparkVersionComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                DefaultLoader.getIdeHelper().setApplicationProperty(SPARK_VERSION_KEY, e.getItem().toString());
            }
        });

        add(sparkVersionComboBox);

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(sparkVersionComboBox, constraints);
        setLayout(layout);

        // To fix focus invisible issue. Refer to https://github.com/microsoft/azure-tools-for-java/issues/3612
        this.setFocusable(false);
    }

    private void useCachedSparkVersion(String cachedSparkVersion) {
        for(int i = 0; i < this.sparkVersionComboBox.getModel().getSize(); i++) {
            if (this.sparkVersionComboBox.getModel().getElementAt(i).toString().equals(cachedSparkVersion)) {
                this.sparkVersionComboBox.getModel().setSelectedItem(this.sparkVersionComboBox.getModel().getElementAt(i));
            }
        }
    }
}
