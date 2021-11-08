/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.intellij.ui.DocumentAdapter;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.compute.network.DraftNetwork;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.Arrays;
import java.util.List;

public class VirtualNetworkDialog extends AzureDialog<DraftNetwork> implements AzureForm<DraftNetwork> {
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
    public AzureForm<DraftNetwork> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Virtual Network";
    }

    @Override
    public DraftNetwork getValue() {
        final DraftNetwork draftNetwork = new DraftNetwork(subscriptionId, resourceGroup, txtName.getValue());
        draftNetwork.setRegion(region);
        draftNetwork.setAddressSpace(txtAddressSpace.getValue());
        draftNetwork.setSubnet(txtSubnetName.getValue());
        draftNetwork.setSubnetAddressSpace(txtSubnetAddressRange.getValue());
        return draftNetwork;
    }

    @Override
    public void setValue(DraftNetwork data) {
        txtName.setText(data.getName());
        txtAddressSpace.setText(data.getAddressSpace());
        txtSubnetName.setText(data.getSubnet());
        txtSubnetAddressRange.setText(data.getSubnetAddressSpace());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtAddressSpace, txtName, txtSubnetAddressRange, txtSubnetName);
    }

    private void createUIComponents() {
        // todo: add name validator
        txtName = new AzureTextInput();
        txtName.setRequired(true);
        txtSubnetName = new AzureTextInput();
        txtSubnetName.setRequired(true);

        txtAddressSpace = new AzureTextInput();
        txtAddressSpace.setRequired(true);
        txtAddressSpace.setValidator(() -> validateSubnet(txtAddressSpace.getValue()));
        txtAddressSpace.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                lblAddressSpaceDetails.setText(getSubnetInfo(txtAddressSpace.getValue()));
            }
        });

        txtSubnetAddressRange = new AzureTextInput();
        txtSubnetAddressRange.setRequired(true);
        txtSubnetAddressRange.setValidator(() -> validateSubnet(txtSubnetAddressRange.getValue()));
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

    private AzureValidationInfo validateSubnet(final String cidrNotation) {
        try {
            new SubnetUtils(cidrNotation);
            return AzureValidationInfo.success(this);
        } catch (final IllegalArgumentException iae) {
            return AzureValidationInfo.builder().type(AzureValidationInfo.Type.ERROR).message(iae.getMessage()).build();
        }
    }
}
