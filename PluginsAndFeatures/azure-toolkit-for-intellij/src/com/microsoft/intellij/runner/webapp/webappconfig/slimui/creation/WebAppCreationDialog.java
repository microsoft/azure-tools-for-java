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

package com.microsoft.intellij.runner.webapp.webappconfig.slimui.creation;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.JdkModel;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.runner.webapp.webappconfig.WebAppConfiguration;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import com.microsoft.intellij.util.ValidationUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenConstants;

import javax.swing.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

public class WebAppCreationDialog extends AzureDialogWrapper implements WebAppCreationMvpView {

    private static final String DIALOG_TITLE = "Create WebApp";
    private static final String NOT_APPLICABLE = "N/A";

    public static final RuntimeStack DEFAULT_LINUX_RUNTIME = RuntimeStack.TOMCAT_9_0_JRE8;
    public static final JdkModel DEFAULT_WINDOWS_JAVAVERSION = JdkModel.JAVA_8_NEWEST;
    public static final WebAppUtils.WebContainerMod DEFAULT_WINDOWS_CONTAINER =
        WebAppUtils.WebContainerMod.Newest_Tomcat_90;
    public static final PricingTier DEFAULT_PRICINGTIER = new PricingTier("Premium", "P1V2");
    public static final Region DEFAULT_REGION = Region.EUROPE_WEST;

    private WebAppCreationViewPresenter presenter = null;

    private JPanel contentPanel;
    private JPanel pnlCreate;
    private JTextField txtWebAppName;
    private JComboBox cbSubscription;
    private JRadioButton rdoUseExistResGrp;
    private JComboBox cbExistResGrp;
    private JRadioButton rdoCreateResGrp;
    private JTextField txtNewResGrp;
    private JRadioButton rdoUseExistAppServicePlan;
    private JComboBox cbExistAppServicePlan;
    private JLabel lblLocation;
    private JLabel lblPricing;
    private JRadioButton rdoCreateAppServicePlan;
    private JTextField txtCreateAppServicePlan;
    private JComboBox cbRegion;
    private JComboBox cbPricing;
    private JLabel lblJavaVersion;
    private JComboBox cbJdkVersion;
    private JLabel lblWebContainer;
    private JComboBox cbWebContainer;
    private JRadioButton rdoLinuxOS;
    private JRadioButton rdoWindowsOS;
    private JLabel lblOS;
    private JPanel pnlNewAppServicePlan;
    private JPanel pnlExistingAppServicePlan;
    private JLabel lblMessage;
    private JLabel lblRuntimeStack;
    private JComboBox cbRuntimeStack;

    private WebAppConfiguration webAppConfiguration;
    private WebApp result = null;

