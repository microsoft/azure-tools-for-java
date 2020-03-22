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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.network.Network;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.model.vm.VirtualNetwork;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.SkuName;
import com.microsoft.azure.management.storage.StorageAccount;

import com.microsoft.azuretools.azureexplorer.forms.CreateArmStorageAccountForm;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.utils.AzureModel;

public class SettingsStep extends WizardPage {
    private static final String CREATE_NEW = "<< Create new >>";
    private static final String LOADING = "<Loading...>";

    private final String NONE = "(None)";

//    private final Node parent;
    private CreateVMWizard wizard;
    private Label resourceGrpLabel;
    private Button createNewRadioButton;
    private Button useExistingRadioButton;
    private Text resourceGrpField;
    private Combo resourceGrpCombo;
    // private JEditorPane imageDescriptionTextPane;
    private Label storageAccountLabel;
    private Combo storageComboBox;
    private Label networkLabel;
    private Combo networkComboBox;
    private Label subnetLabel;
    private Combo subnetComboBox;
    private Label pipLabel;
    private Combo pipCombo;
    private Label nsgLabel;
    private Combo nsgCombo;
    private Label availabilityLabel;
    private Combo availabilityCombo;

    private ComboViewer resourceGroupViewer;

    private List<Network> virtualNetworks;

    private Map<String, StorageAccount> storageAccounts;
    private List<PublicIPAddress> publicIpAddresses;
    private List<NetworkSecurityGroup> networkSecurityGroups;
    private List<AvailabilitySet> availabilitySets;

    public SettingsStep(CreateVMWizard wizard) {
        super("Create new Virtual Machine", "Associated Resources", Activator.getImageDescriptor("icons/large/Azure.png"));
        this.wizard = wizard;
    }

