/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.wizard.WizardModel;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.compute.VirtualMachineImage;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.azure.toolkit.intellij.vm.createarmvm.MachineSettingsStep;
import com.microsoft.azure.toolkit.intellij.vm.createarmvm.SelectImageStep;
import com.microsoft.azure.toolkit.intellij.vm.createarmvm.SettingsStep;
import com.microsoft.azure.toolkit.intellij.vm.createarmvm.SubscriptionStep;
import com.microsoft.tooling.msservices.model.vm.VirtualNetwork;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;

public class VMWizardModel extends WizardModel implements TelemetryProperties {
    private Region region;
    private VirtualMachineImage virtualMachineImage;
    private boolean isKnownMachineImage;
    private Object knownMachineImage;
    private Network virtualNetwork;
    private VirtualNetwork newNetwork;
    private boolean withNewNetwork;
    private StorageAccount storageAccount;
    private com.microsoft.tooling.msservices.model.storage.StorageAccount newStorageAccount;
    private boolean withNewStorageAccount;
    //    private String availabilitySet;
    private PublicIPAddress publicIpAddress;
    private boolean withNewPip;
    private NetworkSecurityGroup networkSecurityGroup;
    private AvailabilitySet availabilitySet;
    private boolean withNewAvailabilitySet;
    private String name;
    private String size;
    private String userName;
    private String password;
    private String certificate;
    private String subnet;
    private Subscription subscription;

    public VMWizardModel(VMArmModule node) {
        super(ApplicationNamesInfo.getInstance().getFullProductName() + " - Create new Virtual Machine");
        Project project = (Project) node.getProject();

        add(new SubscriptionStep(this, project));
        add(new SelectImageStep(this, project));
        add(new MachineSettingsStep(this, project));
        add(new SettingsStep(this, project, node));
    }

    public String[] getStepTitleList() {
        return new String[]{
            "Subscription",
            "Select Image",
            "Machine Settings",
            "Associated Resources"
        };
    }

    public void configStepList(JList list, int step) {

        list.setListData(getStepTitleList());
        list.setSelectedIndex(step);
        list.setBorder(new EmptyBorder(10, 0, 10, 0));

        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList mylist, Object o, int i, boolean b, boolean b1) {
                return super.getListCellRendererComponent(mylist, "  " + o.toString(), i, b, b1);
            }
        });

        for (MouseListener mouseListener : list.getMouseListeners()) {
            list.removeMouseListener(mouseListener);
        }

        for (MouseMotionListener mouseMotionListener : list.getMouseMotionListeners()) {
            list.removeMouseMotionListener(mouseMotionListener);
        }
        list.setBackground(JBColor.background());
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public VirtualMachineImage getVirtualMachineImage() {
        return virtualMachineImage;
    }

    public void setVirtualMachineImage(VirtualMachineImage virtualMachineImage) {
        this.virtualMachineImage = virtualMachineImage;
    }

    public boolean isKnownMachineImage() {
        return isKnownMachineImage;
    }

    public void setKnownMachineImage(boolean knownMachineImage) {
        this.isKnownMachineImage = knownMachineImage;
    }

    public Object getKnownMachineImage() {
        return knownMachineImage;
    }

    public void setKnownMachineImage(Object knownMachineImage) {
        this.knownMachineImage = knownMachineImage;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Network getVirtualNetwork() {
        return virtualNetwork;
    }

    public void setVirtualNetwork(Network virtualNetwork) {
        this.virtualNetwork = virtualNetwork;
    }

    public VirtualNetwork getNewNetwork() {
        return newNetwork;
    }

    public void setNewNetwork(VirtualNetwork newNetwork) {
        this.newNetwork = newNetwork;
    }

    public boolean isWithNewNetwork() {
        return withNewNetwork;
    }

    public void setWithNewNetwork(boolean withNewNetwork) {
        this.withNewNetwork = withNewNetwork;
    }

    public StorageAccount getStorageAccount() {
        return storageAccount;
    }

    public void setStorageAccount(StorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public com.microsoft.tooling.msservices.model.storage.StorageAccount getNewStorageAccount() {
        return newStorageAccount;
    }

    public void setNewStorageAccount(com.microsoft.tooling.msservices.model.storage.StorageAccount newStorageAccount) {
        this.newStorageAccount = newStorageAccount;
    }

    public boolean isWithNewStorageAccount() {
        return withNewStorageAccount;
    }

    public void setWithNewStorageAccount(boolean withNewStorageAccount) {
        this.withNewStorageAccount = withNewStorageAccount;
    }

    public PublicIPAddress getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(PublicIPAddress publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public boolean isWithNewPip() {
        return withNewPip;
    }

    public void setWithNewPip(boolean withNewPip) {
        this.withNewPip = withNewPip;
    }

    public NetworkSecurityGroup getNetworkSecurityGroup() {
        return networkSecurityGroup;
    }

    public void setNetworkSecurityGroup(NetworkSecurityGroup networkSecurityGroup) {
        this.networkSecurityGroup = networkSecurityGroup;
    }

    public AvailabilitySet getAvailabilitySet() {
        return availabilitySet;
    }

    public void setAvailabilitySet(AvailabilitySet availabilitySet) {
        this.availabilitySet = availabilitySet;
    }

    public boolean isWithNewAvailabilitySet() {
        return withNewAvailabilitySet;
    }

    public void setWithNewAvailabilitySet(boolean withNewAvailabilitySet) {
        this.withNewAvailabilitySet = withNewAvailabilitySet;
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        if (this.getSize() != null) properties.put("Size", this.getSize());
        if (this.getSubnet() != null) properties.put("Subnet", this.getSubnet());
        if (this.getSubscription() != null) {
            properties.put("SubscriptionName", this.getSubscription().getName());
            properties.put("SubscriptionId", this.getSubscription().getId());
        }
        if (this.getName() != null) properties.put("Name", this.getName());
        if (this.getRegion() != null) properties.put("Region", this.getRegion().getName());
        return properties;
    }
}
