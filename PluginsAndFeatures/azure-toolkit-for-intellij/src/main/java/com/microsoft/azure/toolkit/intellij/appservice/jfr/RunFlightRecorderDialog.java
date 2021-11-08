/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.jfr;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appservice.ProcessComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.appservice.jfr.FlightRecorderConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.ProcessInfo;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
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

    private IAppService appService;

    public RunFlightRecorderDialog(final Project project, IAppService appService) {
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
    public FlightRecorderConfiguration getValue() {
        final FlightRecorderConfiguration.FlightRecorderConfigurationBuilder builder =
                FlightRecorderConfiguration.builder();
        builder.duration(this.durationPanel.getValue());
        ProcessInfo pi = this.processComboBox1.getValue();
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
