/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.projects;

import com.intellij.openapi.ui.ComboBox;

import javax.swing.JPanel;
import java.awt.*;

public class SparkVersionOptionsPanel extends JPanel {
    private ComboBox comboBox;

    public SparkVersion apply() {
        return (SparkVersion) comboBox.getSelectedItem();
    }

    public SparkVersionOptionsPanel() {
        comboBox = new ComboBox();
        comboBox.addItem(SparkVersion.SPARK_2_0_2);
        comboBox.addItem(SparkVersion.SPARK_1_5_2);
        comboBox.addItem(SparkVersion.SPARK_1_6_2);
        comboBox.addItem(SparkVersion.SPARK_1_6_3);
        comboBox.addItem(SparkVersion.SPARK_2_1_0);
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);
        add(comboBox);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(comboBox, constraints);
    }
}
