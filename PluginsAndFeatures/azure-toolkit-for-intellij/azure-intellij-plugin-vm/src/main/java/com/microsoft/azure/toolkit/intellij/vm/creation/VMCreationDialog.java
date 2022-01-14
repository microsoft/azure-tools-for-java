/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.AzureFileInput;
import com.microsoft.azure.toolkit.intellij.common.component.AzurePasswordFieldInput;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.vm.creation.component.AzureStorageAccountComboBox;
import com.microsoft.azure.toolkit.intellij.vm.creation.component.InboundPortRulesForm;
import com.microsoft.azure.toolkit.intellij.vm.creation.component.NetworkAvailabilityOptionsComboBox;
import com.microsoft.azure.toolkit.intellij.vm.creation.component.SecurityGroupComboBox;
import com.microsoft.azure.toolkit.intellij.vm.creation.component.SubnetComboBox;
import com.microsoft.azure.toolkit.intellij.vm.creation.component.VirtualMachineImageComboBox;
import com.microsoft.azure.toolkit.intellij.vm.creation.component.VirtualMachineSizeComboBox;
import com.microsoft.azure.toolkit.intellij.vm.creation.component.VirtualNetworkComboBox;
import com.microsoft.azure.toolkit.intellij.vm.creation.component.ip.PublicIPAddressComboBox;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.compute.network.Network;
import com.microsoft.azure.toolkit.lib.compute.security.DraftNetworkSecurityGroup;
import com.microsoft.azure.toolkit.lib.compute.security.model.SecurityRule;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureImage;
import com.microsoft.azure.toolkit.lib.compute.vm.DraftVirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.vm.model.AuthenticationType;
import com.microsoft.azure.toolkit.lib.compute.vm.model.AzureSpotConfig;
import com.microsoft.azure.toolkit.lib.compute.vm.model.OperatingSystem;
import io.jsonwebtoken.lang.Collections;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VMCreationDialog extends AzureDialog<DraftVirtualMachine> implements AzureForm<DraftVirtualMachine> {
    private static final String SSH_PUBLIC_KEY_DESCRIPTION = "<html> Provide an RSA public key file in the single-line format (starting with \"ssh-rsa\") or " +
            "the multi-line PEM format. <p/> You can generate SSH keys using ssh-keygen on Linux and OS X, or PuTTYGen on Windows. </html>";
    private static final String SELECT_CERT_TITLE = "Select Cert for Your VM";
    private static final String VIRTUAL_MACHINE_CREATION_DIALOG_TITLE = "Create Virtual Machine";
    private JTabbedPane tabbedPane;
    private JPanel rootPane;
    private JPanel basicPane;
    private JLabel lblResourceGroup;
    private JLabel lblSubscription;
    private JLabel lblVirtualMachineName;
    private AzureTextInput txtVisualMachineName;
    private JLabel lblRegion;
    private JRadioButton rdoSshPublicKey;
    private JRadioButton rdoPassword;
    private AzureTextInput txtUserName;
    private JRadioButton rdoNoneSecurityGroup;
    private JRadioButton rdoBasicSecurityGroup;
    private JRadioButton rdoAdvancedSecurityGroup;
    private JCheckBox chkAzureSpotInstance;
    private JRadioButton rdoStopAndDeallocate;
    private JRadioButton rdoDelete;
    private AzureTextInput txtMaximumPrice;
    private JLabel lblUserName;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JLabel lblPassword;
    private JLabel lblConfirmPassword;
    private JLabel lblCertificate;
    private AzureFileInput txtCertificate;
    private JLabel lblConfigureSecurityGroup;
    private SecurityGroupComboBox cbSecurityGroup;
    private JLabel lblEvictionPolicy;
    private JLabel lblMaximumPrice;
    private JLabel lblAvailabilityOptions;
    private JLabel lblImage;
    private JLabel lblSize;
    private JLabel lblAuthenticationType;
    private JLabel lblVirtualNetwork;
    private JLabel lblSubnet;
    private JLabel lblPublicIP;
    private JLabel lblSecurityGroup;
    private JLabel lblStorageAccount;
    private JLabel lblAzureSportInstance;
    private JPanel pnlSecurityRadios;
    private VirtualMachineSizeComboBox cbSize;
    private SubscriptionComboBox cbSubscription;
    private ResourceGroupComboBox cbResourceGroup;
    private RegionComboBox cbRegion;
    private VirtualMachineImageComboBox cbImage;
    private NetworkAvailabilityOptionsComboBox cbAvailabilityOptions;
    private VirtualNetworkComboBox cbVirtualNetwork;
    private SubnetComboBox cbSubnet;
    private PublicIPAddressComboBox cbPublicIp;
    private AzureStorageAccountComboBox cbStorageAccount;
    private JPanel advancedPane;
    private TitledSeparator titleInboundPortRules;
    private InboundPortRulesForm pnlBasicPorts;
    private InboundPortRulesForm pnlPorts;
    private JLabel lblPublicInboundPorts;
    private JPanel pnlPublicInboundsRadios;
    private JRadioButton rdoAllowSelectedInboundPorts;
    private JRadioButton rdoNoneInboundPorts;
    private AzurePasswordFieldInput passwordFieldInput;
    private AzurePasswordFieldInput confirmPasswordFieldInput;

    @Getter
    private final Project project;

    public VMCreationDialog(@Nullable Project project) {
        super(project);
        this.project = project;

        $$$setupUI$$$();
        pnlPorts.addActionListenerToComponents(e -> pnlBasicPorts.setValue(pnlPorts.getValue()));
        pnlBasicPorts.addActionListenerToComponents(e -> pnlPorts.setValue(pnlBasicPorts.getValue()));
        init();
    }

    @Override
    protected void init() {
        super.init();

        final ButtonGroup authenticationGroup = new ButtonGroup();
        authenticationGroup.add(rdoPassword);
        authenticationGroup.add(rdoSshPublicKey);
        rdoPassword.addItemListener(e -> toggleAuthenticationType(false));
        rdoSshPublicKey.addItemListener(e -> toggleAuthenticationType(true));
        rdoSshPublicKey.setSelected(true);

        final ButtonGroup securityGroup = new ButtonGroup();
        securityGroup.add(rdoNoneSecurityGroup);
        securityGroup.add(rdoBasicSecurityGroup);
        securityGroup.add(rdoAdvancedSecurityGroup);
        rdoNoneSecurityGroup.addItemListener(e -> toggleSecurityGroup(SecurityGroupPolicy.None));
        rdoBasicSecurityGroup.addItemListener(e -> toggleSecurityGroup(SecurityGroupPolicy.Basic));
        rdoAdvancedSecurityGroup.addItemListener(e -> toggleSecurityGroup(SecurityGroupPolicy.Advanced));
        rdoNoneSecurityGroup.setSelected(true);

        final ButtonGroup inboundPortsGroup = new ButtonGroup();
        inboundPortsGroup.add(rdoNoneInboundPorts);
        inboundPortsGroup.add(rdoAllowSelectedInboundPorts);
        rdoNoneInboundPorts.addItemListener(e -> toggleInboundPortsPolicy(false));
        rdoAllowSelectedInboundPorts.addItemListener(e -> toggleInboundPortsPolicy(true));
        rdoNoneInboundPorts.setSelected(true);

        chkAzureSpotInstance.addItemListener(e -> toggleAzureSpotInstance(chkAzureSpotInstance.isSelected()));
        chkAzureSpotInstance.setSelected(false);

        final ButtonGroup evictionPolicyGroup = new ButtonGroup();
        evictionPolicyGroup.add(rdoStopAndDeallocate);
        evictionPolicyGroup.add(rdoDelete);

        cbSubscription.addItemListener(this::onSubscriptionChanged);
        cbRegion.addItemListener(this::onRegionChanged);
        cbResourceGroup.addItemListener(this::onResourceGroupChanged);
        cbVirtualNetwork.addItemListener(this::onNetworkChanged);
        cbImage.addItemListener(this::onImageChanged);
        txtCertificate.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener(SELECT_CERT_TITLE, SSH_PUBLIC_KEY_DESCRIPTION, txtCertificate,
                project, FileChooserDescriptorFactory.createSingleLocalFileDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT));

        unifyComponentsStyle();
    }

    private void toggleInboundPortsPolicy(boolean allowInboundPorts) {
        pnlPorts.toggleInboundPortsPolicy(allowInboundPorts);
        pnlBasicPorts.toggleInboundPortsPolicy(allowInboundPorts);
    }

    private void onImageChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof AzureImage) {
            final AzureImage image = (AzureImage) e.getItem();
            if (image.getOperatingSystem() == OperatingSystem.Windows) {
                // SSH key was not supported for windows
                rdoSshPublicKey.setVisible(false);
                rdoPassword.setSelected(true);
            } else {
                rdoSshPublicKey.setVisible(true);
            }
        }
    }

    private void onResourceGroupChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof ResourceGroup) {
            final ResourceGroup resourceGroup = (ResourceGroup) e.getItem();
            this.cbVirtualNetwork.setResourceGroup(resourceGroup);
            this.cbPublicIp.setResourceGroup(resourceGroup);
        }
    }

    private void onNetworkChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof Network) {
            final Network network = (Network) e.getItem();
            this.cbSubnet.setNetwork(network);
        }
    }

    private void onRegionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof Region) {
            final Region region = (Region) e.getItem();
            this.cbImage.setRegion(region);
            this.cbSize.setRegion(region);
            this.cbVirtualNetwork.setRegion(region);
            this.cbPublicIp.setRegion(region);
            this.cbSecurityGroup.setRegion(region);
        }
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof Subscription) {
            final Subscription subscription = (Subscription) e.getItem();
            this.cbResourceGroup.setSubscription(subscription);
            this.cbRegion.setSubscription(subscription);
            this.cbImage.setSubscription(subscription);
            this.cbAvailabilityOptions.setSubscription(subscription);
            this.cbSize.setSubscription(subscription);
            this.cbVirtualNetwork.setSubscription(subscription);
            this.cbSecurityGroup.setSubscription(subscription);
            this.cbPublicIp.setSubscription(subscription);
            this.cbStorageAccount.setSubscription(subscription);
        }
    }

    private void unifyComponentsStyle() {
        final List<JLabel> labels = Stream.of(getLabels(), pnlBasicPorts.getLabels(), pnlPorts.getLabels()).flatMap(List::stream).collect(Collectors.toList());
        final int maxWidth = labels.stream().map(JLabel::getPreferredSize).map(Dimension::getWidth).max(Double::compare).map(Double::intValue).get();
        final int maxHeight = labels.stream().map(JLabel::getPreferredSize).map(Dimension::getHeight).max(Double::compare).map(Double::intValue).get();
        labels.forEach(field -> {
            final Dimension dimension = new Dimension(maxWidth, Math.max(maxHeight, cbSecurityGroup.getPreferredSize().height));
            field.setPreferredSize(dimension);
            field.setMinimumSize(dimension);
            field.setMaximumSize(dimension);
        });
    }

    private List<JLabel> getLabels() {
        return Arrays.stream(this.getClass().getDeclaredFields())
                .map(field -> {
                    try {
                        return field.get(this);
                    } catch (final IllegalAccessException e) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .filter(value -> value instanceof JLabel)
                .map(value -> (JLabel) value)
                .collect(Collectors.toList());
    }

    private void toggleAzureSpotInstance(boolean enableAzureSpotInstance) {
        lblEvictionPolicy.setVisible(enableAzureSpotInstance);
        rdoStopAndDeallocate.setVisible(enableAzureSpotInstance);
        rdoDelete.setVisible(enableAzureSpotInstance);
        lblMaximumPrice.setVisible(enableAzureSpotInstance);
        txtMaximumPrice.setVisible(enableAzureSpotInstance);
        txtMaximumPrice.setRequired(enableAzureSpotInstance);
        txtMaximumPrice.setValidator(enableAzureSpotInstance ? this::validateMaximumPricing : null);
        txtMaximumPrice.validateValueAsync(); // trigger revalidate after reset validator
        txtMaximumPrice.onDocumentChanged(); // trigger revalidate after reset validator
    }

    private void toggleSecurityGroup(SecurityGroupPolicy policy) {
        titleInboundPortRules.setVisible(policy == SecurityGroupPolicy.Basic);
        pnlPorts.setVisible(policy == SecurityGroupPolicy.Basic);
        pnlBasicPorts.setVisible(policy == SecurityGroupPolicy.Basic);
        // lblPublicInboundPorts.setVisible(policy == SecurityGroupPolicy.Basic);
        // pnlPublicInboundsRadios.setVisible(policy == SecurityGroupPolicy.Basic);
        lblConfigureSecurityGroup.setVisible(policy == SecurityGroupPolicy.Advanced);
        cbSecurityGroup.setVisible(policy == SecurityGroupPolicy.Advanced);
        cbSecurityGroup.setRequired(policy == SecurityGroupPolicy.Advanced);
    }

    private void toggleAuthenticationType(boolean isSSH) {
        lblPassword.setVisible(!isSSH);
        txtPassword.setVisible(!isSSH);
        lblConfirmPassword.setVisible(!isSSH);
        txtConfirmPassword.setVisible(!isSSH);
        lblCertificate.setVisible(isSSH);
        txtCertificate.setVisible(isSSH);
        txtCertificate.setRequired(isSSH);

        passwordFieldInput.setRequired(!isSSH);
        passwordFieldInput.setValidator(isSSH ? null : this::validatePassword);
        confirmPasswordFieldInput.setRequired(!isSSH);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbSubscription = new SubscriptionComboBox();
        this.cbSubscription.setRequired(true);
        this.cbImage = new VirtualMachineImageComboBox();
        this.cbImage.setRequired(true);
        this.cbSize = new VirtualMachineSizeComboBox();
        this.cbSize.setRequired(true);
        this.cbAvailabilityOptions = new NetworkAvailabilityOptionsComboBox();
        this.cbVirtualNetwork = new VirtualNetworkComboBox();
        this.cbVirtualNetwork.setRequired(true);
        this.cbSubnet = new SubnetComboBox();
        this.cbSubnet.setRequired(true);
        this.cbSecurityGroup = new SecurityGroupComboBox();
        this.cbPublicIp = new PublicIPAddressComboBox();
        this.cbStorageAccount = new AzureStorageAccountComboBox();
        this.txtUserName = new AzureTextInput();
        this.txtUserName.setRequired(true);

        this.txtVisualMachineName = new AzureTextInput();
        this.txtVisualMachineName.setRequired(true);
        this.txtVisualMachineName.setValidator(this::validateVirtualMachineName);

        this.txtMaximumPrice = new AzureTextInput();

        this.txtPassword = new JPasswordField();
        this.passwordFieldInput = new AzurePasswordFieldInput(txtPassword);
        this.txtConfirmPassword = new JPasswordField();
        this.confirmPasswordFieldInput = new AzurePasswordFieldInput(txtConfirmPassword);

        this.cbSubscription.refreshItems();
    }

    @Override
    public AzureForm<DraftVirtualMachine> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return VIRTUAL_MACHINE_CREATION_DIALOG_TITLE;
    }

    @Override
    public DraftVirtualMachine getValue() {
        final Subscription subscription = cbSubscription.getValue();
        final String subscriptionId = Optional.ofNullable(subscription).map(Subscription::getId).orElse(StringUtils.EMPTY);
        final ResourceGroup resourceGroup = cbResourceGroup.getValue();
        final String resourceGroupName = Optional.ofNullable(resourceGroup).map(ResourceGroup::getName).orElse(StringUtils.EMPTY);
        final String vmName = txtVisualMachineName.getText();
        final DraftVirtualMachine draftVirtualMachine = new DraftVirtualMachine();
        draftVirtualMachine.setSubscriptionId(subscriptionId);
        draftVirtualMachine.setResourceGroup(resourceGroupName);
        draftVirtualMachine.setName(vmName);
        draftVirtualMachine.setRegion(cbRegion.getValue());
        draftVirtualMachine.setNetwork(cbVirtualNetwork.getValue());
        draftVirtualMachine.setSubnet(cbSubnet.getValue());
        draftVirtualMachine.setImage(cbImage.getValue());
        draftVirtualMachine.setIpAddress(cbPublicIp.getValue());
        draftVirtualMachine.setUserName(txtUserName.getText());
        if (rdoPassword.isSelected()) {
            draftVirtualMachine.setAuthenticationType(AuthenticationType.Password);
            draftVirtualMachine.setPassword(passwordFieldInput.getValue());
        } else if (rdoSshPublicKey.isSelected()) {
            draftVirtualMachine.setAuthenticationType(AuthenticationType.SSH);
            try {
                draftVirtualMachine.setSshKey(readCert(txtCertificate.getValue()));
            } catch (final IOException e) {
                // swallow exception while get data
            }
        }
        draftVirtualMachine.setSize(cbSize.getValue());
        draftVirtualMachine.setAvailabilitySet(cbAvailabilityOptions.getValue());
        // Azure Spot
        if (chkAzureSpotInstance.isSelected()) {
            final AzureSpotConfig.EvictionType evictionType = AzureSpotConfig.EvictionType.CapacityOnly;
            final AzureSpotConfig.EvictionPolicy evictionPolicy = rdoStopAndDeallocate.isSelected() ?
                    AzureSpotConfig.EvictionPolicy.StopAndDeallocate : AzureSpotConfig.EvictionPolicy.Delete;
            final double maximumPrice = Double.parseDouble(txtMaximumPrice.getText());
            final AzureSpotConfig spotConfig = new AzureSpotConfig(maximumPrice, evictionType, evictionPolicy);
            draftVirtualMachine.setAzureSpotConfig(spotConfig);
        } else {
            draftVirtualMachine.setAzureSpotConfig(null);
        }
        // Security Group
        if (rdoAdvancedSecurityGroup.isSelected()) {
            draftVirtualMachine.setSecurityGroup(cbSecurityGroup.getValue());
        } else if (rdoBasicSecurityGroup.isSelected()) {
            final DraftNetworkSecurityGroup draftNetworkSecurityGroup = new DraftNetworkSecurityGroup();
            draftNetworkSecurityGroup.setSubscriptionId(subscriptionId);
            draftNetworkSecurityGroup.setResourceGroup(resourceGroupName);
            draftNetworkSecurityGroup.setName(vmName + "-sg" + Utils.getTimestamp());
            draftNetworkSecurityGroup.setRegion(cbRegion.getValue());
            draftNetworkSecurityGroup.setSecurityRuleList(pnlPorts.getValue());
            draftVirtualMachine.setSecurityGroup(draftNetworkSecurityGroup);
        }
        draftVirtualMachine.setStorageAccount(cbStorageAccount.getValue());
        return draftVirtualMachine;
    }

    private String readCert(final String certificate) throws IOException {
        byte[] certData = new byte[0];
        if (!certificate.isEmpty()) {
            final File certFile = new File(certificate);
            if (certFile.exists()) {
                try (final FileInputStream certStream = new FileInputStream(certFile)) {
                    certData = new byte[(int) certFile.length()];
                    if (certStream.read(certData) != certData.length) {
                        throw new AzureToolkitRuntimeException("Unable to process certificate: stream longer than informed size.");
                    }
                }
            }
        }
        return new String(certData);
    }

    @Override
    public void setValue(DraftVirtualMachine data) {
        Optional.ofNullable(data.getResourceGroup()).ifPresent(groupName -> cbResourceGroup.setValue(
                new AzureComboBox.ItemReference<>(group -> StringUtils.equalsIgnoreCase(group.getName(), groupName))));
        Optional.ofNullable(data.getSubscriptionId()).ifPresent(id -> cbSubscription.setValue(
                new AzureComboBox.ItemReference<>(subscription -> StringUtils.equalsIgnoreCase(subscription.getId(), id))));
        Optional.ofNullable(data.getName()).ifPresent(name -> txtVisualMachineName.setText(name));
        Optional.ofNullable(data.getRegion()).ifPresent(region -> cbRegion.setValue(region));
        Optional.ofNullable(data.getImage()).ifPresent(image -> cbImage.setValue(image));
        Optional.ofNullable(data.getSize()).ifPresent(size -> cbSize.setValue(size));
        Optional.ofNullable(data.getUserName()).ifPresent(name -> txtUserName.setText(name));
        cbAvailabilityOptions.setValue(data.getAvailabilitySet());
        // skip set value for password/cert
        Optional.ofNullable(data.getNetwork()).ifPresent(network -> cbVirtualNetwork.setData(network));
        Optional.ofNullable(data.getSubnet()).ifPresent(subnet -> cbSubnet.setValue(subnet));
        rdoNoneSecurityGroup.setSelected(data.getSecurityGroup() == null);
        Optional.ofNullable(data.getSecurityGroup()).ifPresent(networkSecurityGroup -> {
            if (networkSecurityGroup instanceof DraftNetworkSecurityGroup) {
                rdoBasicSecurityGroup.setSelected(true);
                final List<SecurityRule> securityRuleList = ((DraftNetworkSecurityGroup) networkSecurityGroup).getSecurityRuleList();
                rdoAllowSelectedInboundPorts.setSelected(!Collections.isEmpty(securityRuleList));
                pnlPorts.setValue(securityRuleList);
                pnlBasicPorts.setValue(securityRuleList);
            } else if (networkSecurityGroup.exists()) {
                rdoAdvancedSecurityGroup.setSelected(true);
                cbSecurityGroup.setData(networkSecurityGroup);
            }
        });
        cbPublicIp.setData(data.getIpAddress());
        cbStorageAccount.setData(data.getStorageAccount());
        final AzureSpotConfig azureSpotConfig = data.getAzureSpotConfig();
        if (azureSpotConfig == null) {
            chkAzureSpotInstance.setSelected(false);
        } else {
            chkAzureSpotInstance.setSelected(true);
            rdoStopAndDeallocate.setSelected(azureSpotConfig.getPolicy() != AzureSpotConfig.EvictionPolicy.StopAndDeallocate);
            rdoDelete.setSelected(azureSpotConfig.getPolicy() == AzureSpotConfig.EvictionPolicy.StopAndDeallocate);
            txtMaximumPrice.setText(String.valueOf(azureSpotConfig.getMaximumPrice()));
        }
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(cbSubscription, cbImage, cbSize, cbAvailabilityOptions, cbVirtualNetwork, cbSubnet, cbSecurityGroup, cbPublicIp, cbStorageAccount,
                txtUserName, txtVisualMachineName, passwordFieldInput, confirmPasswordFieldInput, txtCertificate, txtMaximumPrice);
    }

    @Override
    public List<AzureValidationInfo> validateData() {
        final List<AzureValidationInfo> result = AzureForm.super.validateData();
        if (rdoPassword.isSelected()) {
            final String password = passwordFieldInput.getValue();
            final String confirmPassword = confirmPasswordFieldInput.getValue();
            if (!StringUtils.equals(password, confirmPassword)) {
                result.add(AzureValidationInfo.error("Password and confirm password must match.", confirmPasswordFieldInput));
            }
        }
        return result;
    }

    private AzureValidationInfo validateVirtualMachineName() {
        final String name = txtVisualMachineName.getText();
        if (StringUtils.isEmpty(name) || name.length() > 64) {
            return AzureValidationInfo.error("Invalid virtual machine name. The name must be between 1 and 64 " +
                    "characters long.", txtVisualMachineName);
        }
        if (!name.matches("^[A-Za-z][A-Za-z0-9-]*$") || name.endsWith("-")) {
            return AzureValidationInfo.error("Invalid virtual machine name. The name must start with a letter, " +
                    "contain only letters, numbers, and hyphens, and end with a letter or number.", txtVisualMachineName);
        }
        return AzureValidationInfo.success(txtVisualMachineName);
    }

    private AzureValidationInfo validateMaximumPricing() {
        try {
            final Double number = Double.valueOf(txtMaximumPrice.getValue());
        } catch (final NumberFormatException e) {
            return AzureValidationInfo.error("The value must be a valid number.", txtMaximumPrice);
        }
        return AzureValidationInfo.success(txtMaximumPrice);
    }

    private AzureValidationInfo validatePassword() {
        final String password = passwordFieldInput.getValue();
        if (!password.matches("(?=^.{8,72}$)((?=.*\\d)(?=.*[A-Z])(?=.*[a-z])|(?=.*\\d)(?=.*[^A-Za-z0-9])" +
                "(?=.*[a-z])|(?=.*[^A-Za-z0-9])(?=.*[A-Z])(?=.*[a-z])|(?=.*\\d)(?=.*[A-Z])(?=.*[^A-Za-z0-9]))^.*")) {
            return AzureValidationInfo.error("The password does not conform to complexity requirements. \n" +
                    "It should be at least eight characters long and contain a mixture of upper case, lower case, digits and symbols.", passwordFieldInput);
        }
        return AzureValidationInfo.success(passwordFieldInput);
    }

    enum SecurityGroupPolicy {
        None,
        Basic,
        Advanced
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return rootPane;
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
