/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.creation;

import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBIntSpinner;
import com.microsoft.azure.toolkit.intellij.common.*;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.container.AzureDockerClient;
import com.microsoft.azure.toolkit.intellij.containerapps.component.*;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppModule;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.model.EnvironmentType;
import com.microsoft.azure.toolkit.lib.containerapps.model.IngressConfig;
import com.microsoft.azure.toolkit.lib.containerapps.model.ResourceConfiguration;
import com.microsoft.azure.toolkit.lib.containerapps.model.WorkloadProfile;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

@Slf4j
public class ContainerAppCreationDialog extends AzureDialog<ContainerAppDraft.Config> implements AzureForm<ContainerAppDraft.Config> {
    public static final Pattern CONTAINER_APP_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9\\-]{0,30}[a-z0-9]$");
    public static final String CONTAINER_APP_NAME_VALIDATION_MESSAGE = "A name must consist of lower case alphanumeric characters or '-', start with an alphabetic character, and end with an alphanumeric character and cannot have '--'. The length must not be more than 32 characters.";
    private JPanel pnlRoot;

    private AzureHideableTitledSeparator titleApp;
    private JPanel pnlApp;
    private JLabel lblSubscription;
    private SubscriptionComboBox cbSubscription;
    private JLabel lblResourceGroup;
    private ResourceGroupComboBox cbResourceGroup;
    private AzureContainerAppsEnvironmentComboBox cbEnvironment;
    private JLabel lblContainerAppName;
    private AzureTextInput txtContainerAppName;

    private AzureHideableTitledSeparator titleIngress;
    private IngressConfigurationPanel pnlIngress;

    private AzureHideableTitledSeparator titleDeployment;
    private JPanel pnlDeployment;
    private JRadioButton btnDeployCode;
    private JRadioButton btnDeployArtifact;
    private JRadioButton btnDeployImage;
    private CodeForm formCode;
    private ArtifactForm formArtifact;
    private ImageForm formImage;

    private AzureHideableTitledSeparator titleOther;
    private JPanel pnlOther;
    private EnvironmentVariablesTextFieldWithBrowseButton inputEnv;
    private JBIntSpinner intMinReplicas;
    private JBIntSpinner intMaxReplicas;
    private WorkloadProfileComboBox cbWorkloadProfile;
    private AzureHideableTitledSeparator titleResource;
    private JPanel pnlContainerResource;
    private JLabel lblWorkloadProfile;
    private JPanel pnlProfile;
    private JLabel lblCpu;
    private JSpinner spinnerCpu;
    private JLabel lblMemory;
    private JSpinner spinnerMemory;
    private JPanel pnlConsumption;
    private JLabel lblCpuAndMemory;
    private AzureComboBox<ResourceConfiguration> cbCpuAndMemory;

    private DeploymentSourceForm formDeploymentSource;

    public static final IngressConfig QUICK_START_INGRESS = IngressConfig.builder().enableIngress(true).external(true).targetPort(80).build();

    public ContainerAppCreationDialog(final Project project) {
        super(project);
        $$$setupUI$$$();
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.cbSubscription.setRequired(true);
        this.cbResourceGroup.setRequired(true);
        this.txtContainerAppName.setRequired(true);
        this.cbEnvironment.setRequired(true);

        this.txtContainerAppName.addValidator(this::validateContainerAppName);
        this.txtContainerAppName.addValueChangedListener(this::onAppNameChanged);
        this.cbSubscription.addItemListener(this::onSubscriptionChanged);
        this.cbResourceGroup.addItemListener(this::onResourceGroupChanged);
        this.cbEnvironment.addItemListener(this::onEnvironmentChanged);
        this.cbWorkloadProfile.addItemListener(this::onWorkloadProfileChanged);

        this.btnDeployCode.addItemListener(this::onDeploymentSourceChanged);
        this.btnDeployArtifact.addItemListener(this::onDeploymentSourceChanged);
        this.btnDeployImage.addItemListener(this::onDeploymentSourceChanged);
        this.btnDeployCode.setSelected(true);
        this.formCode.setVisible(true);
        this.formImage.setVisible(false);
        this.formArtifact.setVisible(false);
        this.formImage.setOnImageFormChanged(this::onImageFormChanged);
        this.formCode.setOnFolderChanged(this::onSelectedFolderChanged);
        this.formDeploymentSource = this.formCode;

        this.lblSubscription.setLabelFor(cbSubscription);
        this.lblResourceGroup.setLabelFor(cbResourceGroup);
        this.lblContainerAppName.setLabelFor(txtContainerAppName);

        this.titleApp.addContentComponent(pnlApp);
        this.titleDeployment.addContentComponent(pnlDeployment);
        this.titleIngress.addContentComponent(pnlIngress.getPnlRoot());
        this.titleOther.addContentComponent(pnlOther);

        this.titleApp.expand();
        this.titleDeployment.expand();
        this.titleIngress.expand();
        this.titleOther.expand();
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
                this.titleResource.setVisible(false);
                this.pnlContainerResource.setVisible(false);
                return;
            }
            final boolean isWorkloadProfile = environment.getEnvironmentType() == EnvironmentType.WorkloadProfiles;
            this.cbWorkloadProfile.setEnvironment(isWorkloadProfile ? environment : null);
            this.titleResource.setVisible(isWorkloadProfile);
            this.pnlContainerResource.setVisible(isWorkloadProfile);
        }
    }

    private void onImageFormChanged(final String type) {
        final boolean useQuickStartImage = this.formDeploymentSource == this.formImage && Objects.equals(type, ImageSourceTypeComboBox.QUICK_START);
        if (useQuickStartImage) {
            pnlIngress.setValue(QUICK_START_INGRESS);
        }
        this.titleIngress.toggle(!useQuickStartImage);
        this.titleIngress.setVisible(!useQuickStartImage);
        this.pnlIngress.setVisible(!useQuickStartImage);
        this.inputEnv.setEnabled(!useQuickStartImage);
        this.intMaxReplicas.setEnabled(!useQuickStartImage);
        this.intMinReplicas.setEnabled(!useQuickStartImage);
    }

    private void onSelectedFolderChanged(final Path folder) {
        if (this.formDeploymentSource != this.formCode && Objects.nonNull(folder) && Files.isDirectory(folder)) {
            return;
        }
        final Path dockerfile = folder.resolve("Dockerfile");
        if (Files.isRegularFile(dockerfile)) {
            try {
                final List<Integer> ports = AzureDockerClient.getExposedPortsOfDockerfile(dockerfile.toFile());
                if (!ports.isEmpty()) {
                    final IngressConfig ingressConfig = IngressConfig.builder().enableIngress(true).external(true).targetPort(ports.get(0)).build();
                    this.pnlIngress.setValue(ingressConfig);
                }
            } catch (final IOException e) {
                log.error("Failed to parse Dockerfile", e);
            }
        }
    }

    private void onAppNameChanged(String s) {
        Optional.ofNullable(getContainerAppDraft()).ifPresent(this.formImage::setContainerApp);
    }

    private void mergeContainerConfiguration(final ImageForm target, final ContainerAppDraft.ImageConfig value) {
        try {
            final ContainerAppDraft.ImageConfig targetValue = target.getValue();
            if (ObjectUtils.allNotNull(targetValue, value)) {
                if (!Objects.equals(Objects.requireNonNull(targetValue).getContainerRegistry(), value.getContainerRegistry()) ||
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
            Optional.ofNullable(getContainerAppDraft()).ifPresent(this.formImage::setContainerApp);
        }
    }

    private void onSubscriptionChanged(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof Subscription) {
            final Subscription subscription = (Subscription) itemEvent.getItem();
            this.cbResourceGroup.setSubscription(subscription);
            this.cbEnvironment.setSubscription(subscription);
            Optional.ofNullable(getContainerAppDraft()).ifPresent(this.formImage::setContainerApp);
        }
    }

    @Nullable
    private ContainerAppDraft getContainerAppDraft() {
        final Subscription subscription = cbSubscription.getValue();
        final ResourceGroup resourceGroup = cbResourceGroup.getValue();
        final String appName = txtContainerAppName.getValue();
        if (ObjectUtils.anyNull(subscription, resourceGroup) || StringUtils.isBlank(appName)) {
            return null;
        }
        final ContainerAppModule module = az(AzureContainerApps.class).containerApps(subscription.getId());
        final ContainerAppDraft draft = module.create(appName, resourceGroup.getName());
        final ContainerAppDraft.Config config = new ContainerAppDraft.Config();
        config.setEnvironment(cbEnvironment.getValue());
        draft.setConfig(config);
        return draft;
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
        this.formCode = new CodeForm(this.project);
        this.formArtifact = new ArtifactForm(this.project);
        this.intMaxReplicas = new JBIntSpinner(10, 1, 300);
        this.intMinReplicas = new JBIntSpinner(0, 0, 300);

        this.cbWorkloadProfile = new WorkloadProfileComboBox();
        this.cbCpuAndMemory = new AzureComboBox<>(()-> ResourceConfiguration.CONSUMPTION_CONFIGURATIONS) {
            @Override
            protected String getItemText(Object item) {
                return item instanceof ResourceConfiguration configuration ?
                        String.format("%s CPU cores, %s Gi memory", configuration.getCpu(), configuration.getMemory()) : super.getItemText(item);
            }
        };
    }

    private void onDeploymentSourceChanged(ItemEvent event) {
        this.formImage.setVisible(this.btnDeployImage.isSelected());
        this.formArtifact.setVisible(this.btnDeployArtifact.isSelected());
        this.formCode.setVisible(this.btnDeployCode.isSelected());

        this.formDeploymentSource = this.btnDeployImage.isSelected() ? this.formImage :
            this.btnDeployArtifact.isSelected() ? this.formArtifact : this.formCode;
        this.onImageFormChanged(this.formImage.getRegistryType());
        this.onSelectedFolderChanged(this.formCode.getSourceFolder());
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
    @Nullable
    protected JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public ContainerAppDraft.Config getValue() {
        final ContainerAppDraft.Config result = new ContainerAppDraft.Config();
        result.setSubscription(cbSubscription.getValue());
        result.setResourceGroup(cbResourceGroup.getValue());
        result.setName(txtContainerAppName.getValue());
        result.setEnvironment(cbEnvironment.getValue());
        result.setIngressConfig(pnlIngress.getValue());
        if (titleResource.isVisible()) {
            final ResourceConfiguration configuration = ResourceConfiguration.builder().workloadProfile(cbWorkloadProfile.getValue()).build();
            if (pnlProfile.isVisible()) {
                final Double cpu = spinnerCpu.getValue() instanceof Integer integer ? Double.valueOf(integer) : (Double) spinnerCpu.getValue();
                final Double memory = spinnerMemory.getValue() instanceof Integer integer ? Double.valueOf(integer) : (Double) spinnerMemory.getValue();
                Optional.ofNullable(cpu).ifPresent(configuration::setCpu);
                Optional.ofNullable(memory).ifPresent(value -> configuration.setMemory(value + "Gi"));
            } else if (pnlConsumption.isVisible()) {
                Optional.ofNullable(cbCpuAndMemory.getValue()).ifPresent(c -> {
                    configuration.setCpu(c.getCpu());
                    configuration.setMemory(c.getMemory());
                });
            }
            result.setResourceConfiguration(configuration);
        }

        // set app for image form so that it can get correct ImageConfig
        final ContainerAppModule module = az(AzureContainerApps.class).containerApps(result.getSubscription().getId());
        final ContainerAppDraft draft = module.create(result.getName(), result.getResourceGroup().getName());
        draft.setConfig(result);
        this.formDeploymentSource.setContainerApp(draft);

        final ContainerAppDraft.ImageConfig imageConfig = this.formDeploymentSource.getValue();
        final List<EnvironmentVar> vars = inputEnv.getEnvironmentVariables().entrySet().stream()
            .map(e -> new EnvironmentVar().withName(e.getKey()).withValue(e.getValue()))
            .collect(Collectors.toList());
        Optional.ofNullable(imageConfig).ifPresent(config -> config.setEnvironmentVariables(vars));
        result.setImageConfig(imageConfig);

        final ContainerAppDraft.ScaleConfig scaleConfig = ContainerAppDraft.ScaleConfig.builder()
            .maxReplicas(this.intMaxReplicas.getNumber())
            .minReplicas(this.intMinReplicas.getNumber())
            .build();
        result.setScaleConfig(scaleConfig);
        return result;
    }

    @Override
    public void setValue(ContainerAppDraft.Config data) {
        Optional.ofNullable(data.getSubscription()).ifPresent(cbSubscription::setValue);
        Optional.ofNullable(data.getResourceGroup()).ifPresent(cbResourceGroup::setValue);
        Optional.ofNullable(data.getName()).ifPresent(txtContainerAppName::setValue);
        Optional.ofNullable(data.getEnvironment()).ifPresent(cbEnvironment::setValue);
        Optional.ofNullable(data.getScaleConfig()).ifPresent(c -> {
            // https://learn.microsoft.com/en-us/azure/container-apps/scale-app?pivots=azure-cli
            this.intMaxReplicas.setNumber(Optional.ofNullable(c.getMaxReplicas()).orElse(10));
            this.intMinReplicas.setNumber(Optional.ofNullable(c.getMinReplicas()).orElse(0));
        });
        Optional.ofNullable(data.getResourceConfiguration()).map(ResourceConfiguration::getWorkloadProfile).ifPresent(cbWorkloadProfile::setValue);
        final ContainerAppDraft.ImageConfig imageConfig = data.getImageConfig();
        if (Objects.nonNull(imageConfig)) {
            final Optional<Path> source = Optional.ofNullable(imageConfig.getBuildImageConfig()).map(ContainerAppDraft.BuildImageConfig::getSource);
            if (source.isEmpty()) {
                this.btnDeployImage.setSelected(true);
                this.formImage.setValue(imageConfig);
            } else if (source.map(Files::isDirectory).orElse(false)) {
                this.btnDeployCode.setSelected(true);
                this.formCode.setValue(imageConfig);
            } else {
                this.btnDeployArtifact.setSelected(true);
                this.formArtifact.setValue(imageConfig);
            }
            this.inputEnv.setEnvironmentVariables(imageConfig.getEnvironmentVariables().stream()
                .collect(Collectors.toMap(EnvironmentVar::name, EnvironmentVar::value)));
            Optional.ofNullable(data.getIngressConfig()).ifPresent(pnlIngress::setValue);
        }
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(cbSubscription, cbResourceGroup, txtContainerAppName, this.formDeploymentSource);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
