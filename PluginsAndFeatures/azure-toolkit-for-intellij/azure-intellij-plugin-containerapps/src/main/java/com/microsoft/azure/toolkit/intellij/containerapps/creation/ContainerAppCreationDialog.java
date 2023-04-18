/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.creation;

import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureHideableTitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.EnvironmentVariablesTextFieldWithBrowseButton;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.containerapps.component.AzureContainerAppsEnvironmentComboBox;
import com.microsoft.azure.toolkit.intellij.containerapps.component.ImageForm;
import com.microsoft.azure.toolkit.intellij.containerapps.component.IngressConfigurationPanel;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.model.IngressConfig;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ContainerAppCreationDialog extends AzureDialog<ContainerAppDraft.Config> implements AzureForm<ContainerAppDraft.Config> {
    private static final Pattern CONTAINER_APP_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9\\-]{0,30}[a-z0-9]$");
    private static final String CONTAINER_APP_NAME_VALIDATION_MESSAGE = "A name must consist of lower case alphanumeric characters or '-', start with an alphabetic character, and end with an alphanumeric character and cannot have '--'. The length must not be more than 32 characters.";
    private JLabel lblSubscription;
    private SubscriptionComboBox cbSubscription;
    private JLabel lblResourceGroup;
    private ResourceGroupComboBox cbResourceGroup;
    private JLabel lblContainerAppName;
    private AzureTextInput txtContainerAppName;
    private JLabel lblRegion;
    private RegionComboBox cbRegion;
    private JPanel pnlRoot;
    private JCheckBox chkUseQuickStart;
    private ImageForm pnlContainer;
    private AzureContainerAppsEnvironmentComboBox cbEnvironment;
    private TitledSeparator titleContainerDetails;
    private AzureHideableTitledSeparator titleIngress;
    private EnvironmentVariablesTextFieldWithBrowseButton inputEnv;
    private JLabel lblEnv;
    private AzureHideableTitledSeparator titleAppSettings;
    private AzureHideableTitledSeparator titleProjectDetails;
    private AzureHideableTitledSeparator titleContainerAppsEnvironment;
    private JPanel pnlIngressSettingsHolder;
    private JPanel pnlAppSettings;
    private JPanel pnlProjectDetails;
    private JPanel pnlContainerAppsEnvironment;
    private IngressConfigurationPanel pnlIngressConfiguration;

    public static final ContainerAppDraft.ImageConfig QUICK_START_IMAGE = ContainerAppDraft.ImageConfig.builder()
            .fullImageName("mcr.microsoft.com/azuredocs/containerapps-helloworld:latest").environmentVariables(new ArrayList<>()).build();
    public static final IngressConfig QUICK_START_INGRESS = IngressConfig.builder().enableIngress(true).external(true).targetPort(80).build();

    private final Project project;

    public ContainerAppCreationDialog(final Project project) {
        super(project);
        this.project = project;
        $$$setupUI$$$();
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.cbSubscription.setRequired(true);
        this.cbResourceGroup.setRequired(true);
        this.cbRegion.setRequired(true);
        this.txtContainerAppName.setRequired(true);

        this.txtContainerAppName.addValidator(this::validateContainerAppName);
        this.cbSubscription.addItemListener(this::onSubscriptionChanged);
        this.cbRegion.addItemListener(this::onRegionChanged); // trigger validation after resource group changed
        this.cbResourceGroup.addItemListener(this::onResourceGroupChanged);
        this.chkUseQuickStart.addItemListener(e -> this.onSelectQuickImage(chkUseQuickStart.isSelected()));

        this.chkUseQuickStart.setSelected(true);

        this.lblSubscription.setLabelFor(cbSubscription);
        this.lblResourceGroup.setLabelFor(cbResourceGroup);
        this.lblContainerAppName.setLabelFor(txtContainerAppName);
        this.lblRegion.setLabelFor(cbRegion);

        this.titleProjectDetails.addContentComponent(pnlProjectDetails);
        this.titleContainerAppsEnvironment.addContentComponent(pnlContainerAppsEnvironment);
        this.titleAppSettings.addContentComponent(pnlAppSettings);
        this.titleIngress.addContentComponent(pnlIngressSettingsHolder);

        this.titleProjectDetails.expand();
        this.titleContainerAppsEnvironment.expand();
        this.titleAppSettings.expand();
        this.titleIngress.collapse();
    }

    private void mergeContainerConfiguration(final ImageForm target, final ContainerAppDraft.ImageConfig value) {
        try {
            final ContainerAppDraft.ImageConfig targetValue = target.getValue();
            if (ObjectUtils.allNotNull(targetValue, value)) {
                if (!Objects.equals(targetValue.getContainerRegistry(), value.getContainerRegistry()) ||
                        !Objects.equals(targetValue.getFullImageName(), value.getFullImageName())) {
                    target.setValue(value);
                }
            }
        } catch (final RuntimeException e) {
            // swallow exception as required parameters may be null
            target.setValue(value);
        }
    }

    private void onResourceGroupChanged(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof ResourceGroup) {
            final ResourceGroup resourceGroup = (ResourceGroup) itemEvent.getItem();
            this.cbEnvironment.setResourceGroup(resourceGroup);
        }
    }

    private void onRegionChanged(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof Region) {
            final Region region = (Region) itemEvent.getItem();
            this.txtContainerAppName.validateValueAsync();
            this.cbEnvironment.setRegion(region);
        }
    }

    private void onSubscriptionChanged(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof Subscription) {
            final Subscription subscription = (Subscription) itemEvent.getItem();
            this.cbResourceGroup.setSubscription(subscription);
            this.cbRegion.setSubscription(subscription);
            this.cbEnvironment.setSubscription(subscription);
        }
    }

    private AzureValidationInfo validateContainerAppName() {
        final String name = txtContainerAppName.getValue();
        final ContainerAppsEnvironment value = cbEnvironment.getValue();
        if (value != null && !value.isDraftForCreating()) {
            final Availability availability = value.checkContainerAppNameAvailability(name);
            return availability.isAvailable() ? AzureValidationInfo.success(txtContainerAppName) :
                    AzureValidationInfo.error(availability.getUnavailabilityMessage(), txtContainerAppName);
        } else {
            final Matcher matcher = CONTAINER_APP_NAME_PATTERN.matcher(name);
            return matcher.matches() && !StringUtils.contains(name, "--") ? AzureValidationInfo.success(txtContainerAppName) :
                    AzureValidationInfo.error(CONTAINER_APP_NAME_VALIDATION_MESSAGE, txtContainerAppName);
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbEnvironment = new AzureContainerAppsEnvironmentComboBox();
        this.cbEnvironment.setRequired(true);
    }

    private void onSelectQuickImage(boolean useQuickStartImage) {
        if (useQuickStartImage) {
            // set image config
            this.pnlContainer.setValue(QUICK_START_IMAGE);
            // set ingress config
            pnlIngressConfiguration.setValue(QUICK_START_INGRESS);
        }
        // toggle app settings enable status
        this.pnlContainer.setEnabled(!useQuickStartImage);
        this.pnlContainer.setVisible(!useQuickStartImage);
        this.titleIngress.setEnabled(!useQuickStartImage);
        if (!useQuickStartImage) {
            titleIngress.expand();
        } else {
            titleIngress.collapse();
        }
        this.pnlIngressConfiguration.setEnabled(!useQuickStartImage);
        this.lblEnv.setEnabled(!useQuickStartImage);
        this.inputEnv.setEnabled(!useQuickStartImage);
    }

    @Override
    public AzureForm<ContainerAppDraft.Config> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Azure Container App";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public ContainerAppDraft.Config getValue() {
        final ContainerAppDraft.Config result = new ContainerAppDraft.Config();
        result.setSubscription(cbSubscription.getValue());
        result.setResourceGroup(cbResourceGroup.getValue());
        result.setName(txtContainerAppName.getValue());
        result.setRegion(cbRegion.getValue());
        result.setEnvironment(cbEnvironment.getValue());
        final ContainerAppDraft.ImageConfig value = pnlContainer.getValue();
        final List<EnvironmentVar> vars = inputEnv.getEnvironmentVariables().entrySet().stream()
                .map(e -> new EnvironmentVar().withName(e.getKey()).withValue(e.getValue()))
                .collect(Collectors.toList());
        Optional.ofNullable(value).ifPresent(config -> config.setEnvironmentVariables(vars));
        result.setImageConfig(value);
        result.setIngressConfig(pnlIngressConfiguration.getValue());
        return result;
    }

    @Override
    public void setValue(ContainerAppDraft.Config data) {
        Optional.ofNullable(data.getSubscription()).ifPresent(cbSubscription::setValue);
        Optional.ofNullable(data.getResourceGroup()).ifPresent(cbResourceGroup::setValue);
        Optional.ofNullable(data.getName()).ifPresent(txtContainerAppName::setValue);
        Optional.ofNullable(data.getRegion()).ifPresent(cbRegion::setValue);
        Optional.ofNullable(data.getEnvironment()).ifPresent(cbEnvironment::setValue);
        final ContainerAppDraft.ImageConfig imageConfig = data.getImageConfig();
        if (Objects.isNull(imageConfig)) {
            // use quick start image by default
            onSelectQuickImage(true);
        } else {
            this.pnlContainer.setValue(imageConfig);
            // todo: add logic to set envs
            Optional.ofNullable(data.getIngressConfig()).ifPresent(pnlIngressConfiguration::setValue);
        }
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(cbSubscription, cbResourceGroup, txtContainerAppName, cbRegion, pnlContainer);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
