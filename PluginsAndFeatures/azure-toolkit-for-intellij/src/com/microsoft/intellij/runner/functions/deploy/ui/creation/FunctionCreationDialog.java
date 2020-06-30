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

package com.microsoft.intellij.runner.functions.deploy.ui.creation;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationInsightsComponent;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.intellij.runner.functions.IntelliJFunctionContext;
import com.microsoft.intellij.runner.functions.IntelliJFunctionRuntimeConfiguration;
import com.microsoft.intellij.runner.functions.component.AppServicePlanPanel;
import com.microsoft.intellij.runner.functions.component.ApplicationInsightsPanel;
import com.microsoft.intellij.runner.functions.component.ResourceGroupPanel;
import com.microsoft.intellij.runner.functions.component.SubscriptionPanel;
import com.microsoft.intellij.runner.functions.component.table.AppSettingsTable;
import com.microsoft.intellij.runner.functions.component.table.AppSettingsTableUtils;
import com.microsoft.intellij.runner.functions.library.function.CreateFunctionHandler;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.util.ValidationUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_FUNCTION_APP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.FUNCTION;

public class FunctionCreationDialog extends AzureDialogWrapper {

    private static final String DIALOG_TITLE = "Create Function App";
    private static final String AZURE_WEB_JOB_STORAGE_KEY = "AzureWebJobsStorage";
    private static final String APPINSIGHTS_INSTRUMENTATION_KEY = "APPINSIGHTS_INSTRUMENTATIONKEY";

    private JPanel contentPanel;
    private JButton buttonOK;
    private JPanel pnlCreate;
    private JTextField txtFunctionAppName;
    private JRadioButton rdoLinuxOS;
    private JRadioButton rdoWindowsOS;
    private JLabel lblOS;
    private JPanel pnlAppSettings;
    private ResourceGroupPanel resourceGroupPanel;
    private SubscriptionPanel subscriptionPanel;
    private AppServicePlanPanel appServicePlanPanel;
    private ApplicationInsightsPanel applicationInsightsPanel;
    private JRadioButton rdoDisableAI;
    private JRadioButton rdoEnableAI;
    private JPanel pnlApplicationInsightsHolder;
    private JRadioButton rdoJava8;
    private JRadioButton rdoJava11;
    private AppSettingsTable appSettingsTable;

    private IntelliJFunctionContext functionConfiguration;
    private FunctionApp result = null;
    private Project project;