    public WebAppCreationDialog(Project project, WebAppConfiguration configuration) {
        super(project, true);
        setModal(true);

        this.setTitle(DIALOG_TITLE);
        this.webAppConfiguration = configuration;
        this.presenter = new WebAppCreationViewPresenter<>();
        this.presenter.onAttachView(this);

        cbSubscription.addActionListener(e -> selectSubscription());
        cbExistAppServicePlan.addActionListener(e -> selectAppServicePlan());
        cbJdkVersion.addItemListener((itemEvent) -> loadWebContainers());
        cbPricing.addItemListener(e -> loadRegionsBySku());

        rdoCreateAppServicePlan.addActionListener(e -> toggleAppServicePlan(true));
        rdoUseExistAppServicePlan.addActionListener(e -> toggleAppServicePlan(false));

        rdoCreateResGrp.addActionListener(e -> toggleResourceGroup(true));
        rdoUseExistResGrp.addActionListener(e -> toggleResourceGroup(false));

        rdoLinuxOS.addActionListener(e -> toggleOS(false));
        rdoWindowsOS.addActionListener(e -> toggleOS(true));

        final ButtonGroup resourceGroupButtonGroup = new ButtonGroup();
        resourceGroupButtonGroup.add(rdoUseExistResGrp);
        resourceGroupButtonGroup.add(rdoCreateResGrp);

        final ButtonGroup appServicePlanButtonGroup = new ButtonGroup();
        appServicePlanButtonGroup.add(rdoUseExistAppServicePlan);
        appServicePlanButtonGroup.add(rdoCreateAppServicePlan);

        final ButtonGroup osButtonGroup = new ButtonGroup();
        osButtonGroup.add(rdoLinuxOS);
        osButtonGroup.add(rdoWindowsOS);

        // Set label for existing resource group checkbox and new resource group text field, for jaws
        JLabel labelForExistingResourceGroup = new JLabel("Resource Group");
        labelForExistingResourceGroup.setLabelFor(cbExistResGrp);
        JLabel labelForNewResourceGroup = new JLabel("Resource Group");
        labelForNewResourceGroup.setLabelFor(txtNewResGrp);

        cbSubscription.setRenderer(new ListCellRendererWrapper<Subscription>() {
            @Override
            public void customize(JList list, Subscription subscription, int
                index, boolean isSelected, boolean cellHasFocus) {
                if (subscription != null) {
                    setText(subscription.displayName());
                }
            }
        });

        cbExistResGrp.setRenderer(new ListCellRendererWrapper<ResourceGroup>() {
            @Override
            public void customize(JList list, ResourceGroup resourceGroup, int
                index, boolean isSelected, boolean cellHasFocus) {
                if (resourceGroup != null) {
                    setText(resourceGroup.name());
                }
            }
        });

        cbRegion.setRenderer(new ListCellRendererWrapper<Region>() {
            @Override
            public void customize(JList list, Region region, int
                index, boolean isSelected, boolean cellHasFocus) {
                if (region != null) {
                    setText(region.label());
                }
            }
        });

        cbExistAppServicePlan.setRenderer(new ListCellRendererWrapper<AppServicePlan>() {
            @Override
            public void customize(JList list, AppServicePlan appServicePlan, int
                index, boolean isSelected, boolean cellHasFocus) {
                if (appServicePlan != null) {
                    setText(appServicePlan.name());
                }
            }
        });

        init();
    }

    public WebApp getCreatedWebApp() {
        return this.result;
    }

    @Override
    public void fillSubscription(@NotNull List<Subscription> subscriptions) {
        fillCombobox(cbSubscription, subscriptions, null);
    }

    @Override
    public void fillResourceGroup(@NotNull List<ResourceGroup> resourceGroups) {
        fillCombobox(cbExistResGrp, resourceGroups, null);
    }

    @Override
    public void fillAppServicePlan(@NotNull List<AppServicePlan> appServicePlans) {
        cbExistAppServicePlan.removeAllItems();
        appServicePlans.stream()
            .filter(item -> Comparing.equal(item.operatingSystem(), webAppConfiguration.getOS()))
            .sorted(Comparator.comparing(AppServicePlan::name))
            .forEach((plan) -> {
                cbExistAppServicePlan.addItem(plan);
            });
        selectAppServicePlan();
        pack();
    }

    @Override
    public void fillRegion(@NotNull List<Region> regions) {
        cbRegion.removeAllItems();
        regions.stream()
            .sorted(Comparator.comparing(Region::label))
            .forEach((region) -> {
                cbRegion.addItem(region);
                if (Comparing.equal(region.name(), DEFAULT_REGION.name())) {
                    cbRegion.setSelectedItem(region);
                }
            });
        pack();
    }

    @Override
    public void fillPricingTier(@NotNull List<PricingTier> prices) {
        fillCombobox(cbPricing, prices, DEFAULT_PRICINGTIER);
    }

    @Override
    public void fillWebContainer(@NotNull List<WebAppUtils.WebContainerMod> webContainers) {
        fillCombobox(cbWebContainer, webContainers, DEFAULT_WINDOWS_CONTAINER);
    }

    @Override
    public void fillJdkVersion(@NotNull List<JdkModel> jdks) {
        fillCombobox(cbJdkVersion, jdks, DEFAULT_WINDOWS_JAVAVERSION);
    }

