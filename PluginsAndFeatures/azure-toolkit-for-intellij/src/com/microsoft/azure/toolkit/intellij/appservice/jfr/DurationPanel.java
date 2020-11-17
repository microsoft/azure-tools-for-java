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

package com.microsoft.azure.toolkit.intellij.appservice.jfr;

import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.IntegerTextField;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;

import javax.swing.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DurationPanel extends JPanel implements AzureFormPanel<Integer> {
    private JSlider slider1;
    private IntegerTextField textField1;
    private JPanel contentPanel;

    public DurationPanel(int minValue, int maxValue, int defaultValue) {
        $$$setupUI$$$();
        this.slider1.setValue(defaultValue);
        this.textField1.setText(Integer.toString(defaultValue));
        this.slider1.setMinimum(0);
        this.slider1.setMaximum(maxValue);
        this.textField1.setMinValue(minValue);
        this.textField1.setMaxValue(maxValue);
        this.textField1.setRequired(true);
        this.slider1.addChangeListener(e -> {
            int value = this.slider1.getValue();
            if (!Objects.equals(this.textField1.getValue(), value)) {
                this.textField1.setValue(value);
            }
        });
        this.textField1.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                Integer value = textField1.getValue();
                if (Objects.nonNull(value) && value.intValue() != slider1.getValue()) {
                    slider1.setValue(value.intValue());
                    textField1.setText(value.toString());
                }
            }
        });

    }

    @Override
    public Integer getData() {
        return this.textField1.getValue();
    }

    @Override
    public void setData(Integer data) {
        if (Objects.nonNull(data)) {
            this.textField1.setValue(data);
            this.slider1.setValue(data);
        }
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(textField1);
    }

    private void createUIComponents() {
        this.textField1 = new IntegerTextField();
    }
}
