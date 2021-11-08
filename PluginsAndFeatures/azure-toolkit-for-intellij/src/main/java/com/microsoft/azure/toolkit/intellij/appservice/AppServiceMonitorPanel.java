/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.toolkit.intellij.appservice.insights.ApplicationInsightsComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.IntegerTextField;
import com.microsoft.azure.toolkit.lib.appservice.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.appservice.MonitorConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class AppServiceMonitorPanel extends JPanel implements AzureFormPanel<MonitorConfig> {
    private JPanel pnlRoot;
    private JRadioButton rdoDisableApplicationInsights;
    private JRadioButton rdoEnableApplicationInsights;
    private JLabel lblApplicationInsights;
    private ApplicationInsightsComboBox applicationInsightsComboBox;
    private JRadioButton rdoDisableDetailError;
    private JRadioButton rdoEnableDetailError;
    private JRadioButton rdoDisableFailedRequest;
    private JRadioButton rdoEnableFailedRequest;
    private JRadioButton rdoDisableApplicationLog;
    private JRadioButton rdoEnableApplicationLog;
    private JLabel lblInsightsEnable;
    private JLabel lblWebServerLog;
    private JRadioButton rdoDisableWebServerLog;
    private JRadioButton rdoEnableWebServerLog;
    private JPanel pnlWebServerLog;
    private JLabel lblQuota;
    private JLabel lblRetention;
    private JLabel lblDetailedErrorMessage;
    private JLabel lblFailedRequest;
    private JPanel pnlApplicationLog;
    private TitledSeparator titleApplicationInsights;
    private TitledSeparator titleAppServiceLog;
    private IntegerTextField txtQuota;
    private IntegerTextField txtRetention;
    private LogLevelComboBox cbLogLevel;
    private JLabel lblApplicationLog;

    private Project project;

    public AppServiceMonitorPanel(final Project project) {
        this.project = project;
        init();
    }

    public void setApplicationInsightsVisible(boolean visible) {
        titleApplicationInsights.setVisible(visible);
        lblInsightsEnable.setVisible(visible);
        lblApplicationInsights.setVisible(visible);
        applicationInsightsComboBox.setVisible(visible);
        rdoEnableApplicationInsights.setVisible(visible);
        rdoDisableApplicationInsights.setVisible(visible);
        rdoEnableApplicationInsights.setSelected(visible);
    }

    public void setAppServiceLogVisible(boolean visible) {
        setApplicationInsightsVisible(visible);
        setWebServerLogVisible(visible);
        titleAppServiceLog.setVisible(visible);
    }

    public void setApplicationLogVisible(boolean visible) {
        lblApplicationLog.setVisible(visible);
        rdoEnableApplicationLog.setVisible(visible);
        rdoDisableApplicationLog.setVisible(visible);
        pnlApplicationLog.setVisible(visible);
        rdoEnableApplicationLog.setSelected(visible);
        titleAppServiceLog.setVisible(lblApplicationLog.isVisible() || lblWebServerLog.isVisible());
    }

    public void setWebServerLogVisible(boolean visible) {
        lblWebServerLog.setVisible(visible);
        rdoEnableWebServerLog.setVisible(visible);
        rdoDisableWebServerLog.setVisible(visible);
        pnlWebServerLog.setVisible(visible);
        rdoEnableWebServerLog.setSelected(visible);
        titleAppServiceLog.setVisible(lblApplicationLog.isVisible() || lblWebServerLog.isVisible());
    }

    @Override
    public MonitorConfig getValue() {
        final ApplicationInsightsConfig insightsConfig = (rdoEnableApplicationInsights.isSelected() && titleApplicationInsights.isVisible()) ?
                applicationInsightsComboBox.getValue() : null;
        final DiagnosticConfig diagnosticConfig = DiagnosticConfig.builder()
                .enableWebServerLogging(rdoEnableWebServerLog.isSelected() && lblWebServerLog.isVisible())
                .webServerLogQuota(txtQuota.getValue())
                .webServerRetentionPeriod(txtRetention.getValue())
                .enableDetailedErrorMessage(rdoEnableDetailError.isSelected())
                .enableFailedRequestTracing(rdoEnableFailedRequest.isSelected())
                .enableApplicationLog(rdoEnableApplicationLog.isSelected() && lblApplicationLog.isVisible())
                .applicationLogLevel(cbLogLevel.getValue()).build();
        return MonitorConfig.builder().applicationInsightsConfig(insightsConfig).diagnosticConfig(diagnosticConfig).build();
    }

    @Override
    public void setValue(final MonitorConfig data) {
        if (titleApplicationInsights.isVisible()) {
            if (data.getApplicationInsightsConfig() != null) {
                rdoEnableApplicationLog.setSelected(true);
                applicationInsightsComboBox.setValue(data.getApplicationInsightsConfig());
            } else {
                rdoDisableApplicationLog.setSelected(true);
                applicationInsightsComboBox.setSelectedItem(null);
            }
        }
        if (titleAppServiceLog.isVisible()) {
            final DiagnosticConfig diagnosticConfig = data.getDiagnosticConfig();
            if (lblWebServerLog.isVisible()) {
                rdoEnableWebServerLog.setSelected(diagnosticConfig.isEnableWebServerLogging());
                txtQuota.setValue(diagnosticConfig.getWebServerLogQuota());
                txtRetention.setValue(diagnosticConfig.getWebServerRetentionPeriod());
                rdoEnableDetailError.setSelected(diagnosticConfig.isEnableDetailedErrorMessage());
                rdoEnableFailedRequest.setSelected(diagnosticConfig.isEnableFailedRequestTracing());
            }
            if (lblApplicationInsights.isVisible()) {
                rdoEnableApplicationLog.setSelected(diagnosticConfig.isEnableApplicationLog());
                cbLogLevel.setSelectedItem(diagnosticConfig.getApplicationLogLevel());
            }
        }
        this.repaint();
    }

    public ApplicationInsightsComboBox getApplicationInsightsComboBox() {
        return applicationInsightsComboBox;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(applicationInsightsComboBox, cbLogLevel, txtQuota, txtRetention);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        cbLogLevel = new LogLevelComboBox();
        applicationInsightsComboBox = new ApplicationInsightsComboBox();

        txtQuota = new IntegerTextField();
        txtQuota.setMinValue(25);
        txtQuota.setMaxValue(100);

        txtRetention = new IntegerTextField();
        txtRetention.setMinValue(0);
        txtRetention.setMaxValue(99999);
        txtRetention.setRequired(false);
    }

    private void init() {
        final ButtonGroup insightsGroup = new ButtonGroup();
        insightsGroup.add(rdoEnableApplicationInsights);
        insightsGroup.add(rdoDisableApplicationInsights);
        rdoEnableApplicationInsights.addItemListener(e -> toggleApplicationInsights(rdoEnableApplicationInsights.isSelected()));
        rdoDisableApplicationInsights.addItemListener(e -> toggleApplicationInsights(rdoEnableApplicationInsights.isSelected()));

        final ButtonGroup webServerGroup = new ButtonGroup();
        webServerGroup.add(rdoEnableWebServerLog);
        webServerGroup.add(rdoDisableWebServerLog);
        rdoEnableWebServerLog.addItemListener(e -> toggleWebServerLog(rdoEnableWebServerLog.isSelected()));
        rdoDisableWebServerLog.addItemListener(e -> toggleWebServerLog(rdoEnableWebServerLog.isSelected()));

        final ButtonGroup detailedErrorMessageGroup = new ButtonGroup();
        detailedErrorMessageGroup.add(rdoEnableDetailError);
        detailedErrorMessageGroup.add(rdoDisableDetailError);
        final ButtonGroup failedRequestGroup = new ButtonGroup();
        failedRequestGroup.add(rdoEnableFailedRequest);
        failedRequestGroup.add(rdoDisableFailedRequest);
        final ButtonGroup applicationLogGroup = new ButtonGroup();
        applicationLogGroup.add(rdoEnableApplicationLog);
        applicationLogGroup.add(rdoDisableApplicationLog);
        rdoEnableApplicationLog.addItemListener(e -> toggleApplicationLog(rdoEnableApplicationLog.isSelected()));
        rdoDisableApplicationLog.addItemListener(e -> toggleApplicationLog(rdoEnableApplicationLog.isSelected()));
    }

    private void toggleApplicationInsights(boolean enable) {
        lblApplicationInsights.setVisible(enable);
        applicationInsightsComboBox.setVisible(enable);
        applicationInsightsComboBox.setRequired(enable);
    }

    private void toggleWebServerLog(boolean enable) {
        pnlWebServerLog.setVisible(enable);
        txtQuota.setRequired(enable);
        txtRetention.setRequired(enable);
    }

    private void toggleApplicationLog(boolean enable) {
        pnlApplicationLog.setVisible(enable);
    }
}