    @Override
    public void fillLinuxRuntime(@NotNull List<RuntimeStack> linuxRuntimes) {
        fillCombobox(cbRuntimeStack, linuxRuntimes, DEFAULT_LINUX_RUNTIME);
    }

    private void loadRegionsBySku() {
        final String subscriptionId = getValueFromComboBox(cbSubscription, Subscription::subscriptionId);
        if (StringUtils.isEmpty(subscriptionId)) {
            return;
        }
        presenter.onLoadRegion(subscriptionId, (PricingTier) cbPricing.getSelectedItem());
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPanel;
    }

    @Override
    protected void init() {
        super.init();

        final String projectName = webAppConfiguration.getProject().getName();
        final DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        final String date = df.format(new Date());
        final String defaultWebAppName = String.format("%s-%s", projectName, date);
        final String defaultNewResourceGroup = String.format("rg-webapp-%s", projectName);
        final String defaultNewServicePlanName = String.format("appsp-%s", defaultWebAppName);

        txtWebAppName.setText(defaultWebAppName);
        txtNewResGrp.setText(defaultNewResourceGroup);
        txtCreateAppServicePlan.setText(defaultNewServicePlanName);

        loadWebContainers();
        presenter.onLoadSubscription();
        presenter.onLoadPricingTier();
        presenter.onLoadJavaVersions();
        presenter.onLoadLinuxRuntimes();
    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        updateConfiguration();
        List<ValidationInfo> res = new ArrayList<>();
        final ValidationInfo info = validateAzureSubs(cbSubscription);
        if (info != null) {
            res.add(info);
            return res;
        }
        try {
            ValidationUtils.validateAppServiceName(webAppConfiguration.getSubscriptionId(),
                                                   webAppConfiguration.getWebAppName());
        } catch (IllegalArgumentException iae) {
            res.add(new ValidationInfo(iae.getMessage(), txtWebAppName));
        }
        if (StringUtils.isEmpty(webAppConfiguration.getSubscriptionId())) {
            res.add(new ValidationInfo("Please select subscription", cbSubscription));
        }
        if (StringUtils.isEmpty(webAppConfiguration.getResourceGroup())) {
            JComponent component = webAppConfiguration.isCreatingResGrp() ? txtNewResGrp : cbExistResGrp;
            res.add(new ValidationInfo("Please select resource group", component));
        }
        if (webAppConfiguration.isCreatingAppServicePlan()) {
            if (StringUtils.isEmpty(webAppConfiguration.getAppServicePlanName())) {
                res.add(new ValidationInfo("Please set app service plan name", txtCreateAppServicePlan));
            }
            if (StringUtils.isEmpty(webAppConfiguration.getPricing())) {
                res.add(new ValidationInfo("Please select app service plan pricing tier", cbPricing));
            }
            if (StringUtils.isEmpty(webAppConfiguration.getRegion())) {
                res.add(new ValidationInfo("Please select app service plan region", cbRegion));
            }
        } else {
            if (StringUtils.isEmpty(webAppConfiguration.getAppServicePlanId())) {
                res.add(new ValidationInfo("Please select app service plan", cbExistAppServicePlan));
            }
        }
        if (webAppConfiguration.getOS() == OperatingSystem.LINUX) {
            final RuntimeStack runtimeStack = webAppConfiguration.getLinuxRuntime();
            if (StringUtils.isEmpty(runtimeStack.stack()) || StringUtils.isEmpty(runtimeStack.version())) {
                res.add(new ValidationInfo("Please select Linux web container", cbRuntimeStack));
            }
        } else {
            if (webAppConfiguration.getJdkVersion() == null) {
                res.add(new ValidationInfo("Please select Java Version", cbJdkVersion));
            }
            if (StringUtils.isEmpty(webAppConfiguration.getWebContainer())) {
                res.add(new ValidationInfo("Please select web container", cbWebContainer));
            }
        }
        return res;
    }

