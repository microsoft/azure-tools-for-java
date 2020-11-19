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

package com.microsoft.azure.toolkit.intellij.appservice;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.toolkit.intellij.appservice.insights.ApplicationInsightsComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.IntegerTextField;
import com.microsoft.azure.toolkit.lib.appservice.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.appservice.MonitorConfig;
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
    public MonitorConfig getData() {
        MonitorConfig config = MonitorConfig.builder().build();
        final ApplicationInsightsConfig insightsConfig =
                (rdoEnableApplicationInsights.isSelected() && titleAppServiceLog.isVisible()) ? applicationInsightsComboBox.getValue() : null;
        config.setApplicationInsightsConfig(insightsConfig);
        config.setEnableWebServerLogging(rdoEnableWebServerLog.isSelected() && lblWebServerLog.isVisible());
        config.setWebServerLogQuota(txtQuota.getValue());
        config.setWebServerRetentionPeriod(txtRetention.getValue());
        config.setEnableDetailedErrorMessage(rdoEnableDetailError.isSelected());
        config.setEnableFailedRequestTracing(rdoEnableFailedRequest.isSelected());
        config.setEnableApplicationLog(rdoEnableApplicationLog.isSelected() && lblApplicationLog.isVisible());
        config.setApplicationLogLevel(cbLogLevel.getValue());
        return config;
    }

    @Override
    public void setData(final MonitorConfig data) {
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
            if (lblWebServerLog.isVisible()) {
                rdoEnableWebServerLog.setSelected(data.isEnableWebServerLogging());
                txtQuota.setValue(data.getWebServerLogQuota());
                txtRetention.setValue(data.getWebServerRetentionPeriod());
                rdoEnableDetailError.setSelected(data.isEnableDetailedErrorMessage());
                rdoEnableFailedRequest.setSelected(data.isEnableFailedRequestTracing());
            }
            if (lblApplicationInsights.isVisible()) {
                rdoEnableApplicationLog.setSelected(data.isEnableApplicationLog());
                cbLogLevel.setSelectedItem(data.getApplicationLogLevel());
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
