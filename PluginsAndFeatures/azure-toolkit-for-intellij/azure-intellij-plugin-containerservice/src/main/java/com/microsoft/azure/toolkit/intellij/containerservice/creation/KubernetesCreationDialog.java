/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerservice.creation;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureIntegerInput;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerservice.AzureContainerService;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesCluster;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesClusterDraft;
import com.microsoft.azure.toolkit.lib.containerservice.model.VirtualMachineSize;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class KubernetesCreationDialog extends AzureDialog<KubernetesClusterDraft.Config> implements AzureForm<KubernetesClusterDraft.Config> {
    private static final Pattern KUBERNETES_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]([\\w-]*[a-zA-Z0-9])?$");
    private static final Pattern DNS_NAME_PREFIX_PATTERN = Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9-]{0,52}[a-zA-Z0-9])?$");
    public static final int MAX_NODE_COUNT = 1000;
    public static final int MIN_NODE_COUNT = 1;

    private JPanel pnlRoot;
    private JLabel lblSubscription;
    private SubscriptionComboBox cbSubscription;
    private JLabel lblResourceGroup;
    private ResourceGroupComboBox cbResourceGroup;
    private RegionComboBox cbRegion;
    private AzureTextInput txtName;
    private JRadioButton manualRadioButton;
    private JRadioButton autoScaleRadioButton;
    private AzureTextInput txtDnsPrefix;
    private JLabel lblDnsNamePrefix;
    private AzureIntegerInput txtMaxNodeCount;
    private JLabel lblMaxNodeCount;
    private AzureIntegerInput txtMinNodeCount;
    private JLabel lblMinNodeCount;
    private JLabel lblNodeCount;
    private AzureIntegerInput txtNodeCount;
    private JLabel lblScaleMethod;
    private JLabel lblNodeSize;
    private JLabel lblKubernetesVersion;
    private JLabel lblRegion;
    private JLabel lblName;
    private AzureComboBox<String> cbKubernetesVersion;
    private AzureComboBox<VirtualMachineSize> cbNodeSize;

    public KubernetesCreationDialog(@Nullable Project project) {
        super(project);
        $$$setupUI$$$();
        init();
    }

    @Override
    public AzureForm<KubernetesClusterDraft.Config> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Kubernetes Service";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public KubernetesClusterDraft.Config getValue() {
        final KubernetesClusterDraft.Config result = new KubernetesClusterDraft.Config();
        result.setSubscription(cbSubscription.getValue());
        result.setResourceGroup(cbResourceGroup.getValue());
        result.setName(txtName.getValue());
        result.setRegion(cbRegion.getValue());
        result.setKubernetesVersion(cbKubernetesVersion.getValue());
        result.setSize(cbNodeSize.getValue());
        if (autoScaleRadioButton.isSelected()) {
            result.setVmCount(txtMinNodeCount.getValue());
            result.setMinVMCount(txtMinNodeCount.getValue());
            result.setMaxVMCount(txtMaxNodeCount.getValue());
        } else {
            result.setVmCount(txtNodeCount.getValue());
        }
        result.setDnsPrefix(txtDnsPrefix.getValue());
        return result;
    }

    @Override
    public void setValue(@Nonnull KubernetesClusterDraft.Config data) {
        Optional.ofNullable(data.getSubscription()).ifPresent(cbSubscription::setValue);
        Optional.ofNullable(data.getResourceGroup()).ifPresent(cbResourceGroup::setValue);
        Optional.ofNullable(data.getName()).ifPresent(txtName::setValue);
        Optional.ofNullable(data.getRegion()).ifPresent(cbRegion::setValue);
        Optional.ofNullable(data.getKubernetesVersion()).ifPresent(cbKubernetesVersion::setValue);

        Optional.ofNullable(data.getSize()).ifPresent(cbNodeSize::setValue);
        Optional.ofNullable(data.getDnsPrefix()).ifPresent(txtDnsPrefix::setValue);
        if (data.getMinVMCount() != null && data.getMaxVMCount() != null) {
            txtMinNodeCount.setValue(data.getMinVMCount());
            txtMaxNodeCount.setValue(data.getMaxVMCount());
        } else {
            txtNodeCount.setValue(data.getVmCount());
        }
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(cbSubscription, cbResourceGroup, txtName, cbRegion, cbKubernetesVersion,
                cbNodeSize, txtNodeCount, txtMinNodeCount, txtMaxNodeCount, txtDnsPrefix);
    }

    @Override
    protected void init() {
        super.init();
        this.cbSubscription.setRequired(true);
        this.cbResourceGroup.setRequired(true);
        this.cbRegion.setRequired(true);
        this.txtName.setRequired(true);
        this.txtName.addValidator(this::validateKubernetesClusterName);
        this.txtDnsPrefix.setRequired(true);
        this.txtDnsPrefix.addValidator(this::validateDnsNamePrefix);
        final ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(manualRadioButton);
        buttonGroup.add(autoScaleRadioButton);
        // todo: add validator for k8s name
        this.cbSubscription.addItemListener(this::onSubscriptionChanged);
        this.cbResourceGroup.addItemListener(e -> this.txtName.validateValueAsync()); // trigger validation after resource group changed
        this.cbRegion.addItemListener(e -> this.cbKubernetesVersion.reloadItems());

        this.manualRadioButton.addItemListener(e -> toggleScaleMethod(!manualRadioButton.isSelected()));
        this.autoScaleRadioButton.addItemListener(e -> toggleScaleMethod(autoScaleRadioButton.isSelected()));
        this.autoScaleRadioButton.setSelected(true);

        this.txtNodeCount.setMinValue(MIN_NODE_COUNT);
        this.txtNodeCount.setMaxValue(MAX_NODE_COUNT);
        this.txtMinNodeCount.setMinValue(MIN_NODE_COUNT);
        this.txtMaxNodeCount.setMaxValue(MAX_NODE_COUNT);
        this.txtMaxNodeCount.setMinValue(MIN_NODE_COUNT);
        this.txtMaxNodeCount.setMaxValue(MAX_NODE_COUNT);
        this.txtMaxNodeCount.addValidator(this::validateNodeCount);

        this.lblSubscription.setLabelFor(cbSubscription);
        this.lblResourceGroup.setLabelFor(cbResourceGroup);
        this.lblName.setLabelFor(txtName);
        this.lblRegion.setLabelFor(cbRegion);
        this.lblKubernetesVersion.setLabelFor(cbKubernetesVersion);
        this.lblNodeSize.setLabelFor(cbNodeSize);
        this.lblNodeCount.setLabelFor(txtNodeCount);
        this.lblMinNodeCount.setLabelFor(txtMinNodeCount);
        this.lblMaxNodeCount.setLabelFor(txtMaxNodeCount);
        this.lblDnsNamePrefix.setLabelFor(txtDnsPrefix);
    }

    private void toggleScaleMethod(boolean isAutoScale) {
        lblNodeCount.setVisible(!isAutoScale);
        txtNodeCount.setVisible(!isAutoScale);
        txtNodeCount.setRequired(!isAutoScale);

        lblMinNodeCount.setVisible(isAutoScale);
        txtMinNodeCount.setVisible(isAutoScale);
        txtMinNodeCount.setRequired(isAutoScale);
        lblMaxNodeCount.setVisible(isAutoScale);
        txtMaxNodeCount.setVisible(isAutoScale);
        txtMaxNodeCount.setRequired(isAutoScale);
        if (isAutoScale) {
            if (StringUtils.isAllBlank(txtMinNodeCount.getText(), txtMaxNodeCount.getText())) {
                txtMinNodeCount.setValue(txtNodeCount.getValue());
                txtMaxNodeCount.setValue(txtNodeCount.getValue());
            }
        } else {
            if (StringUtils.isBlank(txtNodeCount.getText())) {
                txtNodeCount.setValue(txtMinNodeCount.getValue());
            }
        }
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof Subscription) {
            final Subscription subscription = (Subscription) e.getItem();
            this.cbResourceGroup.setSubscription(subscription);
            this.cbRegion.setSubscription(subscription);
            this.cbKubernetesVersion.reloadItems();
            this.txtName.validateValueAsync(); // trigger validation after subscription changed
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        final Supplier<List<String>> kubernetesVersionSupplier = () -> {
            final Subscription subscription = cbSubscription.getValue();
            final Region region = cbRegion.getValue();
            if (subscription == null || region == null) {
                return Collections.emptyList();
            }
            return new ArrayList<>(Azure.az(AzureContainerService.class).kubernetes(subscription.getId()).listVirtualMachineVersion(region));
        };
        this.cbKubernetesVersion = new AzureComboBox<>(kubernetesVersionSupplier);
        this.cbKubernetesVersion.setRequired(true);
        this.cbNodeSize = new AzureComboBox<>(VirtualMachineSize::values) {
            @Override
            protected String getItemText(Object item) {
                return item instanceof VirtualMachineSize ? ((VirtualMachineSize) item).getValue() : super.getItemText(item);
            }

            @Nonnull
            @Override
            protected List<ExtendableTextComponent.Extension> getExtensions() {
                return Collections.emptyList();
            }
        };
        this.cbNodeSize.setRequired(true);
    }

    public AzureValidationInfo validateNodeCount() {
        final Integer min = txtMinNodeCount.getValue();
        final Integer max = txtMaxNodeCount.getValue();
        if (ObjectUtils.allNotNull(min, max) && min > max) {
            return AzureValidationInfo.error("Min node count is higher than max node count", txtMaxNodeCount);
        }
        return AzureValidationInfo.success(txtMaxNodeCount);
    }

    private AzureValidationInfo validateKubernetesClusterName() {
        final String name = txtName.getValue();
        if (StringUtils.isEmpty(name)) {
            return AzureValidationInfo.error(AzureMessageBundle.message("kubernetes.cluster.name.validate.empty").toString(), txtName);
        }
        if (!KUBERNETES_NAME_PATTERN.matcher(name).matches()) {
            return AzureValidationInfo.error(AzureMessageBundle.message("kubernetes.cluster.name.validate.invalid").toString(), txtName);
        }
        final Subscription subscription = cbSubscription.getValue();
        final ResourceGroup resourceGroup = cbResourceGroup.getValue();
        if (ObjectUtils.allNotNull(subscription, resourceGroup)) {
            final KubernetesCluster kubernetesCluster = Azure.az(AzureContainerService.class).kubernetes(subscription.getId()).get(name, resourceGroup.getName());
            if (kubernetesCluster != null && kubernetesCluster.exists()) {
                return AzureValidationInfo.error(AzureMessageBundle.message("kubernetes.cluster.name.validate.exist").toString(), txtName);
            }
        }
        return AzureValidationInfo.success(txtName);
    }

    private AzureValidationInfo validateDnsNamePrefix() {
        final String name = txtDnsPrefix.getValue();
        if (StringUtils.isEmpty(name)) {
            return AzureValidationInfo.error(AzureMessageBundle.message("kubernetes.cluster.dnsNamePrefix.validate.empty").toString(), txtDnsPrefix);
        }
        if (!DNS_NAME_PREFIX_PATTERN.matcher(name).matches()) {
            return AzureValidationInfo.error(AzureMessageBundle.message("kubernetes.cluster.dnsNamePrefix.validate.invalid").toString(), txtDnsPrefix);
        }
        return AzureValidationInfo.success(txtDnsPrefix);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
