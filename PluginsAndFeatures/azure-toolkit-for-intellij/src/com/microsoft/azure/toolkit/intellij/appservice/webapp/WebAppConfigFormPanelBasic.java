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

package com.microsoft.azure.toolkit.intellij.appservice.webapp;

import com.microsoft.azure.toolkit.lib.appservice.Platform;
import com.microsoft.azure.toolkit.intellij.appservice.component.input.ComboBoxDeployment;
import com.microsoft.azure.toolkit.intellij.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.appservice.component.input.ComboBoxPlatform;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppConfig;

import javax.swing.*;
import java.nio.file.Path;

public class WebAppConfigFormPanelBasic extends JPanel implements AzureFormPanel<WebAppConfig> {
    private JPanel contentPanel;

    protected JTextField textName;
    protected ComboBoxPlatform selectorPlatform;
    protected ComboBoxDeployment selectorApplication;

    @Override
    public WebAppConfig getData() {
        final String name = this.textName.getText();
        final Platform platform = this.selectorPlatform.getValue();
        final Path path = this.selectorApplication.getValue();
        return WebAppConfig.builder()
                           .name(name)
                           .platform(platform)
                           .application(path)
                           .build();
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
        super.setVisible(visible);
    }
}
