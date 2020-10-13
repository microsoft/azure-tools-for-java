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

package com.microsoft.azure.toolkit.appservice.intellij.view;

import com.intellij.ui.components.JBRadioButton;
import com.microsoft.azure.toolkit.appservice.AppServiceConfig;
import com.microsoft.azure.toolkit.appservice.intellij.view.component.*;
import com.microsoft.azure.toolkit.appservice.view.component.*;
import com.microsoft.azure.toolkit.appservice.view.component.input.*;

import javax.swing.*;

public class AppServiceConfigFormPanelAdvanced<T extends AppServiceConfig> extends JPanel implements AzureFormPanel<T> {
    private JPanel contentPanel;

    protected ComboBoxSubscription selectorSubscription;

    protected JTextField textName;
    protected ComboBoxPlatform selectorRuntime;
    protected JBRadioButton radioOsLinux;
    protected JBRadioButton radioOsWindows;
    protected ComboBoxRegion selectorRegion;

    protected JLabel textSku;
    protected ComboBoxDeployment selectorApplication;
    protected ComboBoxResourceGroup selectorGroup;
    protected ComboBoxServicePlan selectorServicePlan;

    public AppServiceConfigFormPanelAdvanced() {
        super();
    }

    @Override
    public T getData() {
        return null;
    }
}
