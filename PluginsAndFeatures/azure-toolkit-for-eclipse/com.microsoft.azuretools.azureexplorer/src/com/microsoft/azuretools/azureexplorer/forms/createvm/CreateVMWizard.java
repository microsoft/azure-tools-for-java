/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azuretools.azureexplorer.forms.createvm;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_VM;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.VM;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineImage;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import com.microsoft.tooling.msservices.model.vm.VirtualNetwork;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.wizard.Wizard;

public class CreateVMWizard extends Wizard implements TelemetryProperties {
    private VMArmModule node;

    protected SubscriptionDetail subscription;
    protected String name;
    protected String userName;
    protected String password;
    protected String certificate;
    protected String subnet;
    protected VirtualMachineSize size;

    private Location region;
    private Network virtualNetwork;
    private VirtualNetwork newNetwork;
    private boolean isNewNetwork;
    private String resourceGroupName;
    private boolean isNewResourceGroup;
    private VirtualMachineImage virtualMachineImage;
    private Object knownMachineImage;
    private boolean isKnownMachineImage;
    private StorageAccount storageAccount;
    private com.microsoft.tooling.msservices.model.storage.StorageAccount newStorageAccount;
    private boolean withNewStorageAccount;
    private PublicIPAddress publicIpAddress;
    private boolean withNewPip;
    private AvailabilitySet availabilitySet;
    private boolean withNewAvailabilitySet;
    private NetworkSecurityGroup networkSecurityGroup;

    private Azure azure;

    public CreateVMWizard(VMArmModule node) {
        this.node = node;
        setWindowTitle("Create new Virtual Machine");
    }

    @Override
    public void addPages() {
        addPage(new SubscriptionStep(this));
        addPage(new SelectImageStep(this));
        addPage(new MachineSettingsStep(this));
        addPage(new SettingsStep(this));
    }

    @Override
    public boolean performFinish() {
        Operation operation = TelemetryManager.createOperation(VM, CREATE_VM);
        DefaultLoader.getIdeHelper().runInBackground(null, "Creating virtual machine " + name + "...", false, true,
            "Creating virtual machine " + name + "...", new Runnable() {
            @Override
            public void run() {
                try {
                    operation.start();
                    byte[] certData = new byte[0];
                    if (!certificate.isEmpty()) {
                        File certFile = new File(certificate);
                        if (certFile.exists()) {
                            try (FileInputStream certStream = new FileInputStream(certFile)){
                                certData = new byte[(int) certFile.length()];
                                if (certStream.read(certData) != certData.length) {
                                    throw new Exception("Unable to process certificate: "
                                        + "stream longer than informed size.");
                                }
                            } finally {
                            }
                        }
                    }

                    VirtualMachine vm = AzureSDKManager.createVirtualMachine(subscription.getSubscriptionId(),
                            name,
                            resourceGroupName,
                            isNewResourceGroup,
                            size.name(),
                            region.name(),
                            virtualMachineImage,
                            knownMachineImage,
                            isKnownMachineImage,
                            storageAccount,
                            newStorageAccount,
                            withNewStorageAccount,
                            virtualNetwork,
                            newNetwork,
                            isNewNetwork,
                            subnet,
                            publicIpAddress,
                            withNewPip,
                            availabilitySet,
                            withNewAvailabilitySet,
                            userName,
                            password,
                            certData.length > 0 ? new String(certData) : null);
                    // update resource groups cache if new resource group was created when creating storage account
                    ResourceGroup rg = null;
                    if (isNewResourceGroup) {
                        rg = azure.resourceGroups().getByName(resourceGroupName);
                        AzureModelController.addNewResourceGroup(subscription, rg);
                    }
                    if (withNewStorageAccount && newStorageAccount.isNewResourceGroup() &&
                            (rg == null ||!rg.name().equals(newStorageAccount.getResourceGroupName()))) {
                        rg = azure.resourceGroups().getByName(newStorageAccount.getResourceGroupName());
                        AzureModelController.addNewResourceGroup(subscription, rg);
                    }
//                    virtualMachine = AzureManagerImpl.getManager().refreshVirtualMachineInformation(virtualMachine);
                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                node.addChildNode(new VMNode(node, subscription.getSubscriptionId(), vm));
                            } catch (AzureCmdException e) {
                                PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err,
                                        "An error occurred while refreshing the list of virtual machines.", e);
                            }
                        }
                    });
                } catch (Exception e) {
                    EventUtil.logError(operation, ErrorType.userError, e, null, null);
                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                        public void run() {
                            PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), "Error Creating Virtual Machine", "An error occurred while trying to create the specified virtual machine", e);
                        }
                    });
                } finally {
                    operation.complete();
                }
            }
        });
        return true;
    }

    @Override
    public boolean canFinish() {
        return getContainer().getCurrentPage() instanceof SettingsStep;
    }

    public Azure getAzure() {
        return azure;
    }

    public void setAzure(Azure azure) {
        this.azure = azure;
    }

    public void setSubscription(SubscriptionDetail subscription) {
        try {
            this.subscription = subscription;
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            azure = azureManager.getAzure(subscription.getSubscriptionId());
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().showException(ex.getMessage(), ex, "Error selecting subscription", true, false);
        }
    }

    public SubscriptionDetail getSubscription() {
        return subscription;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public Location getRegion() {
        return region;
    }

    public void setRegion(Location region) {
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

    public boolean isNewNetwork() {
        return isNewNetwork;
    }

    public void setNewNetwork(boolean isNewNetwork) {
        this.isNewNetwork = isNewNetwork;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public boolean isNewResourceGroup() {
        return isNewResourceGroup;
    }

    public void setNewResourceGroup(boolean isNewResourceGroup) {
        this.isNewResourceGroup = isNewResourceGroup;
    }

    public VirtualMachineImage getVirtualMachineImage() {
        return virtualMachineImage;
    }

    public void setVirtualMachineImage(VirtualMachineImage virtualMachineImage) {
        this.virtualMachineImage = virtualMachineImage;
    }

    public Object getKnownMachineImage() {
        return knownMachineImage;
    }

    public void setKnownMachineImage(Object knownMachineImage) {
        this.knownMachineImage = knownMachineImage;
    }

    public boolean isKnownMachineImage() {
        return isKnownMachineImage;
    }

    public void setKnownMachineImage(boolean isKnownMachineImage) {
        this.isKnownMachineImage = isKnownMachineImage;
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

    public VirtualMachineSize getSize() {
        return size;
    }

    public void setSize(VirtualMachineSize size) {
        this.size = size;
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        if(this.getSubnet() != null) properties.put("Size", this.getSubnet());
        if(this.getSubscription() != null) {
            properties.put("SubscriptionName", this.getSubscription().getSubscriptionName());
            properties.put("SubscriptionId", this.getSubscription().getSubscriptionId());
        }
        if(this.getName() != null) properties.put("Name", this.getName());
        if(this.getRegion() != null) properties.put("Region", this.getRegion().displayName());
        if(this.getSize() != null) properties.put("Size", this.getSize().name());

        return properties;
    }


}
