/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.jfr;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.ProcessComboBox;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.ProcessInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.legacy.appservice.jfr.FlightRecorderConfiguration;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.LinkedList;
import java.util.List;

public class RunFlightRecorderDialog extends AzureDialog<FlightRecorderConfiguration>
        implements AzureForm<FlightRecorderConfiguration> {
    private static final int MAX_DURATION_SECONDS = 600;
    private static final int DEFAULT_DURATION_SECONDS = 30;
    private JPanel contentPanel;
    private ProcessComboBox processComboBox1;
    private DurationPanel durationPanel;

    private final AppServiceAppBase<?, ?, ?> appService;

    public RunFlightRecorderDialog(final Project project, AppServiceAppBase<?, ?, ?> appService) {
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
        return "Flight Recorder";
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }

    @Override
    public FlightRecorderConfiguration getValue() {
        final FlightRecorderConfiguration.FlightRecorderConfigurationBuilder builder =
                FlightRecorderConfiguration.builder();
        builder.duration(this.durationPanel.getValue());
        final ProcessInfo pi = this.processComboBox1.getValue();
        if (pi != null) {
            builder.pid(pi.getId());
            builder.processName(pi.getName());
        }
        return builder.build();
    }

    @Override
    public void setValue(final FlightRecorderConfiguration data) {
        // no need to persist process list since it is volatile
        this.durationPanel.setValue(data.getDuration());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final List<AzureFormInput<?>> res = new LinkedList<>(this.durationPanel.getInputs());
        res.add(0, this.processComboBox1);
        return res;
    }

    private void createUIComponents() {
        this.processComboBox1 = new ProcessComboBox();
        this.processComboBox1.setAppService(this.appService);
        this.processComboBox1.reloadItems();
        this.durationPanel = new DurationPanel(1, MAX_DURATION_SECONDS, DEFAULT_DURATION_SECONDS);
    }
}
