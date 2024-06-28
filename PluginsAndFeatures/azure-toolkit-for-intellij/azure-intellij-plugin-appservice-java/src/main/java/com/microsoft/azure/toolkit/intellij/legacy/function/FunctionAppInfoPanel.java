/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.appservice.DockerUtils;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.containerapps.component.AzureContainerAppsEnvironmentComboBox;
import com.microsoft.azure.toolkit.intellij.containerapps.component.ImageForm;
import com.microsoft.azure.toolkit.intellij.containerapps.component.WorkloadProfileComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppNameInput;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.platform.RuntimeComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.serviceplan.ServicePlanComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.*;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentDraft;
import com.microsoft.azure.toolkit.lib.containerapps.model.EnvironmentType;
import com.microsoft.azure.toolkit.lib.containerapps.model.ResourceConfiguration;
import com.microsoft.azure.toolkit.lib.containerapps.model.WorkloadProfile;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class FunctionAppInfoPanel extends JPanel implements AzureFormPanel<FunctionAppConfig> {
    public static final ContainerAppDraft.ImageConfig QUICK_START_IMAGE =
            new ContainerAppDraft.ImageConfig("mcr.microsoft.com/azure-functions/dotnet7-quickstart-demo:1.0");

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmss");
    private static final String NOT_APPLICABLE = "N/A";
    private final Project project;
    private JPanel contentPanel;
    private SubscriptionComboBox selectorSubscription;
    private ResourceGroupComboBox selectorGroup;
    private AppNameInput textName;
    private RuntimeComboBox selectorRuntime;
    private RegionComboBox selectorRegion;
    private JLabel textSku;
    private ServicePlanComboBox selectorServicePlan;
    private TitledSeparator imageTitle;
    private JLabel lblSubscription;
    private JLabel lblResourceGroup;
    private JLabel lblName;
    private JLabel lblPlatform;
    private JLabel lblRegion;
    private JLabel lblAppServicePlan;
    private JLabel lblSku;
    private JRadioButton rdoServicePlan;
    private JRadioButton rdoContainerAppsEnvironment;
    private TitledSeparator titleEnvironment;
    private JLabel lblEnvironment;
    private AzureContainerAppsEnvironmentComboBox cbEnvironment;
    private JLabel lblHostingOptions;
    private JPanel pnlHostingOptions;
    private TitledSeparator titleServicePlan;
    private ImageForm pnlContainer;
    private JCheckBox chkUseQuickStart;
    private JLabel lblQuickStart;
    private JPanel pnlContainerAppsEnvironment;
    private JPanel pnlAppServicePlan;
    private JPanel pnlImageContainer;
    private JPanel pnlImage;
    private TitledSeparator titleConsumption;
    private FlexConsumptionConfigurationPanel flexConfigurationPanel;
    private JPanel pnlFlexConsumption;
    private WorkloadProfileComboBox cbWorkloadProfile;
    private JSpinner spinnerCpu;
    private AzureComboBox<ResourceConfiguration> cbCpuAndMemory;
    private JSpinner spinnerMemory;
    private JLabel lblMemory;
    private JLabel lblCpu;
    private JLabel lblProfile;
    private JLabel lblCpuAndMemory;
    private JPanel pnlWorkloadProfile;
    private JPanel pnlProfile;
    private JPanel pnlConsumption;

    private FunctionAppConfig config;

    public FunctionAppInfoPanel(final Project project) {
        super();
        this.project = project;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    @Override
    @Nonnull
    public FunctionAppConfig getValue() {
        this.config = this.config == null ? FunctionAppConfig.builder().build() : this.config;
        config.appName(this.textName.getValue());
        config.setRegion(this.selectorRegion.getValue());
        Optional.ofNullable(this.selectorSubscription.getValue()).map(Subscription::getId).ifPresent(config::subscriptionId);
        Optional.ofNullable(this.selectorGroup.getValue()).map(ResourceGroup::getResourceGroupName).ifPresent(config::resourceGroup);
        Optional.ofNullable(this.selectorRuntime.getValue()).map(RuntimeConfig::fromRuntime).ifPresent(config::runtime);
        if (rdoServicePlan.isSelected()) {
            Optional.ofNullable(this.selectorServicePlan.getValue()).ifPresent(plan -> {
                config.servicePlanName(plan.getName());
                config.servicePlanResourceGroup(plan.isDraftForCreating() ? config.getResourceGroup() : plan.getResourceGroupName());
                final PricingTier pricingTier = plan.getPricingTier();
                config.pricingTier(pricingTier);
                if (Objects.nonNull(pricingTier) && pricingTier.isFlexConsumption()) {
                    config.setFlexConsumptionConfiguration(flexConfigurationPanel.getValue());
                }
            });
        } else if (rdoContainerAppsEnvironment.isSelected()) {
            config.diagnosticConfig(null);
            Optional.ofNullable(cbEnvironment.getValue()).ifPresent(e -> {
                if (e instanceof ContainerAppsEnvironmentDraft draft && draft.isDraftForCreating()) {
                    config.environmentConfig(draft.getConfig());
                } else {
                    config.environment(e.getName());
                }
            });
            if (pnlWorkloadProfile.isVisible()) {
                final ContainerAppFunctionConfiguration containerConfiguration = new ContainerAppFunctionConfiguration();
                Optional.ofNullable(cbWorkloadProfile.getValue()).ifPresent(profile -> containerConfiguration.setWorkloadProfileMame(profile.getName()));
                if (pnlProfile.isVisible()) {
                    Optional.ofNullable(getSpinnerValue(spinnerCpu)).ifPresent(containerConfiguration::setCpu);
                    Optional.ofNullable(getSpinnerValue(spinnerMemory)).ifPresent(value -> containerConfiguration.setMemory(value + "Gi"));
                } else {
                    Optional.ofNullable(cbCpuAndMemory.getValue()).ifPresent(configuration -> {
                        containerConfiguration.setCpu(configuration.getCpu());
                        containerConfiguration.setMemory(configuration.getMemory());
                    });
                }
                config.containerConfiguration(containerConfiguration);
            }
        }
        final Boolean isDocker = Optional.ofNullable(selectorRuntime.getValue()).map(Runtime::isDocker).orElse(false);
        if (isDocker) {
            final ContainerAppDraft.ImageConfig image = chkUseQuickStart.isSelected() ? QUICK_START_IMAGE : pnlContainer.getValue();
            Optional.ofNullable(image).map(DockerUtils::convertImageConfigToRuntimeConfig).ifPresent(config::runtime);
        }
        return this.config;
    }

    @Override
    public void setValue(final FunctionAppConfig config) {
        this.config = config;
        this.selectorSubscription.setValue(s -> StringUtils.equalsIgnoreCase(config.subscriptionId(), s.getId()));
        this.textName.setValue(config.appName());
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            Optional.ofNullable(AppServiceConfig.getResourceGroup(config)).ifPresent(group -> {
                this.selectorGroup.setValue(group);
                this.cbEnvironment.setResourceGroup(group);
            });
            Optional.ofNullable(config.getRuntime()).map(RuntimeConfig::toFunctionAppRuntime).ifPresent(this.selectorRuntime::setValue);
            this.selectorRegion.setValue(config.getRegion());
            this.cbEnvironment.setRegion(config.getRegion());
            final boolean useEnvironment = StringUtils.isNotEmpty(config.environment());
            this.rdoContainerAppsEnvironment.setSelected(useEnvironment);
            this.rdoServicePlan.setSelected(!useEnvironment);
            toggleHostingConfiguration(!useEnvironment);
            Optional.ofNullable(AppServiceConfig.getServicePlanConfig(config)).filter(ignore -> !useEnvironment)
                    .map(AppServicePlanConfig::getAppServicePlan)
                    .ifPresent(selectorServicePlan::setValue);
            Optional.ofNullable(config.environment()).filter(ignore -> useEnvironment)
                    .ifPresent(env -> cbEnvironment.setValue(r -> StringUtils.equalsIgnoreCase(r.getName(), env)));
            Optional.ofNullable(config.containerConfiguration()).ifPresent(rc -> {
                Optional.ofNullable(rc.getCpu()).ifPresent(spinnerCpu::setValue);
                Optional.ofNullable(rc.getMemory()).map(m -> StringUtils.removeEndIgnoreCase(m, "Gi")).map(Double::valueOf).ifPresent(spinnerMemory::setValue);
            });
            Optional.ofNullable(config.getFlexConsumptionConfiguration()).ifPresent(flexConfigurationPanel::setValue);
            final ContainerAppDraft.ImageConfig imageConfig = DockerUtils.convertRuntimeConfigToImageConfig(config.getRuntime());
            final boolean useDefaultImage = Objects.isNull(imageConfig) || StringUtils.equalsIgnoreCase(QUICK_START_IMAGE.getFullImageName(), imageConfig.getFullImageName());
            chkUseQuickStart.setSelected(useDefaultImage);
            toggleImageType(useDefaultImage);
            Optional.ofNullable(imageConfig).ifPresent(pnlContainer::setValue);
        });
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.textName,
            this.selectorSubscription,
            this.selectorGroup,
            this.selectorRuntime,
            this.selectorRegion,
            this.selectorServicePlan,
            this.cbEnvironment,
            this.flexConfigurationPanel
        };
        return Arrays.asList(inputs);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
        super.setVisible(visible);
    }

    private void init() {
        final ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(this.rdoServicePlan);
        buttonGroup.add(this.rdoContainerAppsEnvironment);
        rdoServicePlan.addItemListener(ignore -> toggleHostingConfiguration(true));
        rdoContainerAppsEnvironment.addItemListener(ignore -> toggleHostingConfiguration(false));

        lblQuickStart.setLabelFor(chkUseQuickStart);
        chkUseQuickStart.addItemListener(ignore -> toggleImageType(chkUseQuickStart.isSelected()));

        final String date = DATE_FORMAT.format(new Date());
        final String defaultWebAppName = String.format("app-%s-%s", this.project.getName(), date);
        this.textName.setValue(defaultWebAppName);
        this.textSku.setBorder(JBUI.Borders.emptyLeft(5));
        this.textSku.setText(NOT_APPLICABLE);
        this.selectorServicePlan.addItemListener(this::onServicePlanChanged);
        this.selectorSubscription.addItemListener(this::onSubscriptionChanged);
        this.selectorRuntime.addItemListener(this::onRuntimeChanged);
        this.selectorRegion.addItemListener(this::onRegionChanged);
        this.selectorGroup.addItemListener(this::onGroupChanged);
        this.selectorGroup.setUsePreferredSizeAsMinimum(false);

        this.selectorRegion.setItemsLoader(this::getValidRegions);
        this.selectorRuntime.setItemsLoader(this::getValidRuntimes);
        this.selectorServicePlan.setValidPricingTierList(new ArrayList<>(PricingTier.FUNCTION_PRICING), PricingTier.CONSUMPTION);

        this.textName.setRequired(true);
        this.selectorServicePlan.setRequired(true);
        this.selectorSubscription.setRequired(true);
        this.selectorGroup.setRequired(true);
        this.selectorRuntime.setRequired(true);
        this.selectorRegion.setRequired(true);

        this.lblSubscription.setLabelFor(selectorSubscription);
        this.lblResourceGroup.setLabelFor(selectorGroup);
        this.lblName.setLabelFor(textName);
        this.lblPlatform.setLabelFor(selectorRuntime);
        this.lblRegion.setLabelFor(selectorRegion);
        this.lblAppServicePlan.setLabelFor(selectorServicePlan);
        
        this.cbEnvironment.addItemListener(this::onEnvironmentChanged);
        this.cbWorkloadProfile.addItemListener(this::onWorkloadProfileChanged);

        this.lblSubscription.setIcon(AllIcons.General.ContextHelp);
        this.lblResourceGroup.setIcon(AllIcons.General.ContextHelp);
        this.lblAppServicePlan.setIcon(AllIcons.General.ContextHelp);
    }

    private void onWorkloadProfileChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final WorkloadProfile profile = (WorkloadProfile) e.getItem();
            if (Objects.isNull(profile)) {
                this.pnlProfile.setVisible(false);
                this.pnlConsumption.setVisible(false);
                return;
            }
            final boolean isConsumption = StringUtils.equalsIgnoreCase(profile.getWorkloadProfileType(), WorkloadProfile.CONSUMPTION);
            this.pnlProfile.setVisible(!isConsumption);
            this.pnlConsumption.setVisible(isConsumption);
        }
    }

    private void onEnvironmentChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final ContainerAppsEnvironment environment = (ContainerAppsEnvironment) e.getItem();
            if (Objects.isNull(environment)) {
                this.cbWorkloadProfile.setEnvironment(null);
                this.pnlWorkloadProfile.setVisible(false);
                return;
            }
            final boolean isWorkloadProfile = environment.getEnvironmentType() == EnvironmentType.WorkloadProfiles;
            this.cbWorkloadProfile.setEnvironment(isWorkloadProfile ? environment : null);
            this.pnlWorkloadProfile.setVisible(isWorkloadProfile);
        }
    }

    private List<? extends Region> getValidRegions() {
        final Subscription subscription = selectorSubscription.getValue();
        final PricingTier pricingTier = Optional.ofNullable(selectorServicePlan.getValue()).map(AppServicePlan::getPricingTier).orElse(null);
        if (Objects.isNull(subscription)) {
            return Collections.emptyList();
        } else if (Objects.isNull(pricingTier)) {
            return Azure.az(AzureAccount.class).listRegions(subscription.getId());
        } else {
            return Azure.az(AzureAppService.class).forSubscription(subscription.getId()).functionApps().listRegions(pricingTier);
        }
    }

    private List<? extends Runtime> getValidRuntimes() {
        final Subscription subscription = selectorSubscription.getValue();
        final Region region = selectorRegion.getValue();
        if (Objects.isNull(subscription) || rdoContainerAppsEnvironment.isSelected() || !isFlexConsumptionApp()) {
            return FunctionAppRuntime.getMajorRuntimes();
        } else {
            return Objects.isNull(region) ? Arrays.asList(FunctionAppLinuxRuntime.FUNCTION_JAVA17, FunctionAppLinuxRuntime.FUNCTION_JAVA11) :
                    Azure.az(AzureAppService.class).forSubscription(subscription.getId()).functionApps().listFlexConsumptionRuntimes(region);
        }
    }

    private void toggleImageType(final boolean useQuickStart){
        pnlContainer.setVisible(!useQuickStart);
    }

    private void toggleHostingConfiguration(final boolean useServicePlan) {
        this.titleEnvironment.setVisible(!useServicePlan);
        this.pnlContainerAppsEnvironment.setVisible(!useServicePlan);
        this.titleServicePlan.setVisible(useServicePlan);
        this.pnlAppServicePlan.setVisible(useServicePlan);
        this.pnlFlexConsumption.setVisible(useServicePlan && isFlexConsumptionApp());

        this.selectorServicePlan.setRequired(useServicePlan);
        this.selectorServicePlan.revalidate();
        this.cbEnvironment.setRequired(!useServicePlan);
        this.cbEnvironment.revalidate();
        if (!useServicePlan) {
            this.selectorRuntime.setValue(FunctionAppDockerRuntime.INSTANCE);
        }
        if (useServicePlan && isFlexConsumptionApp()) {
            // as flex did not support docker runtime, clear it when switch back from environment host
            this.selectorRuntime.clear();
        }
        this.selectorRuntime.reloadItems();
    }

    private void onGroupChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final ResourceGroup item = (ResourceGroup) e.getItem();
            this.selectorServicePlan.setResourceGroup(item);
            this.cbEnvironment.setResourceGroupWithDraftItems(item);
        }
    }

    private void onRegionChanged(final ItemEvent e) {
        try {
            if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
                final Region region = (Region) e.getItem();
                this.selectorServicePlan.setRegion(region);
                this.cbEnvironment.setRegionWithDraftItem(region);
            }
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }

    private void onRuntimeChanged(final ItemEvent e) {
        final Runtime runtime = (Runtime) e.getItem();
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final OperatingSystem operatingSystem = Objects.isNull(runtime) ? null :
                    // Docker runtime use Linux service plan too
                    runtime.isWindows() ? OperatingSystem.WINDOWS : OperatingSystem.LINUX;
            this.selectorServicePlan.setOperatingSystem(operatingSystem);

            final boolean isDocker = Optional.ofNullable(runtime).map(Runtime::isDocker).orElse(false);
            this.imageTitle.setVisible(isDocker);
            this.pnlImage.setVisible(isDocker);
        } else if (e.getStateChange() == ItemEvent.DESELECTED && runtime.isDocker()) {
            // hide image panel when docker runtime is deselected
            // as sometime runtime may select null, this case could not be handled by logic in ItemEvent.SELECTED
            this.imageTitle.setVisible(false);
            this.pnlImage.setVisible(false);
        }
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        //TODO: @wangmi try subscription mechanism? e.g. this.selectorGroup.subscribe(this.selectSubscription)
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription = (Subscription) e.getItem();
            this.selectorGroup.setSubscription(subscription);
            this.textName.setSubscription(subscription);
            this.selectorRegion.setSubscription(subscription);
            this.selectorServicePlan.setSubscription(subscription);
            this.cbEnvironment.setSubscription(subscription);
        }
    }

    private void onServicePlanChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final AppServicePlan plan = (AppServicePlan) e.getItem();
            if (plan == null || plan.getPricingTier() == null) {
                return;
            }
            final PricingTier pricingTier = plan.getPricingTier();
            this.textSku.setText(pricingTier.toString());
            this.pnlFlexConsumption.setVisible(pricingTier.isFlexConsumption());
            if (pricingTier.isFlexConsumption()) {
                this.selectorRuntime.clear();
                this.selectorRegion.clear();
            }
            this.selectorRuntime.reloadItems();
            this.selectorRegion.reloadItems();
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.textSku.setText(NOT_APPLICABLE);
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.pnlImageContainer = new JPanel(new GridLayoutManager(1, 1));
        this.pnlContainer = new ImageForm();
        this.pnlImageContainer.add(this.pnlContainer.getContentPanel(), new GridConstraints(0, 0, 1, 1, 0,
                GridConstraints.FILL_BOTH, 7, 7, null, null, null, 0));

        this.cbWorkloadProfile = new WorkloadProfileComboBox();
        this.cbCpuAndMemory = new AzureComboBox<>(()-> ResourceConfiguration.CONSUMPTION_CONFIGURATIONS) {
            @Override
            protected String getItemText(Object item) {
                return item instanceof ResourceConfiguration configuration ?
                        String.format("%s CPU cores, %s Gi memory", configuration.getCpu(), configuration.getMemory()) : super.getItemText(item);
            }
        };
    }

    private boolean isFlexConsumptionApp() {
        if (rdoServicePlan.isSelected()) {
            final AppServicePlan value = selectorServicePlan.getValue();
            return Optional.ofNullable(value).map(AppServicePlan::getPricingTier).map(PricingTier::isFlexConsumption).orElse(false);
        } else {
            return false;
        }
    }

    @Nullable
    private Double getSpinnerValue(JSpinner spinner) {
        try {
            return spinner.getValue() instanceof Integer ? Double.valueOf((Integer) spinner.getValue()) : (Double) spinner.getValue();
        } catch (final RuntimeException e) {
            return null;
        }
    }

    @Override
    public List<AzureValidationInfo> validateAdditionalInfo() {
        final String subsId = Optional.ofNullable(selectorSubscription.getValue()).map(Subscription::getId).orElse(StringUtils.EMPTY);
        if (StringUtils.isNotBlank(subsId) && isFlexConsumptionApp()) {
            final List<Region> validRegions = Azure.az(AzureAppService.class).forSubscription(subsId)
                    .functionApps().listRegions(PricingTier.FLEX_CONSUMPTION);
            final Region region = selectorRegion.getValue();
            final String supportedRegionsValue = validRegions.stream().map(Region::getName).collect(Collectors.joining(","));
            if (Objects.nonNull(region) && !validRegions.contains(region)) {
                return Collections.singletonList(AzureValidationInfo.error(String.format("`%s` is not a valid region for flex consumption app, supported values are %s", region.getName(), supportedRegionsValue), selectorRegion));
            }
            // runtime
            final List<? extends FunctionAppRuntime> validFlexRuntimes = Objects.isNull(region) ? Collections.emptyList() :
                    Azure.az(AzureAppService.class).forSubscription(subsId).functionApps().listFlexConsumptionRuntimes(region);
            final Runtime appRuntime = selectorRuntime.getValue();
            if (Objects.nonNull(region) && !validFlexRuntimes.contains(appRuntime)) {
                final String validValues = validFlexRuntimes.stream().map(FunctionAppRuntime::getDisplayName).collect(Collectors.joining(","));
                return Collections.singletonList(AzureValidationInfo.error(String.format("`%s` is not a valid runtime for flex consumption app, supported values are %s", appRuntime.getDisplayName(), validValues), selectorRuntime));
            }
        }
        if (pnlProfile.isVisible()) {
            final Double cpu = getSpinnerValue(spinnerCpu);
            if (cpu == null || cpu <= 0) {
                return List.of(AzureValidationInfo.error("Invalid CPU value, which should be more than 0", spinnerCpu));
            }

            final Double memory = getSpinnerValue(spinnerMemory);
            if (memory == null || memory <= 0) {
                return List.of(AzureValidationInfo.error("Invalid Memory value, which should be more than 0", spinnerMemory));
            }
        }
        return AzureFormPanel.super.validateAdditionalInfo();
    }
}