    @Override
    protected void doOKAction() {
        createWebApp();
    }

    private void selectSubscription() {
        Subscription subscription = (Subscription) cbSubscription.getSelectedItem();
        presenter.onLoadResourceGroups(subscription.subscriptionId());
        presenter.onLoadAppServicePlan(subscription.subscriptionId());
        presenter.onLoadRegion(subscription.subscriptionId(), getValueFromComboBox(cbPricing, (PricingTier p) -> p));
    }

    private void toggleOS(boolean isWindows) {
        lblJavaVersion.setVisible(isWindows);
        cbJdkVersion.setVisible(isWindows);
        lblWebContainer.setVisible(isWindows);
        cbWebContainer.setVisible(isWindows);
        lblRuntimeStack.setVisible(!isWindows);
        cbRuntimeStack.setVisible(!isWindows);
        // Filter App Service Plan
        Subscription subscription = (Subscription) cbSubscription.getSelectedItem();
        if (subscription != null) {
            presenter.onLoadAppServicePlan(subscription.subscriptionId());
        }
        pack();
    }

    private void toggleResourceGroup(boolean isCreateNew) {
        cbExistResGrp.setVisible(!isCreateNew);
        txtNewResGrp.setVisible(isCreateNew);
        pack();
    }

    private void toggleAppServicePlan(boolean isCreateNew) {
        pnlNewAppServicePlan.setVisible(isCreateNew);
        pnlExistingAppServicePlan.setVisible(!isCreateNew);
        pack();
    }

    private void selectAppServicePlan() {
        AppServicePlan appServicePlan = (AppServicePlan) cbExistAppServicePlan.getSelectedItem();
        if (appServicePlan == null) {
            lblLocation.setText(NOT_APPLICABLE);
            lblPricing.setText(NOT_APPLICABLE);
        } else {
            lblLocation.setText(appServicePlan.regionName());
            lblPricing.setText(appServicePlan.pricingTier().toString());
        }
    }

    private void onOK() {
        createWebApp();
    }

    private static <T> void fillCombobox(JComboBox<T> comboBox, List<T> values, T defaultValue) {
        comboBox.removeAllItems();
        values.forEach(value -> comboBox.addItem(value));
        if (defaultValue != null && values.contains(defaultValue)) {
            comboBox.setSelectedItem(defaultValue);
        }
    }

    private void updateConfiguration() {
        webAppConfiguration.setWebAppName(txtWebAppName.getText());
        webAppConfiguration.setSubscriptionId(getValueFromComboBox(cbSubscription, Subscription::subscriptionId));
        // resource group
        if (rdoCreateResGrp.isSelected()) {
            webAppConfiguration.setCreatingResGrp(true);
            webAppConfiguration.setResourceGroup(txtNewResGrp.getText());
        } else {
            webAppConfiguration.setCreatingResGrp(false);
            webAppConfiguration.setResourceGroup(getValueFromComboBox(cbExistResGrp, ResourceGroup::name));
        }
        // app service plan
        if (rdoCreateAppServicePlan.isSelected()) {
            webAppConfiguration.setCreatingAppServicePlan(true);
            webAppConfiguration.setAppServicePlanName(txtCreateAppServicePlan.getText());
            webAppConfiguration.setRegion(getValueFromComboBox(cbRegion, Region::name, DEFAULT_REGION.name()));
            webAppConfiguration.setPricing(getValueFromComboBox(cbPricing, PricingTier::toString,
                                                                DEFAULT_PRICINGTIER.toString()));
        } else {
            webAppConfiguration.setCreatingAppServicePlan(false);
            webAppConfiguration.setAppServicePlanId(getValueFromComboBox(cbExistAppServicePlan, AppServicePlan::id));
        }
        // runtime
        if (rdoLinuxOS.isSelected()) {
            webAppConfiguration.setOS(OperatingSystem.LINUX);
            RuntimeStack linuxRuntime = cbRuntimeStack.getSelectedItem() == null ? null :
                                        (RuntimeStack) cbRuntimeStack.getSelectedItem();
            if (linuxRuntime != null) {
                webAppConfiguration.setStack(linuxRuntime.stack());
                webAppConfiguration.setVersion(linuxRuntime.version());
            }
        } else if (rdoWindowsOS.isSelected()) {
            webAppConfiguration.setOS(OperatingSystem.WINDOWS);
            webAppConfiguration.setJdkVersion(getValueFromComboBox(cbJdkVersion, JdkModel::getJavaVersion));
            webAppConfiguration.setWebContainer(getValueFromComboBox(cbWebContainer,
                                                                     WebAppUtils.WebContainerMod::getValue));
        }
        webAppConfiguration.setCreatingNew(true);
    }

