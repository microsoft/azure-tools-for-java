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
