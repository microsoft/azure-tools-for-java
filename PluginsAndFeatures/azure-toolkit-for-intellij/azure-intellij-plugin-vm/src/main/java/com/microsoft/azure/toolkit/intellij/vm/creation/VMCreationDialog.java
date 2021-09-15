package com.microsoft.azure.toolkit.intellij.vm.creation;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.component.AzureFileInput;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.storage.component.AzureStorageAccountComboBox;
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
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.compute.network.Network;
import com.microsoft.azure.toolkit.lib.compute.security.DraftNetworkSecurityGroup;
import com.microsoft.azure.toolkit.lib.compute.security.model.SecurityRule;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureImage;
import com.microsoft.azure.toolkit.lib.compute.vm.DraftVirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.vm.model.AuthenticationType;
import com.microsoft.azure.toolkit.lib.compute.vm.model.AzureSpotConfig;
import com.microsoft.azure.toolkit.lib.compute.vm.model.OperatingSystem;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VMCreationDialog extends AzureDialog<DraftVirtualMachine> implements AzureForm<DraftVirtualMachine> {
    public static final String SSH_PUBLIC_KEY_DESCRIPTION = "<html> Provide an RSA public key file in the single-line format (starting with \"ssh-rsa\") or " +
            "the multi-line PEM format. <p/> You can generate SSH keys using ssh-keygen on Linux and OS X, or PuTTYGen on Windows. </html>";
    public static final String SELECT_CERT_TITLE = "Select Cert for Your VM";
    private JTabbedPane tabbedPane;
    private JPanel rootPane;
    private JPanel basicPane;
    private JLabel lblResourceGroup;
    private JLabel lblSubscription;
    private JLabel lblVirtualMachineName;
    private JTextField txtVisualMachineName;
    private JLabel lblRegion;
    private JRadioButton rdoSshPublicKey;
    private JRadioButton rdoPassword;
    private JTextField txtUserName;
    private JRadioButton rdoNoneSecurityGroup;
    private JRadioButton rdoBasicSecurityGroup;
    private JRadioButton rdoAdvancedSecurityGroup;
    private JRadioButton rdoNoneInboundPorts;
    private JRadioButton rdoAllowSelectedInboundPorts;
    private JCheckBox chkAzureSpotInstance;
    private JRadioButton rdoCapacityOnly;
    private JRadioButton rdoPriceOrCapacity;
    private JRadioButton rdoStopAndDeallocate;
    private JRadioButton rdoDelete;
    private JTextField txtMaximumPrice;
    private JLabel lblUserName;
    private JTextField txtPassword;
    private JTextField txtConfirmPassword;
    private JLabel lblPassword;
    private JLabel lblConfirmPassword;
    private JLabel lblCertificate;
    private AzureFileInput txtCertificate;
    private JLabel lblPublicInboundPorts;
    private JLabel lblSelectInboundPorts;
    private JLabel lblConfigureSecurityGroup;
    private SecurityGroupComboBox cbSecurityGroup;
    private JLabel lblEvictionType;
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
    private JPanel pnlPublicInboundsRadios;
    private VirtualMachineSizeComboBox cbSize;
    private SubscriptionComboBox cbSubscription;
    private ResourceGroupComboBox cbResourceGroup;
    private RegionComboBox cbRegion;
    private VirtualMachineImageComboBox cbImage;
    private NetworkAvailabilityOptionsComboBox cbAvailabilityOptions;
    private VirtualNetworkComboBox cbVirtualNetwork;
    private SubnetComboBox cbSubnet;
    private JPanel pnlInboundPorts;
    private JCheckBox chkHTTP;
    private JCheckBox chkHTTPS;
    private JCheckBox chkSSH;
    private JCheckBox chkRDP;
    private PublicIPAddressComboBox cbPublicIp;
    private AzureStorageAccountComboBox cbStorageAccount;
    private JPanel networkPane;
    private JPanel advancedPane;

    @Getter
    private final Project project;

    public VMCreationDialog(@Nullable Project project) {
        super(project);
        this.project = project;

        $$$setupUI$$$();

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

        final ButtonGroup inboundPortsGroup = new ButtonGroup();
        inboundPortsGroup.add(rdoNoneInboundPorts);
        inboundPortsGroup.add(rdoAllowSelectedInboundPorts);
        rdoNoneInboundPorts.addItemListener(e -> toggleInboundPortsPolicy(false));
        rdoAllowSelectedInboundPorts.addItemListener(e -> toggleInboundPortsPolicy(true));
        rdoNoneInboundPorts.setSelected(true);

        final ButtonGroup securityGroup = new ButtonGroup();
        securityGroup.add(rdoNoneSecurityGroup);
        securityGroup.add(rdoBasicSecurityGroup);
        securityGroup.add(rdoAdvancedSecurityGroup);
        rdoNoneSecurityGroup.addItemListener(e -> toggleSecurityGroup(SecurityGroupPolicy.None));
        rdoBasicSecurityGroup.addItemListener(e -> toggleSecurityGroup(SecurityGroupPolicy.Basic));
        rdoAdvancedSecurityGroup.addItemListener(e -> toggleSecurityGroup(SecurityGroupPolicy.Advanced));
        rdoNoneSecurityGroup.setSelected(true);

        chkAzureSpotInstance.addItemListener(e -> toggleAzureSpotInstance(chkAzureSpotInstance.isSelected()));
        chkAzureSpotInstance.setSelected(false);

        final ButtonGroup evictionTypeGroup = new ButtonGroup();
        evictionTypeGroup.add(rdoCapacityOnly);
        evictionTypeGroup.add(rdoPriceOrCapacity);

        final ButtonGroup evictionPolicyGroup = new ButtonGroup();
        evictionPolicyGroup.add(rdoStopAndDeallocate);
        evictionPolicyGroup.add(rdoDelete);

        cbSubscription.addItemListener(this::onSubscriptionChanged);
        cbRegion.addItemListener(this::onRegionChanged);
        cbResourceGroup.addItemListener(this::onResourceGroupChanged);
        cbVirtualNetwork.addItemListener(this::onNetworkChanged);
        cbImage.addItemListener(this::onImageChanged);
        txtCertificate.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener(SELECT_CERT_TITLE, SSH_PUBLIC_KEY_DESCRIPTION, txtCertificate, project, FileChooserDescriptorFactory.createSingleLocalFileDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT));
        unifyComponentsStyle();
    }

    private void toggleInboundPortsPolicy(boolean allowInboundPorts) {
        Stream.of(chkRDP, chkHTTP, chkHTTPS, chkSSH).forEach(chk -> chk.setEnabled(allowInboundPorts));
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
        final List<JLabel> collect = Arrays.stream(this.getClass().getDeclaredFields())
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
        final int maxWidth = collect.stream().map(JLabel::getPreferredSize).map(Dimension::getWidth).max(Double::compare).map(Double::intValue).get();
        final int maxHeight = collect.stream().map(JLabel::getPreferredSize).map(Dimension::getHeight).max(Double::compare).map(Double::intValue).get();
        collect.forEach(field -> {
            final Dimension dimension = new Dimension(maxWidth, Math.max(maxHeight, cbSecurityGroup.getPreferredSize().height));
            field.setPreferredSize(dimension);
            field.setMinimumSize(dimension);
            field.setMaximumSize(dimension);
        });
    }

    private void toggleAzureSpotInstance(boolean enableAzureSpotInstance) {
        lblEvictionType.setVisible(enableAzureSpotInstance);
        rdoCapacityOnly.setVisible(enableAzureSpotInstance);
        rdoPriceOrCapacity.setVisible(enableAzureSpotInstance);
        lblEvictionPolicy.setVisible(enableAzureSpotInstance);
        rdoStopAndDeallocate.setVisible(enableAzureSpotInstance);
        rdoDelete.setVisible(enableAzureSpotInstance);
        lblMaximumPrice.setVisible(enableAzureSpotInstance);
        txtMaximumPrice.setVisible(enableAzureSpotInstance);
    }

    private void toggleSecurityGroup(SecurityGroupPolicy policy) {
        lblPublicInboundPorts.setVisible(policy == SecurityGroupPolicy.Basic);
        pnlPublicInboundsRadios.setVisible(policy == SecurityGroupPolicy.Basic);
        rdoNoneInboundPorts.setVisible(policy == SecurityGroupPolicy.Basic);
        rdoAllowSelectedInboundPorts.setVisible(policy == SecurityGroupPolicy.Basic);
        lblSelectInboundPorts.setVisible(policy == SecurityGroupPolicy.Basic);
        pnlInboundPorts.setVisible(policy == SecurityGroupPolicy.Basic);
        lblConfigureSecurityGroup.setVisible(policy == SecurityGroupPolicy.Advanced);
        cbSecurityGroup.setVisible(policy == SecurityGroupPolicy.Advanced);
    }

    private void toggleAuthenticationType(boolean isSSH) {
        lblPassword.setVisible(!isSSH);
        txtPassword.setVisible(!isSSH);
        lblConfirmPassword.setVisible(!isSSH);
        txtConfirmPassword.setVisible(!isSSH);
        lblCertificate.setVisible(isSSH);
        txtCertificate.setVisible(isSSH);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbSubscription = new SubscriptionComboBox();
        this.cbImage = new VirtualMachineImageComboBox();
        this.cbSize = new VirtualMachineSizeComboBox();
        this.cbAvailabilityOptions = new NetworkAvailabilityOptionsComboBox();
        this.cbVirtualNetwork = new VirtualNetworkComboBox();
        this.cbSubnet = new SubnetComboBox();
        this.cbSecurityGroup = new SecurityGroupComboBox();
        this.cbPublicIp = new PublicIPAddressComboBox();
        this.cbStorageAccount = new AzureStorageAccountComboBox();

        this.cbSubscription.refreshItems();
    }

    @Override
    public AzureForm<DraftVirtualMachine> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Virtual Machine";
    }

    @Override
    public DraftVirtualMachine getData() {
        final Subscription subscription = cbSubscription.getValue();
        final String subscriptionId = Optional.ofNullable(subscription).map(Subscription::getId).orElse(StringUtils.EMPTY);
        final ResourceGroup resourceGroup = cbResourceGroup.getValue();
        final String resourceGroupName = Optional.ofNullable(resourceGroup).map(ResourceGroup::getName).orElse(StringUtils.EMPTY);
        final String vmName = txtVisualMachineName.getText();
        final DraftVirtualMachine draftVirtualMachine = new DraftVirtualMachine(subscriptionId, resourceGroupName, vmName);
        draftVirtualMachine.setRegion(cbRegion.getValue());
        draftVirtualMachine.setNetwork(cbVirtualNetwork.getValue());
        draftVirtualMachine.setSubnet(cbSubnet.getValue());
        draftVirtualMachine.setImage(cbImage.getValue());
        draftVirtualMachine.setIpAddress(cbPublicIp.getValue());
        draftVirtualMachine.setUserName(txtUserName.getText());
        if (rdoPassword.isSelected()) {
            draftVirtualMachine.setAuthenticationType(AuthenticationType.Password);
            draftVirtualMachine.setPassword(txtPassword.getText());
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
            final AzureSpotConfig.EvictionType evictionType = rdoCapacityOnly.isSelected() ?
                    AzureSpotConfig.EvictionType.CapacityOnly : AzureSpotConfig.EvictionType.PriceOrCapacity;
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
            final DraftNetworkSecurityGroup draftNetworkSecurityGroup = new DraftNetworkSecurityGroup(subscriptionId, resourceGroupName, vmName + "-sg");
            draftNetworkSecurityGroup.setRegion(cbRegion.getValue());
            final List<SecurityRule> policies = new ArrayList<>();
            if (chkHTTP.isSelected()) {
                policies.add(SecurityRule.HTTP_RULE);
            }
            if (chkHTTPS.isSelected()) {
                policies.add(SecurityRule.HTTPS_RULE);
            }
            if (chkSSH.isSelected()) {
                policies.add(SecurityRule.SSH_RULE);
            }
            if (chkRDP.isSelected()) {
                policies.add(SecurityRule.RDP_RULE);
            }
            draftNetworkSecurityGroup.setSecurityRuleList(policies);
            draftVirtualMachine.setSecurityGroup(draftNetworkSecurityGroup);
        }
        // todo: Implement storage account related logic, as currently we did not implement none storage
        // draftVirtualMachine.setStorageAccount(cbStorageAccount.getValue());
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
    public void setData(DraftVirtualMachine data) {
        Optional.ofNullable(data.getResourceGroup()).ifPresent(groupName -> cbResourceGroup.setValue(
                new AzureComboBox.ItemReference<>(group -> StringUtils.equalsIgnoreCase(group.getName(), groupName))));
        Optional.ofNullable(data.getSubscriptionId()).ifPresent(id -> cbSubscription.setValue(
                new AzureComboBox.ItemReference<>(subscription -> StringUtils.equalsIgnoreCase(subscription.getId(), id))));
        Optional.ofNullable(data.getName()).ifPresent(name -> txtVisualMachineName.setText(name));
        Optional.ofNullable(data.getRegion()).ifPresent(region -> cbRegion.setValue(region));
        Optional.ofNullable(data.getImage()).ifPresent(image -> cbImage.setValue(image));
        Optional.ofNullable(data.getSize()).ifPresent(size -> cbSize.setValue(size));
        Optional.ofNullable(data.getUserName()).ifPresent(name -> txtUserName.setText(name));
        // skip set value for password/cert
        Optional.ofNullable(data.getNetwork()).ifPresent(network -> cbVirtualNetwork.setValue(network));
        Optional.ofNullable(data.getSubnet()).ifPresent(subnet -> cbSubnet.setValue(subnet));
        Optional.ofNullable(data.getSecurityGroup()).ifPresent(networkSecurityGroup -> {
            if (networkSecurityGroup.exists()) {
                cbSecurityGroup.setValue(networkSecurityGroup);
            } else if (networkSecurityGroup instanceof DraftNetworkSecurityGroup) {
                final List<SecurityRule> securityRuleList = ((DraftNetworkSecurityGroup) networkSecurityGroup).getSecurityRuleList();
                if (CollectionUtils.isEmpty(securityRuleList)) {
                    rdoNoneInboundPorts.setSelected(true);
                } else {
                    rdoAllowSelectedInboundPorts.setSelected(true);
                    securityRuleList.forEach(rule -> {
                        if (rule == SecurityRule.HTTP_RULE) {
                            chkHTTP.setSelected(true);
                        }
                        if (rule == SecurityRule.HTTPS_RULE) {
                            chkHTTPS.setSelected(true);
                        }
                        if (rule == SecurityRule.SSH_RULE) {
                            chkSSH.setSelected(true);
                        }
                        if (rule == SecurityRule.RDP_RULE) {
                            chkRDP.setSelected(true);
                        }
                    });
                }
            }
        });
        // todo: Implement storage account related logic
        cbAvailabilityOptions.setValue(data.getAvailabilitySet());
        cbPublicIp.setValue(data.getIpAddress());
        final AzureSpotConfig azureSpotConfig = data.getAzureSpotConfig();
        if (azureSpotConfig == null) {
            chkAzureSpotInstance.setSelected(false);
        } else {
            rdoCapacityOnly.setSelected(azureSpotConfig.getType() == AzureSpotConfig.EvictionType.CapacityOnly);
            rdoPriceOrCapacity.setSelected(azureSpotConfig.getType() == AzureSpotConfig.EvictionType.PriceOrCapacity);
            rdoStopAndDeallocate.setSelected(azureSpotConfig.getPolicy() == AzureSpotConfig.EvictionPolicy.StopAndDeallocate);
            rdoDelete.setSelected(azureSpotConfig.getPolicy() == AzureSpotConfig.EvictionPolicy.Delete);
            txtMaximumPrice.setText(String.valueOf(azureSpotConfig.getMaximumPrice()));
        }
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(cbSubscription, cbImage, cbSize, cbAvailabilityOptions, cbVirtualNetwork, cbSubnet, cbSecurityGroup, cbPublicIp, cbStorageAccount);
    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        final List<ValidationInfo> result = super.doValidateAll();
        // validate password
        final String password = txtPassword.getText();
        final String confirmPassword = txtConfirmPassword.getText();
        if (!StringUtils.equals(password, confirmPassword)) {
            result.add(new ValidationInfo("Password and confirm password must match.", txtConfirmPassword));
        }
        return result;
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

    private void $$$setupUI$$$() {
    }
}