    private void createWebApp() {
        updateConfiguration();
        ProgressManager.getInstance().run(new Task.Modal(null, "Creating New WebApp...", true) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                Map<String, String> properties = webAppConfiguration.getModel().getTelemetryProperties(null);
                EventUtil.executeWithLog(WEBAPP, CREATE_WEBAPP, properties, null, (operation) -> {
                    progressIndicator.setIndeterminate(true);
                    EventUtil.logEvent(EventType.info, operation, properties);
                    result = AzureWebAppMvpModel.getInstance().createWebApp(webAppConfiguration.getModel());
                    ApplicationManager.getApplication().invokeLater(() -> {
                        sendTelemetry(true, null);
                        if (AzureUIRefreshCore.listeners != null) {
                            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH,
                                null));
                        }
                    });
                    DefaultLoader.getIdeHelper().invokeLater(() -> WebAppCreationDialog.super.doOKAction());
                }, (ex) -> {
                        DefaultLoader.getUIHelper().showError("Create WebApp Failed : " + ex.getMessage(),
                                                              "Create WebApp Failed");
                        sendTelemetry(false, ex.getMessage());
                    });
            }
        });
    }

    private void sendTelemetry(boolean success, @Nullable String errorMsg) {
        Map<String, String> telemetryMap = new HashMap<>();
        telemetryMap.put("SubscriptionId", webAppConfiguration.getSubscriptionId());
        telemetryMap.put("CreateNewApp", String.valueOf(webAppConfiguration.isCreatingNew()));
        telemetryMap.put("CreateNewSP", String.valueOf(webAppConfiguration.isCreatingAppServicePlan()));
        telemetryMap.put("CreateNewRGP", String.valueOf(webAppConfiguration.isCreatingResGrp()));
        telemetryMap.put("FileType", MavenRunTaskUtil.getFileType(webAppConfiguration.getTargetName()));
        telemetryMap.put("Success", String.valueOf(success));
        if (!success) {
            telemetryMap.put("ErrorMsg", errorMsg);
        }
        final String deploymentType = webAppConfiguration.isDeployToSlot() ? "DeploymentSlot" : "WebApp";
        AppInsightsClient.createByType(AppInsightsClient.EventType.Action
            , deploymentType, "Deploy", telemetryMap);
    }

    private void loadWebContainers() {
        if (isJarApplication()) {
            final JdkModel jdkModel = (JdkModel) cbJdkVersion.getSelectedItem();
            this.presenter.onLoadJarWebContainer(jdkModel);
        } else {
            this.presenter.onLoadWarWebContainer();
        }
    }

    private boolean isJarApplication() {
        return MavenRunTaskUtil.getFileType(webAppConfiguration.getTargetName())
                               .equalsIgnoreCase(MavenConstants.TYPE_JAR);
    }

    private static <T, R> R getValueFromComboBox(JComboBox comboBox, Function<T, R> function) {
        return getValueFromComboBox(comboBox, function, null);
    }

    private static <T, R> R getValueFromComboBox(JComboBox comboBox, Function<T, R> function, R defaultValue) {
        final T selectedItem = (T) comboBox.getSelectedItem();
        return selectedItem == null ? defaultValue : function.apply(selectedItem);
    }
}
