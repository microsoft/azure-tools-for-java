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

package com.microsoft.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TitlePanel;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResource;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResourceRegistry;
import com.microsoft.azuretools.azurecommons.util.WAEclipseHelperMethods;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginUtil;

import javax.swing.*;
import java.util.ArrayList;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class ApplicationInsightsAddDialog extends AzureDialogWrapper {
    private JPanel contentPane;
    private JTextField txtName;
    private JTextField txtKey;
    private Project myProject;

    public ApplicationInsightsAddDialog(Project project) {
        super(true);
        this.myProject = project;
        setTitle(message("aiErrTtl"));
        super.init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("addKeyTtl"), message("addKeyMsg"));
    }

    @Override
    protected void doOKAction() {
        boolean isValid = false;
        String key = txtKey.getText().trim();
        String name = txtName.getText().trim();
        if (key.isEmpty() || name.isEmpty()) {
            PluginUtil.displayErrorDialog(message("aiErrTtl"), message("aiEmptyMsg"));
        } else {
            int index = ApplicationInsightsResourceRegistry.getResourceIndexAsPerKey(key);
            if (index >= 0) {
                // registry already has an entry with same key. Show error
                ApplicationInsightsResource resource =
                        ApplicationInsightsResourceRegistry.getAppInsightsResrcList().get(index);
                // error message can be more descriptive by adding subscription name after resource name.
                // might be useful in the scenarios where same resource name exists in different subscriptions
                PluginUtil.displayErrorDialog(message("aiErrTtl"),
                        String.format(message("sameKeyErrMsg"), resource.getResourceName()));
            } else {
                ArrayList<String> resourceNameList = ApplicationInsightsResourceRegistry.getResourcesNames();
                if (resourceNameList.contains(name)) {
                    // registry already has entry with same name. Show error
                    PluginUtil.displayErrorDialog(message("aiErrTtl"), message("sameNameErrMsg"));
                } else {
                    // check instrumentation key is valid or not and show error if its invalid.
                    if (WAEclipseHelperMethods.isValidInstrumentationKey(key)) {
                        ApplicationInsightsResource resourceToAdd = new ApplicationInsightsResource(
                                name, key, message("unknown"), message("unknown"),
                                message("unknown"), message("unknown"), false);
                        ApplicationInsightsResourceRegistry.getAppInsightsResrcList().add(resourceToAdd);
                        AzureSettings.getSafeInstance(myProject).saveAppInsights();
                        isValid = true;
                    } else {
                        PluginUtil.displayErrorDialog(message("aiErrTtl"), message("aiKeyErrMsg"));
                    }
                }
            }
        }
        if (isValid) {
            super.doOKAction();
        }
    }
}