    public FunctionCreationDialog(Project project) {
        super(project, true);
        this.project = project;

        setModal(true);
        setTitle(DIALOG_TITLE);
        getRootPane().setDefaultButton(buttonOK);

        this.functionConfiguration = new IntelliJFunctionContext(project);

        rdoLinuxOS.addActionListener(e -> selectOS());
        rdoWindowsOS.addActionListener(e -> selectOS());

        final ButtonGroup osButtonGroup = new ButtonGroup();
        osButtonGroup.add(rdoLinuxOS);
        osButtonGroup.add(rdoWindowsOS);

        rdoDisableAI.addActionListener(e -> toggleApplicationInsights(false));
        rdoEnableAI.addActionListener(e -> toggleApplicationInsights(true));

        final ButtonGroup insightsGroup = new ButtonGroup();
        insightsGroup.add(rdoDisableAI);
        insightsGroup.add(rdoEnableAI);

        final ButtonGroup javaVersionGroup = new ButtonGroup();
        javaVersionGroup.add(rdoJava8);
        javaVersionGroup.add(rdoJava11);

        subscriptionPanel.addItemListener(e -> {
            final String subscriptionId = subscriptionPanel.getSubscriptionId();
            if (subscriptionId != null) {
                resourceGroupPanel.loadResourceGroup(subscriptionId);
                applicationInsightsPanel.loadApplicationInsights(subscriptionId);
                appServicePlanPanel.loadAppServicePlan(subscriptionId, getSelectedOperationSystemEnum());
            }
        });

        txtFunctionAppName.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull final DocumentEvent documentEvent) {
                applicationInsightsPanel.changeDefaultApplicationInsightsName(txtFunctionAppName.getText());
            }
        });

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPanel;
    }

    public FunctionApp getCreatedWebApp() {
        return this.result;
    }

    public OperatingSystem getSelectedOperationSystemEnum() {
        return rdoWindowsOS.isSelected() ? OperatingSystem.WINDOWS : OperatingSystem.LINUX;
    }

    @Override
    public void init() {
        super.init();
        final String projectName = project.getName();
        final DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        final String date = df.format(new Date());
        final String defaultWebAppName = String.format("%s-%s", projectName, date);

        txtFunctionAppName.setText(defaultWebAppName);

        rdoWindowsOS.setSelected(true);
        subscriptionPanel.loadSubscription();
    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        applyToConfiguration();
        final List<ValidationInfo> res = new ArrayList<>();
        final ValidationInfo info = validateAzureSubs(subscriptionPanel.getComboComponent());
        if (info != null) {
            res.add(info);
            return res;
        }
        try {
            ValidationUtils.validateAppServiceName(functionConfiguration.getSubscription(),
                                                   functionConfiguration.getAppName());
        } catch (IllegalArgumentException iae) {
            res.add(new ValidationInfo(iae.getMessage(), txtFunctionAppName));
        }
        if (applicationInsightsPanel.isCreateNewInsights()) {
            try {
                ValidationUtils.validateApplicationInsightsName(applicationInsightsPanel.getNewApplicationInsightsName());
            } catch (IllegalArgumentException iae) {
                res.add(new ValidationInfo(iae.getMessage(), applicationInsightsPanel.getComboComponent()));
            }
        }
        if (StringUtils.isEmpty(functionConfiguration.getSubscription())) {
            res.add(new ValidationInfo("Please select subscription", subscriptionPanel.getComboComponent()));
        }
        if (StringUtils.isEmpty(functionConfiguration.getResourceGroup())) {
            res.add(new ValidationInfo("Please select resource group", resourceGroupPanel.getComboComponent()));
        }
        if (StringUtils.isEmpty(functionConfiguration.getAppServicePlanName())) {
            res.add(new ValidationInfo("Please select app service plan", appServicePlanPanel.getComboComponent()));
        }
        return res;
    }

    @Override
    protected void doOKAction() {
        createFunctionApp();
    }

    private void selectOS() {
        appServicePlanPanel.setOSType(getSelectedOperationSystemEnum());
    }

    private void onOK() {
        createFunctionApp();
    }

    private void applyToConfiguration() {
        functionConfiguration.setAppName(txtFunctionAppName.getText());
        functionConfiguration.setSubscription(subscriptionPanel.getSubscriptionId());
        // resource group
        functionConfiguration.setResourceGroup(resourceGroupPanel.getResourceGroupName());
        // app service plan
        if (appServicePlanPanel.isNewAppServicePlan()) {
            functionConfiguration.setAppServicePlanName(appServicePlanPanel.getAppServicePlanName());
            functionConfiguration.setRegion(appServicePlanPanel.getAppServicePlanRegion());
            functionConfiguration.setPricingTier(appServicePlanPanel.getAppServicePlanPricingTier());
        } else {
            functionConfiguration.setAppServicePlanName(appServicePlanPanel.getAppServicePlanName());
            functionConfiguration.setAppServicePlanResourceGroup(appServicePlanPanel.getAppServicePlanResourceGroup());
        }
        // runtime
        final IntelliJFunctionRuntimeConfiguration runtimeConfiguration = new IntelliJFunctionRuntimeConfiguration();
        runtimeConfiguration.setOs(getSelectedOperationSystemEnum() == OperatingSystem.WINDOWS ? "windows" : "linux");
        runtimeConfiguration.setJavaVersion(rdoJava8.isSelected() ? "8" : "11");
        functionConfiguration.setRuntime(runtimeConfiguration);

        functionConfiguration.setAppSettings(getFixedAppSettings());
    }

    private Map<String, String> getFixedAppSettings() {
        final Map<String, String> appSettings = appSettingsTable.getAppSettings();
        // SDK will not create storage if user specify `AZURE_WEB_JOB_STORAGE_KEY` in app settings
        // So remove the empty `AZURE_WEB_JOB_STORAGE_KEY` which is the default values in local.settings.json
        if (appSettings.containsKey(AZURE_WEB_JOB_STORAGE_KEY) && StringUtils.isEmpty(appSettings.get(AZURE_WEB_JOB_STORAGE_KEY))) {
            appSettings.remove(AZURE_WEB_JOB_STORAGE_KEY);
        }
        return appSettings;
    }

    private void toggleApplicationInsights(boolean enable) {
        pnlApplicationInsightsHolder.setVisible(enable);
        pack();
    }

    private boolean isApplicationInsightsEnabled() {
        return rdoEnableAI.isSelected();
    }

    private boolean isCreateApplicationInsights() {
        return applicationInsightsPanel.isCreateNewInsights();
    }

    private void createFunctionApp() {
        ProgressManager.getInstance().run(new Task.Modal(null, "Creating New Function App...", true) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                final Map<String, String> properties = functionConfiguration.getTelemetryProperties(null);
                EventUtil.executeWithLog(FUNCTION, CREATE_FUNCTION_APP, properties, null, (operation) -> {
                    progressIndicator.setIndeterminate(true);
                    EventUtil.logEvent(EventType.info, operation, properties);
                    bindingApplicationInsights(functionConfiguration);
                    final CreateFunctionHandler createFunctionHandler = new CreateFunctionHandler(functionConfiguration);
                    createFunctionHandler.execute();
                    result = AuthMethodManager.getInstance().getAzureClient(functionConfiguration.getSubscription()).appServices().functionApps()
                            .getByResourceGroup(functionConfiguration.getResourceGroup(), functionConfiguration.getAppName());
                    ApplicationManager.getApplication().invokeLater(() -> {
                        sendTelemetry(true, null);
                        if (AzureUIRefreshCore.listeners != null) {
                            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH,
                                    null));
                        }
                    });
                    DefaultLoader.getIdeHelper().invokeLater(() -> FunctionCreationDialog.super.doOKAction());
                }, (ex) -> {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            PluginUtil.displayErrorDialog("Create Function App Failed",
                                                          "Create Function Failed : " + ex.getMessage());
                            sendTelemetry(false, ex.getMessage());
                        });
                    });
            }
        });
    }

    private void bindingApplicationInsights(IntelliJFunctionContext functionConfiguration) throws IOException {
        if (!isApplicationInsightsEnabled()) {
            return;
        }
        String instrumentationKey = applicationInsightsPanel.getApplicationInsightsInstrumentKey();
        if (isCreateApplicationInsights()) {
            final String region = appServicePlanPanel.getAppServicePlanRegion();
            final String insightsName = applicationInsightsPanel.getNewApplicationInsightsName();
            final ApplicationInsightsComponent insights =
                    AzureSDKManager.getOrCreateApplicationInsights(functionConfiguration.getSubscription(),
                                                                   functionConfiguration.getResourceGroup(),
                                                                   insightsName,
                                                                   region);
            instrumentationKey = insights.instrumentationKey();
        }
        functionConfiguration.getAppSettings().put(APPINSIGHTS_INSTRUMENTATION_KEY, instrumentationKey);
    }

    private void sendTelemetry(boolean success, @Nullable String errorMsg) {
        final Map<String, String> telemetryMap = new HashMap<>();
        telemetryMap.put("SubscriptionId", functionConfiguration.getSubscription());
        telemetryMap.put("CreateNewApp", String.valueOf(true));
        telemetryMap.put("CreateNewSP", String.valueOf(appServicePlanPanel.isNewAppServicePlan()));
        telemetryMap.put("CreateNewRGP", String.valueOf(resourceGroupPanel.isNewResourceGroup()));
        telemetryMap.put("Success", String.valueOf(success));
        telemetryMap.put("EnableApplicationInsights", String.valueOf(isApplicationInsightsEnabled()));
        telemetryMap.put("CreateNewApplicationInsights", String.valueOf(isCreateApplicationInsights()));
        if (!success) {
            telemetryMap.put("ErrorMsg", errorMsg);
        }
        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, "Function", "Deploy", telemetryMap);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        final String localSettingPath = Paths.get(project.getBasePath(), "local.settings.json").toString();
        appSettingsTable = new AppSettingsTable(localSettingPath);
        pnlAppSettings = AppSettingsTableUtils.createAppSettingPanel(appSettingsTable);
        appSettingsTable.loadLocalSetting();

        appServicePlanPanel = new AppServicePlanPanel();
        resourceGroupPanel = new ResourceGroupPanel();
        subscriptionPanel = new SubscriptionPanel();
        applicationInsightsPanel = new ApplicationInsightsPanel();
    }
}
