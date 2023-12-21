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
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.containerapps.component.AzureContainerAppsEnvironmentComboBox;
import com.microsoft.azure.toolkit.intellij.containerapps.component.ImageForm;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppNameInput;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.platform.RuntimeComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.serviceplan.ServicePlanComboBox;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.*;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.legacy.function.FunctionAppService;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig;
import com.microsoft.azuretools.utils.WebAppUtils;
import lombok.Getter;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

@Getter
public class FunctionAppInfoPanel extends JPanel implements AzureFormPanel<FunctionAppConfig> {
    public static final ContainerAppDraft.ImageConfig QUICK_START_IMAGE =
            new ContainerAppDraft.ImageConfig(FunctionAppService.DEFAULT_IMAGE);

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
    private AzureArtifactComboBox selectorApplication;
    private ServicePlanComboBox selectorServicePlan;
    private TitledSeparator deploymentTitle;
    private JLabel lblArtifact;
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

    public FunctionAppInfoPanel(final Project project) {
        super();
        this.project = project;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    @Override
    @Nonnull
    public FunctionAppConfig getValue() {
        final FunctionAppConfig config = FunctionAppConfig.builder().build();
        config.setSubscription(this.selectorSubscription.getValue());
        config.setResourceGroup(ResourceGroupConfig.fromResource(this.selectorGroup.getValue()));
        config.setName(this.textName.getValue());
        final Runtime runtime = this.selectorRuntime.getValue();
        config.setRuntime(runtime);
        config.setRegion(this.selectorRegion.getValue());
        if (rdoServicePlan.isSelected()) {
            final AppServicePlan servicePlan = this.selectorServicePlan.getValue();
            final AppServicePlanConfig planConfig = AppServicePlanConfig.fromResource(servicePlan);
            if (Objects.nonNull(planConfig) && servicePlan.isDraftForCreating()) {
                planConfig.setResourceGroupName(config.getResourceGroupName());
                planConfig.setRegion(config.getRegion());
                final boolean isWindows = Optional.ofNullable(runtime).map(Runtime::isWindows).orElse(false);
                planConfig.setOs(isWindows ? OperatingSystem.WINDOWS : OperatingSystem.LINUX);
            }
            config.setServicePlan(planConfig);
        } else if (rdoContainerAppsEnvironment.isSelected()) {
            config.setRuntime(FunctionAppDockerRuntime.INSTANCE);
            // environment
            final ContainerAppsEnvironment env = cbEnvironment.getValue();
            Optional.ofNullable(env).ifPresent(environment -> config.setEnvironment(environment.getName()));
            // image
            final ContainerAppDraft.ImageConfig image = chkUseQuickStart.isSelected() ? QUICK_START_IMAGE : pnlContainer.getValue();
            Optional.ofNullable(image).ifPresent(config::setImage);
        }
        Optional.ofNullable(this.selectorApplication.getValue())
                .map(AzureArtifact::getFileForDeployment).map(Paths::get).ifPresent(config::setApplication);
        return config;
    }

    @Override
    public void setValue(final FunctionAppConfig config) {
        this.selectorSubscription.setValue(config.getSubscription());
        this.textName.setValue(config.getName());
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            this.selectorGroup.setValue(Optional.ofNullable(config.getResourceGroup()).map(ResourceGroupConfig::toResource).orElse(null));
            this.selectorRuntime.setValue(config.getRuntime());
            this.selectorRegion.setValue(config.getRegion());
            final boolean useEnvironment = StringUtils.isNotEmpty(config.getEnvironment());
            this.rdoContainerAppsEnvironment.setSelected(useEnvironment);
            this.rdoServicePlan.setSelected(!useEnvironment);
            toggleHostingConfiguration(!useEnvironment);

            Optional.ofNullable(config.getServicePlan()).filter(ignore -> !useEnvironment)
                    .map(AppServicePlanConfig::toResource).ifPresent(selectorServicePlan::setValue);
            Optional.ofNullable(config.getEnvironment()).filter(ignore -> useEnvironment)
                    .ifPresent(env -> cbEnvironment.setValue(r -> StringUtils.equalsIgnoreCase(r.getName(), env)));
            chkUseQuickStart.setSelected(Objects.isNull(config.getImage()) || StringUtils.equals(FunctionAppService.DEFAULT_IMAGE, config.getImage().getFullImageName()));
            toggleImageType(Objects.isNull(config.getImage()));
            Optional.ofNullable(config.getImage()).filter(ignore -> useEnvironment).ifPresent(pnlContainer::setValue);
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
            this.selectorApplication,
            this.selectorServicePlan,
            this.cbEnvironment
        };
        return Arrays.asList(inputs);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
        super.setVisible(visible);
    }