    @Override
    public void createControl(Composite parent) {
        GridLayout gridLayout = new GridLayout(1, false);
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        Composite container = new Composite(parent, 0);
        container.setLayout(gridLayout);
        container.setLayoutData(gridData);

        createSettingsPanel(container);
        //
        // imageDescription = wizard.createImageDescriptor(container);
        resourceGrpField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                wizard.setResourceGroupName(resourceGrpField.getText());
            }
        });
        resourceGrpCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                wizard.setResourceGroupName(resourceGrpCombo.getText());
            }
        });
        storageComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                validateNext();
            }
        });
        subnetComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                wizard.setSubnet((String) subnetComboBox.getText());
                validateNext();
            }
        });
        this.setControl(container);
    }

    private void createSettingsPanel(Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = GridData.BEGINNING;
         gridData.grabExcessHorizontalSpace = true;
        composite.setLayout(gridLayout);
        composite.setLayoutData(gridData);

        resourceGrpLabel = new Label(composite, SWT.LEFT);
        resourceGrpLabel.setText("Resource group:");

        final Composite container = new Composite(composite, SWT.NONE);
        gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = GridData.BEGINNING;
        // gridData.grabExcessHorizontalSpace = true;
        gridData.widthHint = 250;
        container.setLayout(gridLayout);
        container.setLayoutData(gridData);

        createNewRadioButton = new Button(container, SWT.RADIO);
        createNewRadioButton.setText("Create new");
        resourceGrpField = new Text(container, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        resourceGrpField.setLayoutData(gridData);

        useExistingRadioButton = new Button(container, SWT.RADIO);
        useExistingRadioButton.setText("Use existing");
        resourceGrpCombo = new Combo(container, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        resourceGrpCombo.setLayoutData(gridData);
        resourceGroupViewer = new ComboViewer(resourceGrpCombo);
        resourceGroupViewer.setContentProvider(ArrayContentProvider.getInstance());

        SelectionListener updateListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                handleResourceGroup();
            }
        };
        createNewRadioButton.addSelectionListener(updateListener);
        useExistingRadioButton.addSelectionListener(updateListener);
        createNewRadioButton.setSelection(true);

        handleResourceGroup();

        storageAccountLabel = new Label(composite, SWT.LEFT);
        storageAccountLabel.setText("Storage account:");
        storageComboBox = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        storageComboBox.setLayoutData(gridData);

        networkLabel = new Label(composite, SWT.LEFT);
        networkLabel.setText("Virtual Network:");
        networkComboBox = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        networkComboBox.setLayoutData(gridData);

        subnetLabel = new Label(composite, SWT.LEFT);
        subnetLabel.setText("Subnet:");
        subnetComboBox = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        subnetComboBox.setLayoutData(gridData);

        pipLabel = new Label(composite, SWT.LEFT);
        pipLabel.setText("Public IP address:");
        pipCombo = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        pipCombo.setLayoutData(gridData);

        nsgLabel = new Label(composite, SWT.LEFT);
        nsgLabel.setText("Network security group (firewall):");
        nsgCombo = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        nsgCombo.setLayoutData(gridData);

        availabilityLabel = new Label(composite, SWT.LEFT);
        availabilityLabel.setText("Availability set:");
        availabilityCombo = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        availabilityCombo.setLayoutData(gridData);

    }

    private void handleResourceGroup() {
        final boolean isNewGroup = createNewRadioButton.getSelection();
        wizard.setNewResourceGroup(isNewGroup);
        resourceGrpField.setEnabled(isNewGroup);
        resourceGrpCombo.setEnabled(!isNewGroup);
        wizard.setResourceGroupName(isNewGroup ? resourceGrpField.getText() : resourceGrpCombo.getText());
    }

    @Override
    public String getTitle() {
        setPageComplete(false);

        // final VirtualMachineImage virtualMachineImage =
        // wizard.getVirtualMachineImage();
        // imageDescription.setText(wizard.getHtmlFromVMImage(virtualMachineImage));
        fillResourceGroups();
        retrieveStorageAccounts();
        retrieveVirtualNetworks();
        retrievePublicIpAddresses();
        retrieveNetworkSecurityGroups();
        retrieveAvailabilitySets();

        return super.getTitle();
    }

    public void fillResourceGroups() {
        resourceGrpCombo.add("<Loading...>");

        DefaultLoader.getIdeHelper().runInBackground(null, "Loading resource groups...", false, true, "Loading resource groups...", new Runnable() {
                    @Override
                    public void run() {
                        // Resource groups already initialized in cache when loading locations on SelectImageStep
                        List<ResourceGroup> resourceGroups = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(wizard.getSubscription());
                        List<String> sortedGroups = resourceGroups.stream().map(ResourceGroup::name).sorted().collect(Collectors.toList());
                        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                final Vector<Object> vector = new Vector<Object>();
                                vector.addAll(sortedGroups);
                                resourceGroupViewer.setInput(vector);
                                if (vector.size() > 0) {
                                    resourceGrpCombo.select(0);
                                }
                            }
                        });

                    }
                });
    }

    private void retrieveVirtualNetworks() {
        DefaultLoader.getIdeHelper().runInBackground(null, "Loading virtual networks...", false, true,
                "Loading virtual networks...", new Runnable() {
                    @Override
                    public void run() {
                        if (virtualNetworks == null) {
                            virtualNetworks = wizard.getAzure().networks().list();
                        }
                        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                networkComboBox.removeAll();
                                networkComboBox.add(CREATE_NEW);
                                for (Network network : filterVN()) {
                                    networkComboBox.add(network.name());
                                    networkComboBox.setData(network.name(), network);
                                }
                            }
                        });
                    }
                });
        networkComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (CREATE_NEW.equals(networkComboBox.getText())) {
                     showNewVirtualNetworkForm();
                } else if ((Network) networkComboBox.getData(networkComboBox.getText()) != null) {
                    Network network = (Network) networkComboBox.getData(networkComboBox.getText());
                    wizard.setVirtualNetwork(network);
                    wizard.setNewNetwork(false);
                    wizard.setNewNetwork(null);
                    subnetComboBox.removeAll();

                    for (String subnet : network.subnets().keySet()) {
                        subnetComboBox.add(subnet);
                    }
                    subnetComboBox.setEnabled(true);
                    if (network.subnets().size() > 0) {
                        subnetComboBox.select(0);
                        wizard.setSubnet(subnetComboBox.getText());
                    }
                }
            }
        });
        if (virtualNetworks == null) {
            DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    networkComboBox.setItems(new String[] { CREATE_NEW, LOADING });
                    subnetComboBox.removeAll();
                    subnetComboBox.setEnabled(false);
                }
            });
        }
    }

    private List<Network> filterVN() {
        List<Network> filteredNetworks = new ArrayList<>();

        for (Network network : virtualNetworks) {
            if (network.regionName().equals(wizard.getRegion().name())) {
                filteredNetworks.add(network);
            }
        }
        return filteredNetworks;
    }

    private void retrieveStorageAccounts() {
        DefaultLoader.getIdeHelper().runInBackground(null, "Loading storage accounts...", false, true,
                "Loading storage accounts...", new Runnable() {
            @Override
            public void run() {
                if (storageAccounts == null) {
                    java.util.List<StorageAccount> accounts = wizard.getAzure().storageAccounts().list();
                    storageAccounts = new TreeMap<String, StorageAccount>();
                    for (StorageAccount storageAccount : accounts) {
                        storageAccounts.put(storageAccount.name(), storageAccount);
                    }
                }
                fillStorage(null);
            }
        });
        storageComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (CREATE_NEW.equals(storageComboBox.getText())) {
                     showNewStorageForm();
                } else if (storageComboBox.getData(storageComboBox.getText()) != null) {
                    StorageAccount storageAccount = (StorageAccount) storageComboBox.getData(storageComboBox.getText());
                    wizard.setStorageAccount(storageAccount);
                }

            }
        });
        if (storageAccounts == null) {
            DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    storageComboBox.setItems(new String[]{CREATE_NEW, LOADING});
                }
            });
        }
    }

    private void fillStorage(String selectedSA) {
        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            @Override
            public void run() {
                storageComboBox.removeAll();
                storageComboBox.add(CREATE_NEW);
                for (StorageAccount storageAccount : filterSA()) {
                    storageComboBox.add(storageAccount.name());
                    storageComboBox.setData(storageAccount.name(), storageAccount);
                }
                if (selectedSA != null) {
                    storageComboBox.setText(selectedSA);
                }
            }
        });
    }

    private Vector<StorageAccount> filterSA() {
        Vector<StorageAccount> filteredStorageAccounts = new Vector<>();

        for (StorageAccount storageAccount : storageAccounts.values()) {
            // VM and storage account need to be in the same region; only general purpose accounts support page blobs, so only they can be used to create vm
            if (storageAccount.kind() == Kind.STORAGE
                    && storageAccount.sku().name() != SkuName.STANDARD_ZRS) {
                filteredStorageAccounts.add(storageAccount);
            }
        }
        return filteredStorageAccounts;
    }

    private void retrievePublicIpAddresses() {
        DefaultLoader.getIdeHelper().runInBackground(null, "Loading public ip addresses...", false, true,
                "Loading public ip addresses...", new Runnable() {
            @Override
            public void run() {
                if (publicIpAddresses == null) {
                    publicIpAddresses = wizard.getAzure().publicIPAddresses().list();
                }
                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        pipCombo.removeAll();
                        pipCombo.add(NONE);
                        pipCombo.add(CREATE_NEW);
                        for (PublicIPAddress pip : filterPip()) {
                            pipCombo.add(pip.name());
                            pipCombo.setData(pip.name(), pip);
                        }
                        pipCombo.select(0);
                    }
                });
            }
        });
        pipCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (NONE.equals(pipCombo.getText())) {
                    wizard.setPublicIpAddress(null);
                    wizard.setWithNewPip(false);
                } else if (CREATE_NEW.equals(pipCombo.getText())) {
                    wizard.setWithNewPip(true);
                    wizard.setPublicIpAddress(null);
//                    showNewPipForm();
                } else if (pipCombo.getData(pipCombo.getText()) instanceof PublicIPAddress) {
                    wizard.setPublicIpAddress((PublicIPAddress) pipCombo.getData(pipCombo.getText()));
                    wizard.setWithNewPip(false);
                }
            }
        });
        if (publicIpAddresses == null) {
            DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    pipCombo.setItems(new String[]{NONE, CREATE_NEW, LOADING});
                    pipCombo.select(0);
                    wizard.setPublicIpAddress(null);
                    wizard.setWithNewPip(false);
                }
            });
        }
    }

    private Vector<PublicIPAddress> filterPip() {
        Vector<PublicIPAddress> filteredPips = new Vector<>();

        for (PublicIPAddress publicIpAddress : publicIpAddresses) {
            // VM and public ip address need to be in the same region
            if (publicIpAddress.regionName().equals(wizard.getRegion().name())) {
                filteredPips.add(publicIpAddress);
            }
        }
        return filteredPips;
    }

    private void retrieveNetworkSecurityGroups() {
        DefaultLoader.getIdeHelper().runInBackground(null, "Loading network security groups...", false, true,
                "Loading network security groups...", new Runnable() {
            @Override
            public void run() {
                if (networkSecurityGroups == null) {
                    networkSecurityGroups = wizard.getAzure().networkSecurityGroups().list();
                }
                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        nsgCombo.removeAll();
                        nsgCombo.add(NONE);
                        for (NetworkSecurityGroup nsg : networkSecurityGroups) {
                            nsgCombo.add(nsg.name());
                            nsgCombo.setData(nsg.name(), nsg);
                        }
                        nsgCombo.select(0);
                    }
                });
            }
        });
        nsgCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (NONE.equals(nsgCombo.getText())) {
                    wizard.setNetworkSecurityGroup(null);
                } else if (nsgCombo.getData(nsgCombo.getText()) instanceof NetworkSecurityGroup) {
                    wizard.setNetworkSecurityGroup((NetworkSecurityGroup) nsgCombo.getData(nsgCombo.getText()));
                }
            }
        });
        if (networkSecurityGroups == null) {
            DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    nsgCombo.setItems(new String[]{NONE, LOADING});
                    nsgCombo.select(0);
                }
            });
        }
    }

    private void retrieveAvailabilitySets() {
        DefaultLoader.getIdeHelper().runInBackground(null, "Loading availability sets...", false, true,
                "Loading availability sets...", new Runnable() {
            @Override
            public void run() {
                if (availabilitySets == null) {
                    availabilitySets = wizard.getAzure().availabilitySets().list();
                }
                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        availabilityCombo.removeAll();
                        availabilityCombo.add(NONE);
                        availabilityCombo.add(CREATE_NEW);
                        for (AvailabilitySet availabilitySet : availabilitySets) {
                            availabilityCombo.add(availabilitySet.name());
                            availabilityCombo.setData(availabilitySet.name(), availabilitySet);
                        }
                        availabilityCombo.select(0);
                    }
                });
            }
        });
        availabilityCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (NONE.equals(availabilityCombo.getText())) {
                    wizard.setAvailabilitySet(null);
                    wizard.setWithNewAvailabilitySet(false);
                } else if (CREATE_NEW.equals(availabilityCombo.getText())) {
                    wizard.setWithNewAvailabilitySet(true);
                    wizard.setAvailabilitySet(null);
                } else if (availabilityCombo.getData(availabilityCombo.getText()) instanceof AvailabilitySet) {
                    wizard.setAvailabilitySet((AvailabilitySet) availabilityCombo.getData(availabilityCombo.getText()));
                    wizard.setWithNewAvailabilitySet(false);
                }
            }
        });
        if (availabilitySets == null) {
            DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    availabilityCombo.setItems(new String[]{NONE, CREATE_NEW, LOADING});
                }
            });
        }
    }


    private Vector<NetworkSecurityGroup> filterNsg() {
        Vector<NetworkSecurityGroup> filteredNsgs = new Vector<>();

        for (NetworkSecurityGroup nsg : networkSecurityGroups) {
            // VM and network security group
            if (nsg.regionName().equals(wizard.getRegion().name())) {
                filteredNsgs.add(nsg);
            }
        }
        return filteredNsgs;
    }

    private void showNewStorageForm() {
        final CreateArmStorageAccountForm form = new CreateArmStorageAccountForm(PluginUtil.getParentShell(), wizard.getSubscription(), wizard.getRegion());

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                com.microsoft.tooling.msservices.model.storage.StorageAccount newStorageAccount = form.getStorageAccount();
                if (newStorageAccount != null) {
                    wizard.setNewStorageAccount(newStorageAccount);
                    wizard.setWithNewStorageAccount(true);
                    wizard.setStorageAccount(null);
                    storageComboBox.add(newStorageAccount.getName() + " (New)", 0);
                    storageComboBox.select(0);
//                    storageAccounts.put(newStorageAccount.name(), newStorageAccount);
//                    fillStorage(newStorageAccount.name());
                }
            }
        });

        form.open();
    }

    private void showNewVirtualNetworkForm() {
        final CreateVirtualNetworkForm form = new CreateVirtualNetworkForm(wizard.getName());
        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                VirtualNetwork newVirtualNetwork = form.getNetwork();

                if (newVirtualNetwork != null) {
                    wizard.setNewNetwork(newVirtualNetwork);
                    wizard.setNewNetwork(true);
                    networkComboBox.add(newVirtualNetwork.name + " (New)", 0);
                    networkComboBox.select(0);
                    subnetComboBox.setEnabled(false);
                    subnetComboBox.removeAll();
                    subnetComboBox.add(newVirtualNetwork.subnet.name);
                    subnetComboBox.select(0);
                    wizard.setSubnet(newVirtualNetwork.subnet.name);
                } else {
                    networkComboBox.setText(null);
                }
            }
        });
        form.open();
    }

    private void validateNext() {
        setPageComplete(storageComboBox.getData(storageComboBox.getText()) instanceof StorageAccount &&
                (!subnetComboBox.isEnabled() || subnetComboBox.getText() != null));
    }
}
