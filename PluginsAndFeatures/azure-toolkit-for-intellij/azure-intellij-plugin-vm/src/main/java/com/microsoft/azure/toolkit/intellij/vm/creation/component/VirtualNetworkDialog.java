/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.intellij.ui.DocumentAdapter;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.network.AzureNetwork;
import com.microsoft.azure.toolkit.lib.network.virtualnetwork.NetworkDraft;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.Arrays;
import java.util.List;

public class VirtualNetworkDialog extends AzureDialog<NetworkDraft> implements AzureForm<NetworkDraft> {
    private static final String NETWORK_NAME_RULES = "The name must begin with a letter or number, end with a letter, number or underscore, " +
        "and may contain only letters, numbers, underscores, periods, or hyphens.";
    private static final String NETWORK_NAME_PATTERN = "[a-zA-Z0-9][a-zA-Z0-9_\\.-]{0,62}[a-zA-Z0-9_]";
    private static final String SUBNET_NAME_PATTERN = "[a-zA-Z0-9]([a-zA-Z0-9_\\.-]{0,78}[a-zA-Z0-9_])?";

    private JPanel contentPane;
    private AzureTextInput txtName;
    private JLabel lblName;
    private JLabel lblAddressSpace;
    private JLabel lblAddressSpaceDetails;
    private JLabel lblSubnetName;
    private JLabel lblSubNetAddress;
    private JLabel lblSubnetAddressDetails;
    private AzureTextInput txtAddressSpace;
    private AzureTextInput txtSubnetName;
    private AzureTextInput txtSubnetAddressRange;

    private final String subscriptionId;
    private final String resourceGroup;
    private final Region region;

    public VirtualNetworkDialog(String subscriptionId, String resourceGroup, Region region) {
        super();
        this.subscriptionId = subscriptionId;
        this.resourceGroup = resourceGroup;
        this.region = region;
        setModal(true);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    public AzureForm<NetworkDraft> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Virtual Network";
    }

    @Override
    public NetworkDraft getValue() {
        final NetworkDraft draft = Azure.az(AzureNetwork.class).virtualNetworks(subscriptionId).create(txtName.getValue(), resourceGroup);
        draft.setRegion(region);
        draft.setAddressSpace(txtAddressSpace.getValue());
        draft.setSubnet(txtSubnetName.getValue(), txtSubnetAddressRange.getValue());
        return draft;
    }

    @Override
    public void setValue(NetworkDraft data) {
        txtName.setText(data.getName());
        txtAddressSpace.setText(data.getAddressSpace());
        txtSubnetName.setText(data.getSubnet().getName());
        txtSubnetAddressRange.setText(data.getSubnet().getAddressSpace());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtName, txtAddressSpace, txtSubnetAddressRange, txtSubnetName);
    }

    private void createUIComponents() {
        // todo: add name validator
        txtName = new AzureTextInput();
        txtName.setRequired(true);
        txtName.addValidator(this::validateVirtualNetworkName);
        txtSubnetName = new AzureTextInput();
        txtSubnetName.setRequired(true);
        txtSubnetName.addValidator(this::validateSubnetName);

        txtAddressSpace = new AzureTextInput();
        txtAddressSpace.setRequired(true);
        txtAddressSpace.addValidator(() -> validateSubnet(txtAddressSpace.getValue(), txtAddressSpace));
        txtAddressSpace.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                lblAddressSpaceDetails.setText(getSubnetInfo(txtAddressSpace.getValue()));
            }
        });

        txtSubnetAddressRange = new AzureTextInput();
        txtSubnetAddressRange.setRequired(true);
        txtSubnetAddressRange.addValidator(() -> validateSubnet(txtSubnetAddressRange.getValue(), txtSubnetAddressRange));
        txtSubnetAddressRange.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                lblSubnetAddressDetails.setText(getSubnetInfo(txtSubnetAddressRange.getValue()));
            }
        });
    }

    private String getSubnetInfo(final String cidrNotation) {
        try {
            final SubnetUtils subnetUtils = new SubnetUtils(cidrNotation);
            subnetUtils.setInclusiveHostCount(true);
            final SubnetUtils.SubnetInfo info = subnetUtils.getInfo();
            return String.format("%s-%s (%s addresses)", info.getLowAddress(), info.getHighAddress(), info.getAddressCountLong());
        } catch (final IllegalArgumentException iae) {
            return StringUtils.EMPTY;
        }
    }

    private AzureValidationInfo validateVirtualNetworkName() {
        final String virtualNetworkName = txtName.getValue();
        if (StringUtils.isEmpty(virtualNetworkName) || virtualNetworkName.length() < 2 || virtualNetworkName.length() > 64) {
            return AzureValidationInfo.error("The name must be between 2 and 64 characters.", txtName);
        } else if (!virtualNetworkName.matches(NETWORK_NAME_PATTERN)) {
            return AzureValidationInfo.error(NETWORK_NAME_RULES, txtName);
        }
        return AzureValidationInfo.ok(txtName);
    }

    private AzureValidationInfo validateSubnetName() {
        final String subNetName = txtSubnetName.getValue();
        if (StringUtils.isEmpty(subNetName) || subNetName.length() > 80) {
            return AzureValidationInfo.error("The name must be between 1 and 80 characters.", txtSubnetName);
        } else if (!subNetName.matches(SUBNET_NAME_PATTERN)) {
            return AzureValidationInfo.error(NETWORK_NAME_RULES, txtSubnetName);
        }
        return AzureValidationInfo.success(txtSubnetName);
    }

    private AzureValidationInfo validateSubnet(final String cidrNotation, final AzureFormInput<?> input) {
        try {
            new SubnetUtils(cidrNotation);
            return AzureValidationInfo.success(input);
        } catch (final IllegalArgumentException iae) {
            return AzureValidationInfo.error(iae.getMessage(), input);
        }
    }
}