    public void setDeploymentVisible(boolean visible) {
        this.deploymentTitle.setVisible(visible);
        this.lblArtifact.setVisible(visible);
        this.selectorApplication.setVisible(visible);
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

        this.selectorRuntime.setPlatformList(FunctionAppRuntime.getMajorRuntimes());
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
        this.lblArtifact.setLabelFor(selectorApplication);
        this.selectorApplication.setFileFilter(virtualFile -> {
            final String ext = FileNameUtils.getExtension(virtualFile.getPath());
            final Runtime runtime = this.selectorRuntime.getValue();
            return StringUtils.isNotBlank(ext) && (runtime == null || WebAppUtils.isSupportedArtifactType(runtime, ext));
        });

        this.lblSubscription.setIcon(AllIcons.General.ContextHelp);
        this.lblResourceGroup.setIcon(AllIcons.General.ContextHelp);
        this.lblAppServicePlan.setIcon(AllIcons.General.ContextHelp);
    }

    private void toggleImageType(final boolean useQuickStart){
        pnlContainer.setVisible(!useQuickStart);
    }

    private void toggleHostingConfiguration(final boolean useServicePlan) {
        this.titleEnvironment.setVisible(!useServicePlan);
        this.pnlContainerAppsEnvironment.setVisible(!useServicePlan);
        this.titleServicePlan.setVisible(useServicePlan);
        this.pnlAppServicePlan.setVisible(useServicePlan);

        this.selectorServicePlan.setRequired(useServicePlan);
        this.selectorServicePlan.revalidate();
        this.cbEnvironment.setRequired(!useServicePlan);
        this.cbEnvironment.revalidate();

        this.lblPlatform.setVisible(useServicePlan);
        this.selectorRuntime.setVisible(useServicePlan); // for container based function, only docker is supported
    }

    private void onGroupChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final ResourceGroup item = (ResourceGroup) e.getItem();
            this.selectorServicePlan.setResourceGroup(item);
            this.cbEnvironment.setResourceGroup(item);
        }
    }

    private void onRegionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Region region = (Region) e.getItem();
            this.selectorServicePlan.setRegion(region);
            this.cbEnvironment.setRegion(region);
        }
    }

    private void onRuntimeChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Runtime runtime = (Runtime) e.getItem();
            final OperatingSystem operatingSystem = Objects.isNull(runtime) ? null :
                                                    // Docker runtime use Linux service plan too
                                                    runtime.isWindows() ? OperatingSystem.WINDOWS : OperatingSystem.LINUX;
            this.selectorServicePlan.setOperatingSystem(operatingSystem);
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
            final String pricing = plan.getPricingTier().toString();
            this.textSku.setText(pricing);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.textSku.setText(NOT_APPLICABLE);
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.selectorApplication = new AzureArtifactComboBox(project, true);
        this.selectorApplication.reloadItems();

        this.pnlImageContainer = new JPanel(new GridLayoutManager(1, 1));
        this.pnlContainer = new ImageForm();
        this.pnlImageContainer.add(this.pnlContainer.getContentPanel(), new GridConstraints(0, 0, 1, 1, 0,
                GridConstraints.FILL_BOTH, 7, 7, null, null, null, 0));
    }
}