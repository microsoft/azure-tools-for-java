/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function;

import com.intellij.openapi.project.Project;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceLogsPanel;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.ApplicationInsightsPanel;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.insights.ApplicationInsightsComboBox;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FunctionAppAdvancedConfigPanel extends JPanel implements AzureFormPanel<FunctionAppConfig> {
    private final Project project;
    private JTabbedPane tabPane;
    private JPanel pnlRoot;
    private FunctionAppInfoPanel appServiceConfigPanelAdvanced;
    private JPanel pnlMonitoring;
    private JPanel pnlAppService;
    private TitledSeparator titleApplicationInsights;
    private ApplicationInsightsPanel applicationInsightsPanel;
    private TitledSeparator titleAppServiceLog;
    private AppServiceLogsPanel appServiceLogsPanel;

    public FunctionAppAdvancedConfigPanel(final Project project) {
        super();
        this.project = project;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    @Override
    public void setVisible(final boolean visible) {
        pnlRoot.setVisible(visible);
    }

    @Override
    public FunctionAppConfig getValue() {
        final FunctionAppConfig data = appServiceConfigPanelAdvanced.getValue();
        data.applicationInsightsConfig(applicationInsightsPanel.getValue());
        data.diagnosticConfig(appServiceLogsPanel.getValue());
        return data;
    }

    @Override
    public void setValue(final FunctionAppConfig data) {
        appServiceConfigPanelAdvanced.setValue(data);
        Optional.ofNullable(data.diagnosticConfig()).ifPresent(appServiceLogsPanel::setValue);
        Optional.ofNullable(data.applicationInsightsConfig()).ifPresent(applicationInsightsPanel::setValue);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Stream.of(appServiceConfigPanelAdvanced.getInputs(), applicationInsightsPanel.getInputs(), appServiceLogsPanel.getInputs())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private void init() {
        appServiceLogsPanel.setWebServerLogVisible(false);
        appServiceLogsPanel.setApplicationLogVisible(true);

        appServiceConfigPanelAdvanced.getSelectorSubscription().addActionListener(event ->
                applicationInsightsPanel.getApplicationInsightsComboBox().setSubscription(appServiceConfigPanelAdvanced.getSelectorSubscription().getValue()));

        appServiceConfigPanelAdvanced.getSelectorRuntime().addValueChangedListener(runtime -> {
            final OperatingSystem operatingSystem = Optional.ofNullable(runtime).map(Runtime::getOperatingSystem).orElse(OperatingSystem.WINDOWS);
            titleAppServiceLog.setVisible(operatingSystem == OperatingSystem.WINDOWS);
            appServiceLogsPanel.setVisible(operatingSystem == OperatingSystem.WINDOWS);
        });

        appServiceConfigPanelAdvanced.getSelectorRegion().addItemListener(event ->
                applicationInsightsPanel.getApplicationInsightsComboBox().setRegion(appServiceConfigPanelAdvanced.getValue().region()));

        appServiceConfigPanelAdvanced.getTextName().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@Nonnull final DocumentEvent documentEvent) {
                // ai name pattern is the subset of function name pattern, so no need to validate the ai instance name

                final ApplicationInsightsComboBox insightsComboBox = applicationInsightsPanel.getApplicationInsightsComboBox();
                final ApplicationInsightsConfig value = insightsComboBox.getValue();
                if (Objects.isNull(value)) {
                    final ApplicationInsightsConfig config = ApplicationInsightsConfig.builder().createNewInstance(true)
                            .name(appServiceConfigPanelAdvanced.getTextName().getValue())
                            .build();
                    insightsComboBox.setValue(config);
                } else if (BooleanUtils.isTrue(value.getCreateNewInstance())) {
                    value.setName(appServiceConfigPanelAdvanced.getTextName().getValue());
                }
                insightsComboBox.reloadItems();
            }
        });
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        appServiceConfigPanelAdvanced = new FunctionAppInfoPanel(project);
    }
}
