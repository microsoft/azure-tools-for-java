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

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appservice.component.AppServiceConfigDialog;
import com.microsoft.azure.toolkit.intellij.appservice.component.AppServiceConfigPanel;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppConfig;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class WebAppCreationDialog extends AppServiceConfigDialog<WebAppConfig> {
    public static final String TITLE_CREATE_WEBAPP_DIALOG = "Create Web App";
    private JPanel panel;
    private WebAppConfigFormPanelAdvanced advancedForm;
    private WebAppConfigFormPanelBasic basicForm;

    public WebAppCreationDialog(Project project) {
        super(project);
        this.init();
    }

    @Override
    protected void init() {
        super.init();
        this.toggleAdvancedMode(false);
    }

    @Override
    protected AppServiceConfigPanel<WebAppConfig> getAdvancedFormPanel() {
        return this.advancedForm;
    }

    @Override
    protected AppServiceConfigPanel<WebAppConfig> getBasicFormPanel() {
        return this.basicForm;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.panel;
    }

    protected String getDialogTitle() {
        return TITLE_CREATE_WEBAPP_DIALOG;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        advancedForm = new WebAppConfigFormPanelAdvanced(project);
        basicForm = new WebAppConfigFormPanelBasic(project);
    }
}
