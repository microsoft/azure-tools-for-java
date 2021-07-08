/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component;

import com.intellij.ui.JBIntSpinner;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.util.Hashtable;

public class AzureSlider {
    @Getter
    private JPanel contentPanel;

    private JSlider numSlider;
    private JBIntSpinner numValue;
    @Setter
    private int realMin = Integer.MIN_VALUE;

    public AzureSlider() {
        this.init();
    }

    private void init() {
        this.numSlider.addChangeListener(e -> this.setValue(this.numSlider.getValue()));
        this.numValue.addChangeListener(e -> this.setValue((Integer) this.numValue.getValue()));
    }

    public void addChangeListener(ChangeListener l) {
        this.numSlider.addChangeListener(l);
    }

    public void setMaximum(int max) {
        this.numSlider.setMaximum(max);
        this.numValue.setMax(max);
    }

    public void setMinimum(int min) {
        this.numSlider.setMinimum(min);
        this.numValue.setMin(min);
    }

    public void setMajorTickSpacing(int tick) {
        this.numSlider.setMajorTickSpacing(tick);
    }

    public void setMinorTickSpacing(int tick) {
        this.numSlider.setMinorTickSpacing(tick);
    }

    public void setValue(int value) {
        final int val = Math.max(this.realMin, value);
        this.numSlider.setValue(val);
        this.numValue.setValue(val);
    }

    public int getValue() {
        return (int) this.numValue.getValue();
    }

    public void updateLabels() {
        final int majorTickSpacing = this.numSlider.getMajorTickSpacing();
        final Hashtable<Integer, JComponent> labels = this.numSlider.createStandardLabels(majorTickSpacing);
        this.numSlider.setLabelTable(labels);
    }

    private void createUIComponents() {
        this.numValue = new JBIntSpinner(1, 0, 10);
    }

    public void setEnabled(boolean isEnabled) {
        this.numSlider.setEnabled(isEnabled);
        this.numValue.setEnabled(isEnabled);
    }
}
