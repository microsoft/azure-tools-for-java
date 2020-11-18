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

import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.toolkit.intellij.appservice.ProcessComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.appservice.ProcessInfo;
import com.microsoft.azure.toolkit.lib.appservice.jfr.FlightRecorderConfiguration;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RunFlightRecorderDialog extends AzureDialog<FlightRecorderConfiguration>
        implements AzureForm<FlightRecorderConfiguration> {
    private static final int MAX_DURATION_SECONDS = 600;
    private static final int DEFAULT_DURATION_SECONDS = 30;
    private JPanel contentPanel;
    private ProcessComboBox processComboBox1;
    private DurationPanel durationPanel;

    private WebAppBase appService;

    public RunFlightRecorderDialog(final Project project, WebAppBase appService) {
        super(project);
        this.appService = appService;
        this.init();
    }

    @Override
    public AzureForm<FlightRecorderConfiguration> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return null;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }

    @Override
    public FlightRecorderConfiguration getData() {
        final FlightRecorderConfiguration.FlightRecorderConfigurationBuilder builder =
                FlightRecorderConfiguration.builder();
        builder.duration(this.durationPanel.getData());
        ProcessInfo pi = this.processComboBox1.getValue();
        if (pi != null) {
            builder.pid(pi.getId());
            builder.processName(pi.getName());
        }
        return builder.build();
    }

    @Override
    public void setData(final FlightRecorderConfiguration data) {
        // no need to persist process list since it is volatile
        this.durationPanel.setData(data.getDuration());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        List<AzureFormInput<?>> res = new ArrayList<>(this.durationPanel.getInputs());
        res.addAll(Collections.singletonList(this.processComboBox1));
        return res;
    }

    private void createUIComponents() {
        this.processComboBox1 = new ProcessComboBox();
        this.processComboBox1.setAppService(this.appService);
        this.processComboBox1.refreshItems();
        this.durationPanel = new DurationPanel(1, MAX_DURATION_SECONDS, DEFAULT_DURATION_SECONDS);
    }
}
