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

package com.microsoft.intellij.wizards.createarmvm;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.wizard.WizardNavigationState;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.SkuName;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskRunner;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.forms.CreateArmStorageAccountForm;
import com.microsoft.intellij.forms.CreateVirtualNetworkForm;
import com.microsoft.intellij.ui.components.AzureWizardStep;
import com.microsoft.intellij.wizards.VMWizardModel;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import com.microsoft.tooling.msservices.model.vm.VirtualNetwork;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_VM;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.VM;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class SettingsStep extends AzureWizardStep<VMWizardModel> implements TelemetryProperties {
    private static final String CREATE_NEW = "<< Create new >>";
    private final String NONE = "(None)";

    private final Node parent;
    private Project project;
    private VMWizardModel model;
    private JPanel rootPanel;
    private JList createVmStepsList;
    private JEditorPane imageDescriptionTextPane;
    private JComboBox storageComboBox;
    private JComboBox availabilityComboBox;
    private JComboBox networkComboBox;
    private JComboBox subnetComboBox;
    private JRadioButton createNewRadioButton;
    private JRadioButton useExistingRadioButton;
    private JTextField resourceGrpField;
    private JComboBox<String> resourceGrpCombo;
    private JComboBox pipCombo;
    private JComboBox nsgCombo;

    private List<Network> virtualNetworks;

    private Map<String, StorageAccount> storageAccounts;
    private List<PublicIPAddress> publicIpAddresses;
    private List<NetworkSecurityGroup> networkSecurityGroups;
    private List<AvailabilitySet> availabilitySets;

    private Azure azure;

    public SettingsStep(final VMWizardModel model, Project project, Node parent) {
        super("Settings", null, null);

        this.parent = parent;
        this.project = project;
        this.model = model;

        model.configStepList(createVmStepsList, 3);

        final ButtonGroup resourceGroup = new ButtonGroup();
        resourceGroup.add(createNewRadioButton);
        resourceGroup.add(useExistingRadioButton);
        final ItemListener updateListener = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final boolean isNewGroup = createNewRadioButton.isSelected();
                resourceGrpField.setEnabled(isNewGroup);
                resourceGrpCombo.setEnabled(!isNewGroup);
            }
        };
        createNewRadioButton.addItemListener(updateListener);
        useExistingRadioButton.addItemListener(updateListener);

        storageComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof StorageAccount) {
                    StorageAccount sa = (StorageAccount) o;
                    setText(String.format("%s (%s)", sa.name(), sa.resourceGroupName()));
                }
            }
        });

        storageComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                validateFinish();
            }
        });

        networkComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof Network) {
                    setText(String.format("%s (%s)", ((Network) o).name(), ((Network) o).resourceGroupName()));
                }
            }
        });

        subnetComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                model.setSubnet((String) subnetComboBox.getSelectedItem());
                validateFinish();
            }
        });

        pipCombo.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof PublicIPAddress) {
                    PublicIPAddress pip = (PublicIPAddress) o;
                    setText(String.format("%s (%s)", pip.name(), pip.resourceGroupName()));
                }
            }
        });
        nsgCombo.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof NetworkSecurityGroup) {
                    setText(((NetworkSecurityGroup) o).name());
                }
            }
        });
        availabilityComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof AvailabilitySet) {
                    setText(((AvailabilitySet) o).name());
                }
            }
        });
    }

    private void fillResourceGroups() {
        // Resource groups already initialized in cache when loading locations on SelectImageStep
        List<ResourceGroup> groups = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(model.getSubscription());
        List<String> sortedGroups = groups.stream().map(ResourceGroup::name).sorted().collect(Collectors.toList());
        resourceGrpCombo.setModel(new DefaultComboBoxModel<>(sortedGroups.toArray(new String[sortedGroups.size()])));
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();

        model.getCurrentNavigationState().NEXT.setEnabled(false);
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            azure = azureManager.getAzure(model.getSubscription().getSubscriptionId());
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError("An error occurred when trying to authenticate\n\n" + ex.getMessage(), ex);
        }
        fillResourceGroups();
        retrieveStorageAccounts();
        retrieveVirtualNetworks();
        retrievePublicIpAddresses();
        retrieveNetworkSecurityGroups();
        retrieveAvailabilitySets();

        return rootPanel;
    }

    private void retrieveVirtualNetworks() {
        AzureTaskRunner.getInstance().runInBackground(new AzureTask(project, "Loading virtual networks...", false, () -> {
            final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            progressIndicator.setIndeterminate(true);
            if (virtualNetworks == null) {
                virtualNetworks = azure.networks().list();
            }
            final Runnable task = () -> networkComboBox.setModel(getVirtualNetworkModel(model.getVirtualNetwork(), model.getSubnet()));
            AzureTaskRunner.getInstance().runLater(task);
        }));

        if (virtualNetworks == null) {
            AzureTaskRunner.getInstance().runAndWait(() -> {
                final DefaultComboBoxModel loadingVNModel = new DefaultComboBoxModel(new String[]{CREATE_NEW, "<Loading...>"}) {
                    @Override
                    public void setSelectedItem(Object o) {
                        super.setSelectedItem(o);
                        if (CREATE_NEW.equals(o)) {
                            showNewVirtualNetworkForm();
                        } else {
                            model.setVirtualNetwork((Network) o);
                        }
                    }
                };
                loadingVNModel.setSelectedItem(null);
                networkComboBox.setModel(loadingVNModel);
                subnetComboBox.removeAllItems();
                subnetComboBox.setEnabled(false);
            });
        }
    }

    private DefaultComboBoxModel getVirtualNetworkModel(Network selectedVN, final String selectedSN) {
        DefaultComboBoxModel refreshedVNModel = new DefaultComboBoxModel(filterVN().toArray()) {
            @Override
            public void setSelectedItem(final Object o) {
                super.setSelectedItem(o);
                if (CREATE_NEW.equals(o)) {
                    showNewVirtualNetworkForm();
//                    model.setWithNewNetwork(true);
//                    model.setVirtualNetwork(null);
//                    model.setSubnet(null);
                } else {
//                    super.setSelectedItem(o);
                    if (o instanceof Network) {
                        if (((DefaultComboBoxModel) networkComboBox.getModel()).getIndexOf(CREATE_NEW) > 0) {
                            // new virtual network name at 0 position
                            ((DefaultComboBoxModel) networkComboBox.getModel()).removeElementAt(0);
                        }
                        model.setWithNewNetwork(false);
                        model.setVirtualNetwork((Network) o);
                        model.setNewNetwork(null);
                        AzureTaskRunner.getInstance().runAndWait(() -> {
                            subnetComboBox.setEnabled(false);
                            boolean validSubnet = false;
                            subnetComboBox.removeAllItems();
                            for (String subnet : ((Network) o).subnets().keySet()) {
                                subnetComboBox.addItem(subnet);
                                if (subnet.equals(selectedSN)) {
                                    validSubnet = true;
                                }
                            }
                            if (validSubnet) {
                                subnetComboBox.setSelectedItem(selectedSN);
                            } else {
                                model.setSubnet(null);
                                subnetComboBox.setSelectedItem(null);
                            }
                            subnetComboBox.setEnabled(true);
                        });
                    } else if (o instanceof String) {
                        // new virtual network
                        if (model.getNewNetwork() != null) {
                            subnetComboBox.setEnabled(false);
                            subnetComboBox.removeAllItems();
                            subnetComboBox.addItem(model.getNewNetwork().subnet.name);
                            subnetComboBox.setSelectedIndex(0);
                            model.setSubnet(model.getNewNetwork().subnet.name);
                        }

                    } else {
                        model.setVirtualNetwork(null);

                        AzureTaskRunner.getInstance().runAndWait(() -> {
                            subnetComboBox.removeAllItems();
                            subnetComboBox.setEnabled(false);
                        });
                    }
                }
            }
        };

        refreshedVNModel.insertElementAt(CREATE_NEW, 0);

        if (selectedVN != null && virtualNetworks.contains(selectedVN)) {
            refreshedVNModel.setSelectedItem(selectedVN);
        } else {
            model.setVirtualNetwork(null);
            refreshedVNModel.setSelectedItem(null);
        }

        return refreshedVNModel;
    }

    private List<Network> filterVN() {
        List<Network> filteredNetworks = new ArrayList<>();

        for (Network network : virtualNetworks) {
            if (network.regionName() != null && network.regionName().equals(model.getRegion().name())) {
                filteredNetworks.add(network);
            }
        }
        return filteredNetworks;
    }

    private void retrieveStorageAccounts() {
        AzureTaskRunner.getInstance().runInBackground(new AzureTask(project, "Loading storage accounts...", false, () -> {
            final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            progressIndicator.setIndeterminate(true);
            if (storageAccounts == null) {
                List<StorageAccount> accounts = azure.storageAccounts().list();
                storageAccounts = new TreeMap<>();
                for (StorageAccount storageAccount : accounts) {
                    storageAccounts.put(storageAccount.name(), storageAccount);
                }
            }
            refreshStorageAccounts(null);
        }));

        if (storageAccounts == null) {
            final DefaultComboBoxModel loadingSAModel = new DefaultComboBoxModel(new String[]{CREATE_NEW, "<Loading...>"}) {
                @Override
                public void setSelectedItem(Object o) {
                    if (CREATE_NEW.equals(o)) {
                        showNewStorageForm();
                    } else {
                        super.setSelectedItem(o);
                    }
                }
            };

            loadingSAModel.setSelectedItem(null);

            AzureTaskRunner.getInstance().runAndWait(() -> storageComboBox.setModel(loadingSAModel));
        }
    }

    private void fillStorage() {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                StorageAccount selectedSA = model.getStorageAccount();
                if (selectedSA != null && !storageAccounts.containsKey(selectedSA.name())) {
                    storageAccounts.put(selectedSA.name(), selectedSA);
                }
                refreshStorageAccounts(selectedSA);
            }
        });
    }

    private void refreshStorageAccounts(final StorageAccount selectedSA) {
        final DefaultComboBoxModel refreshedSAModel = getStorageAccountModel(selectedSA);

        AzureTaskRunner.getInstance().runAndWait(() -> storageComboBox.setModel(refreshedSAModel));
    }

    private DefaultComboBoxModel getStorageAccountModel(StorageAccount selectedSA) {
        Vector<StorageAccount> accounts = filterSA();

        final DefaultComboBoxModel refreshedSAModel = new DefaultComboBoxModel(accounts) {
            @Override
            public void setSelectedItem(Object o) {
                super.setSelectedItem(o);
                if (CREATE_NEW.equals(o)) {
                    showNewStorageForm();
                } else {
                    if (o instanceof StorageAccount) {
                        model.setStorageAccount((StorageAccount) o);
                    } else {
                        model.setStorageAccount(null); // creating new storage account
                    }
                }
            }
        };

        refreshedSAModel.insertElementAt(CREATE_NEW, 0);

        if (accounts.contains(selectedSA)) {
            refreshedSAModel.setSelectedItem(selectedSA);
        } else {
            refreshedSAModel.setSelectedItem(null);
            model.setStorageAccount(null);
        }

        return refreshedSAModel;
    }

    private Vector<StorageAccount> filterSA() {
        Vector<StorageAccount> filteredStorageAccounts = new Vector<>();

        for (StorageAccount storageAccount : storageAccounts.values()) {
            // only general purpose accounts support page blobs, so only they can be used to create vm;
            // zone-redundant acounts not supported for vm
            if (storageAccount.kind() == Kind.STORAGE
                    && storageAccount.sku().name() != SkuName.STANDARD_ZRS) {
                filteredStorageAccounts.add(storageAccount);
            }
        }
        return filteredStorageAccounts;
    }

    private void retrievePublicIpAddresses() {
        AzureTaskRunner.getInstance().runInBackground(new AzureTask(project, "Loading public ip addresses...", false, () -> {
            final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            progressIndicator.setIndeterminate(true);
            if (publicIpAddresses == null) {
                publicIpAddresses = azure.publicIPAddresses().list();
            }
            AzureTaskRunner.getInstance().runLater(() -> pipCombo.setModel(getPipAddressModel(model.getPublicIpAddress())));
        }));

        if (publicIpAddresses == null) {
            AzureTaskRunner.getInstance().runAndWait(new Runnable() {
                @Override
                public void run() {
                    final DefaultComboBoxModel loadingPipModel = new DefaultComboBoxModel(new String[]{NONE, CREATE_NEW, "<Loading...>"}) {
                        @Override
                        public void setSelectedItem(Object o) {
                            super.setSelectedItem(o);
                            if (CREATE_NEW.equals(o)) {
                                model.setWithNewPip(true);
//                    showNewPipForm();
                            } else if (NONE.equals(o)) {
                                model.setPublicIpAddress(null);
                                model.setWithNewPip(false);
                            } else {
//                                model.setVirtualNetwork((Network) o);
                            }
                        }
                    };
                    loadingPipModel.setSelectedItem(null);
                    pipCombo.setModel(loadingPipModel);
                }
            });
        }
    }

    private DefaultComboBoxModel getPipAddressModel(PublicIPAddress selectedPip) {
        DefaultComboBoxModel refreshedPipModel = new DefaultComboBoxModel(filterPip().toArray()) {
            @Override
            public void setSelectedItem(final Object o) {
                super.setSelectedItem(o);
                if (NONE.equals(o)) {
                    model.setPublicIpAddress(null);
                    model.setWithNewPip(false);
                } else if (CREATE_NEW.equals(o)) {
                    model.setWithNewPip(true);
                    model.setPublicIpAddress(null);
//                    showNewPipForm();
                } else if (o instanceof PublicIPAddress) {
                    model.setPublicIpAddress((PublicIPAddress) o);
                    model.setWithNewPip(false);
                }
            }
        };
        refreshedPipModel.insertElementAt(NONE, 0);
        refreshedPipModel.insertElementAt(CREATE_NEW, 1);

        if (selectedPip != null && publicIpAddresses.contains(selectedPip)) {
            refreshedPipModel.setSelectedItem(selectedPip);
        } else {
            model.setPublicIpAddress(null);
            refreshedPipModel.setSelectedItem(NONE);
        }

        return refreshedPipModel;
    }

    private Vector<PublicIPAddress> filterPip() {
        Vector<PublicIPAddress> filteredPips = new Vector<>();

        for (PublicIPAddress publicIpAddress : publicIpAddresses) {

            // VM and public ip address need to be in the same region
            if (publicIpAddress.regionName() != null && publicIpAddress.regionName().equals(model.getRegion().name())) {
                filteredPips.add(publicIpAddress);
            }
        }
        return filteredPips;
    }

    private void retrieveNetworkSecurityGroups() {
        AzureTaskRunner.getInstance().runInBackground(new AzureTask(project, "Loading network security groups...", false, () -> {
            final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            progressIndicator.setIndeterminate(true);
            if (networkSecurityGroups == null) {
                networkSecurityGroups = azure.networkSecurityGroups().list();
            }
            AzureTaskRunner.getInstance().runLater(() -> nsgCombo.setModel(getNsgModel(model.getNetworkSecurityGroup())));
        }));

        if (networkSecurityGroups == null) {
            AzureTaskRunner.getInstance().runAndWait(new Runnable() {
                @Override
                public void run() {
                    final DefaultComboBoxModel loadingNsgModel = new DefaultComboBoxModel(new String[]{NONE, "<Loading...>"}) {
                        @Override
                        public void setSelectedItem(Object o) {
                            super.setSelectedItem(o);
                            if (NONE.equals(o)) {
                                model.setNetworkSecurityGroup(null);
                            } else {
//                                model.setVirtualNetwork((Network) o);
                            }
                        }
                    };
                    loadingNsgModel.setSelectedItem(null);
                    nsgCombo.setModel(loadingNsgModel);
                }
            });
        }
    }

    private DefaultComboBoxModel getNsgModel(NetworkSecurityGroup selectedNsg) {
        DefaultComboBoxModel refreshedNsgModel = new DefaultComboBoxModel(filterNsg().toArray()) {
            @Override
            public void setSelectedItem(final Object o) {
                super.setSelectedItem(o);
                if (NONE.equals(o)) {
                    model.setNetworkSecurityGroup(null);
                } else if (o instanceof NetworkSecurityGroup) {
                    model.setNetworkSecurityGroup((NetworkSecurityGroup) o);
                } else {
                    model.setNetworkSecurityGroup(null);
                }
            }
        };
        refreshedNsgModel.insertElementAt(NONE, 0);

        if (selectedNsg != null && networkSecurityGroups.contains(selectedNsg)) {
            refreshedNsgModel.setSelectedItem(selectedNsg);
        } else {
            model.setNetworkSecurityGroup(null);
            refreshedNsgModel.setSelectedItem(NONE);
        }
        return refreshedNsgModel;
    }

    private Vector<NetworkSecurityGroup> filterNsg() {
        Vector<NetworkSecurityGroup> filteredNsgs = new Vector<>();

        for (NetworkSecurityGroup nsg : networkSecurityGroups) {
            // VM and network security group
            if (model.getRegion().name().equals(nsg.regionName())) {
                filteredNsgs.add(nsg);
            }
        }
        return filteredNsgs;
    }

    private void retrieveAvailabilitySets() {
        AzureTaskRunner.getInstance().runInBackground(new AzureTask(project, "Loading availability sets...", false, () -> {
            final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            progressIndicator.setIndeterminate(true);
            if (availabilitySets == null) {
                availabilitySets = azure.availabilitySets().list();
            }
            AzureTaskRunner.getInstance().runLater(() -> availabilityComboBox.setModel(getAvailabilitySetsModel(model.getAvailabilitySet())));
        }));

        if (availabilitySets == null) {
            AzureTaskRunner.getInstance().runAndWait(new Runnable() {
                @Override
                public void run() {
                    final DefaultComboBoxModel loadingPipModel = new DefaultComboBoxModel(new String[]{NONE, CREATE_NEW, "<Loading...>"}) {
                        @Override
                        public void setSelectedItem(Object o) {
                            super.setSelectedItem(o);
                            if (CREATE_NEW.equals(o)) {
                                model.setWithNewAvailabilitySet(true);
                                model.setAvailabilitySet(null);
                            } else if (NONE.equals(o)) {
                                model.setAvailabilitySet(null);
                                model.setWithNewAvailabilitySet(false);
                            } else {
//                                model.setVirtualNetwork((Network) o);
                            }
                        }
                    };
                    loadingPipModel.setSelectedItem(null);
                    pipCombo.setModel(loadingPipModel);
                }
            });
        }
    }

    private DefaultComboBoxModel getAvailabilitySetsModel(AvailabilitySet selectedAvailabilitySet) {
        DefaultComboBoxModel refreshedAvailabilitySetModel = new DefaultComboBoxModel(availabilitySets.toArray()) {
            @Override
            public void setSelectedItem(final Object o) {
                super.setSelectedItem(o);
                if (NONE.equals(o)) {
                    model.setAvailabilitySet(null);
                    model.setWithNewAvailabilitySet(false);
                } else if (CREATE_NEW.equals(o)) {
                    model.setWithNewAvailabilitySet(true);
                    model.setAvailabilitySet(null);
                } else if (o instanceof AvailabilitySet) {
                    model.setAvailabilitySet(((AvailabilitySet) o));
                    model.setWithNewAvailabilitySet(false);
                }
            }
        };
        refreshedAvailabilitySetModel.insertElementAt(NONE, 0);
        refreshedAvailabilitySetModel.insertElementAt(CREATE_NEW, 1);

        if (selectedAvailabilitySet != null && availabilitySets.contains(selectedAvailabilitySet)) {
            refreshedAvailabilitySetModel.setSelectedItem(selectedAvailabilitySet);
        } else {
            model.setPublicIpAddress(null);
            refreshedAvailabilitySetModel.setSelectedItem(NONE);
        }

        return refreshedAvailabilitySetModel;
    }

    private void showNewVirtualNetworkForm() {
        final String resourceGroupName = createNewRadioButton.isSelected() ? resourceGrpField.getText() : resourceGrpCombo.getSelectedItem().toString();

        final CreateVirtualNetworkForm form = new CreateVirtualNetworkForm(project, model.getSubscription().getSubscriptionId(), model.getRegion(),
                model.getName());
        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                VirtualNetwork newVirtualNetwork = form.getNetwork();

                if (newVirtualNetwork != null) {
                    model.setNewNetwork(newVirtualNetwork);
                    model.setWithNewNetwork(true);
                    ((DefaultComboBoxModel)networkComboBox.getModel()).insertElementAt(newVirtualNetwork.name + " (New)", 0);
                    networkComboBox.setSelectedIndex(0);
                } else {
                    networkComboBox.setSelectedItem(null);
                }
            }
        });
        form.show();
    }

    private void showNewStorageForm() {
        final CreateArmStorageAccountForm form = new CreateArmStorageAccountForm(project);
        form.fillFields(model.getSubscription(), model.getRegion());

        form.setOnCreate(() -> AzureTaskRunner.getInstance().runLater(() -> {
            com.microsoft.tooling.msservices.model.storage.StorageAccount newStorageAccount = form.getStorageAccount();

            if (newStorageAccount != null) {
                model.setNewStorageAccount(newStorageAccount);
                model.setWithNewStorageAccount(true);
                ((DefaultComboBoxModel)storageComboBox.getModel()).insertElementAt(newStorageAccount.getName() + " (New)", 0);
                storageComboBox.setSelectedIndex(0);
            } else {
                storageComboBox.setSelectedItem(null);
            }
        }));

        form.show();
    }

    private void validateFinish() {
        model.getCurrentNavigationState().FINISH.setEnabled(((storageComboBox.getSelectedItem() instanceof StorageAccount) || model.isWithNewStorageAccount()) &&
                (!subnetComboBox.isEnabled() || subnetComboBox.getSelectedItem() instanceof String));
    }

    @Override
    public boolean onFinish() {
        final boolean isNewResourceGroup = createNewRadioButton.isSelected();
        final String resourceGroupName = isNewResourceGroup ? resourceGrpField.getText() : resourceGrpCombo.getSelectedItem().toString();
        Operation operation = TelemetryManager.createOperation(VM, CREATE_VM);
        AzureTaskRunner.getInstance().runInBackground(new AzureTask(project, "Creating virtual machine " + model.getName() + "...", false, () -> {
            final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            progressIndicator.setIndeterminate(true);

            try {
                operation.start();
                String certificate = model.getCertificate();
                byte[] certData = new byte[0];
                if (!certificate.isEmpty()) {
                    File certFile = new File(certificate);
                    if (certFile.exists()) {
                        try (FileInputStream certStream = new FileInputStream(certFile)) {
                            certData = new byte[(int) certFile.length()];
                            if (certStream.read(certData) != certData.length) {
                                throw new Exception("Unable to process certificate: stream longer than informed size.");
                            }
                        } finally {
                        }
                    }
                }

//                    for (StorageAccount account : AzureManagerImpl.getManager(project).getStorageAccounts(
//                            model.getSubscription().getId(), true)) {
//                        if (account.getName().equals(storageAccount.getName())) {
//                            storageAccount = account;
//                            break;
//                        }
//                    }
                final com.microsoft.azure.management.compute.VirtualMachine vm = AzureSDKManager
                    .createVirtualMachine(model.getSubscription().getSubscriptionId(),
                                          model.getName(),
                                          resourceGroupName,
                                          createNewRadioButton.isSelected(),
                                          model.getSize(),
                                          model.getRegion().name(),
                                          model.getVirtualMachineImage(),
                                          model.getKnownMachineImage(),
                                          model.isKnownMachineImage(),
                                          model.getStorageAccount(),
                                          model.getNewStorageAccount(),
                                          model.isWithNewStorageAccount(),
                                          model.getVirtualNetwork(),
                                          model.getNewNetwork(),
                                          model.isWithNewNetwork(),
                                          model.getSubnet(),
                                          model.getPublicIpAddress(),
                                          model.isWithNewPip(),
                                          model.getAvailabilitySet(),
                                          model.isWithNewAvailabilitySet(),
                                          model.getUserName(),
                                          model.getPassword(),
                                          certData.length > 0 ? new String(certData) : null);
                // update resource groups cache if new resource group was created when creating vm
                ResourceGroup rg = null;
                if (createNewRadioButton.isSelected()) {
                    rg = azure.resourceGroups().getByName(resourceGroupName);
                    AzureModelController.addNewResourceGroup(model.getSubscription(), rg);
                }
                if (model.isWithNewStorageAccount() && model.getNewStorageAccount().isNewResourceGroup() &&
                    (rg == null || !rg.name().equals(model.getNewStorageAccount().getResourceGroupName()))) {
                    rg = azure.resourceGroups().getByName(model.getNewStorageAccount().getResourceGroupName());
                    AzureModelController.addNewResourceGroup(model.getSubscription(), rg);
                }

                AzureTaskRunner.getInstance().runLater(() -> {
                    try {
                        parent.addChildNode(new com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.
                            VMNode(parent, model.getSubscription().getSubscriptionId(), vm));
                    } catch (AzureCmdException e) {
                        String msg = "An error occurred while attempting to refresh the list of virtual machines.";
                        DefaultLoader.getUIHelper().showException(msg, e, "Azure Services Explorer - Error Refreshing VM List", false, true);
                        AzurePlugin.log(msg, e);
                    }
                });
            } catch (Exception e) {
                EventUtil.logError(operation, ErrorType.userError, e, null, null);
                String msg = "An error occurred while attempting to create the specified virtual machine."
                    + "<br>" + String.format(message("webappExpMsg"), e.getMessage());
                DefaultLoader.getUIHelper().showException(msg, e, message("errTtl"), false, true);
                AzurePlugin.log(msg, e);
            } finally {
                operation.complete();
            }
        }));
        return super.onFinish();
    }

    @Override
    public Map<String, String> toProperties() {
        return model.toProperties();
    }
}
