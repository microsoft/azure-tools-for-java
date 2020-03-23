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

package com.microsoft.azuretools.docker.ui.wizards.createhost;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerSubscription;
import com.microsoft.azure.docker.model.AzureDockerVnet;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.KnownDockerVirtualMachineImage;
import com.microsoft.azure.docker.model.KnownDockerVirtualMachineSizes;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.docker.ops.utils.AzureDockerValidationUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class AzureNewDockerConfigPage extends WizardPage {
    private static final Logger log =  Logger.getLogger(AzureNewDockerConfigPage.class.getName());

    private Text dockerHostNameTextField;
    private ComboViewer dockerSubscriptionComboViewer;
    private Combo dockerSubscriptionCombo;
    private Text dockerSubscriptionIdTextField;
    private Combo dockerLocationComboBox;

    private TabFolder hostDetailsTabFolder;
    private TabItem vmKindTableItem;
    private Composite vmKindComposite;
    private Combo dockerHostOSTypeComboBox;
    private Combo dockerHostVMSizeComboBox;
    private Button dockerHostVMPreferredSizesCheckBox;

    private TabItem rgTableItem;
    private Button dockerHostNewRGRadioButton;
    private Text dockerHostRGTextField;
    private Button dockerHostSelectRGRadioButton;
    private Combo dockerHostSelectRGComboBox;

    private TabItem networkTableItem;
    private Button dockerHostNewVNetRadioButton;
    private Text dockerHostNewVNetNameTextField;
    private Text dockerHostNewVNetAddrSpaceTextField;
    private Button dockerHostSelectVNetRadioButton;
    private Combo dockerHostSelectVnetComboBox;
    private Combo dockerHostSelectSubnetComboBox;

    private TabItem storageTableItem;
    private Button dockerHostNewStorageRadioButton;
    private Text dockerNewStorageTextField;
    private Button dockerHostSelectStorageRadioButton;
    private Combo dockerSelectStorageComboBox;

    private String preferredLocation;
    private final String SELECT_REGION = "<select region>";

    private AzureNewDockerWizard wizard;
    private AzureDockerHostsManager dockerManager;
    private DockerHost newHost;
    private IProject project;

    private final FormToolkit formToolkit = new FormToolkit(Display.getDefault());
    private ManagedForm managedForm;
    private ScrolledForm errMsgForm;
    private IMessageManager errDispatcher;

    /**
     * Create the wizard.
     */
    public AzureNewDockerConfigPage(AzureNewDockerWizard wizard) {
        super("Create Docker Host", "Configure the new virtual machine", Activator.getImageDescriptor("icons/large/Azure.png"));
//        setTitle("Configure the new virtual machine");
//        setDescription("");

        this.wizard = wizard;
        this.dockerManager = wizard.getDockerManager();
        this.newHost = wizard.getDockerHost();
        this.project = wizard.getProject();

        preferredLocation = null;
    }

    /**
     * Create contents of the wizard.
     * @param parent
     */
    public void createControl(Composite parent) {
        Composite mainContainer = new Composite(parent, SWT.NONE);
        setControl(mainContainer);
        mainContainer.setLayout(new GridLayout(3, false));


        Label lblName = new Label(mainContainer, SWT.NONE);
        GridData gd_lblName = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblName.horizontalIndent = 5;
        lblName.setLayoutData(gd_lblName);
        lblName.setText("Name:");

        dockerHostNameTextField = new Text(mainContainer, SWT.BORDER);
        GridData gd_dockerHostNameTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_dockerHostNameTextField.horizontalIndent = 3;
        gd_dockerHostNameTextField.widthHint = 200;
        dockerHostNameTextField.setLayoutData(gd_dockerHostNameTextField);

        Label lblNewLabel = new Label(mainContainer, SWT.NONE);
        GridData gd_lblNewLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblNewLabel.horizontalIndent = 5;
        lblNewLabel.setLayoutData(gd_lblNewLabel);
        lblNewLabel.setText("Subscription:");

        dockerSubscriptionComboViewer = new ComboViewer(mainContainer, SWT.READ_ONLY);
        dockerSubscriptionCombo = dockerSubscriptionComboViewer.getCombo();
        dockerSubscriptionCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        new Label(mainContainer, SWT.NONE);

        Label lblId = new Label(mainContainer, SWT.NONE);
        GridData gd_lblId = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblId.horizontalIndent = 5;
        lblId.setLayoutData(gd_lblId);
        lblId.setText("Id:");

        dockerSubscriptionIdTextField = new Text(mainContainer, SWT.NONE);
        GridData gd_dockerSubscriptionIdTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerSubscriptionIdTextField.horizontalIndent = 3;
        gd_dockerSubscriptionIdTextField.widthHint = 300;
        dockerSubscriptionIdTextField.setLayoutData(gd_dockerSubscriptionIdTextField);
        dockerSubscriptionIdTextField.setEditable(false);
        dockerSubscriptionIdTextField.setBackground(mainContainer.getBackground());
        new Label(mainContainer, SWT.NONE);

        Label lblRegion = new Label(mainContainer, SWT.NONE);
        GridData gd_lblRegion = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblRegion.horizontalIndent = 5;
        lblRegion.setLayoutData(gd_lblRegion);
        lblRegion.setText("Region:");

        dockerLocationComboBox = new Combo(mainContainer, SWT.READ_ONLY);
        GridData gd_dockerLocationComboBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerLocationComboBox.widthHint = 200;
        dockerLocationComboBox.setLayoutData(gd_dockerLocationComboBox);
        new Label(mainContainer, SWT.NONE);

        hostDetailsTabFolder = new TabFolder(mainContainer, SWT.NONE);
        GridData gd_hostDetailsTabFolder = new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1);
        gd_hostDetailsTabFolder.heightHint = 128;
        hostDetailsTabFolder.setLayoutData(gd_hostDetailsTabFolder);

        vmKindTableItem = new TabItem(hostDetailsTabFolder, SWT.NONE);
        vmKindTableItem.setText("OS and Size");

        vmKindComposite = new Composite(hostDetailsTabFolder, SWT.NONE);
        vmKindTableItem.setControl(vmKindComposite);
        vmKindComposite.setLayout(new GridLayout(3, false));

        Label lblNewLabel_1 = new Label(vmKindComposite, SWT.NONE);
        lblNewLabel_1.setText("Host OS:");

        dockerHostOSTypeComboBox = new Combo(vmKindComposite, SWT.READ_ONLY);
        GridData gd_dockerHostOSTypeComboBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostOSTypeComboBox.widthHint = 200;
        dockerHostOSTypeComboBox.setLayoutData(gd_dockerHostOSTypeComboBox);
        new Label(vmKindComposite, SWT.NONE);

        Label lblSize = new Label(vmKindComposite, SWT.NONE);
        lblSize.setText("Size:");

        dockerHostVMSizeComboBox = new Combo(vmKindComposite, SWT.READ_ONLY);
        GridData gd_dockerHostVMSizeComboBox = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        gd_dockerHostVMSizeComboBox.widthHint = 200;
        dockerHostVMSizeComboBox.setLayoutData(gd_dockerHostVMSizeComboBox);

        Link dockerPricingHyperlink = new Link(vmKindComposite, SWT.NONE);
        dockerPricingHyperlink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL("https://azure.microsoft.com/en-us/pricing/details/virtual-machines/linux/"));
                } catch (Exception ex) {
                    DefaultLoader.getUIHelper().logError(ex.getMessage(), ex);
                }
            }
        });
        dockerPricingHyperlink.setText("<a>Pricing</a>");
        GridData gd_dockerPricingHyperlink = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 2);
        dockerPricingHyperlink.setLayoutData(gd_dockerPricingHyperlink);

        dockerHostVMPreferredSizesCheckBox = new Button(vmKindComposite, SWT.CHECK);
        dockerHostVMPreferredSizesCheckBox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        dockerHostVMPreferredSizesCheckBox.setText("Show preferred sizes only");

        rgTableItem = new TabItem(hostDetailsTabFolder, SWT.NONE);
        rgTableItem.setText("Resource Group");

        Composite rgComposite = new Composite(hostDetailsTabFolder, SWT.NONE);
        rgTableItem.setControl(rgComposite);
        rgComposite.setLayout(new GridLayout(2, false));

        dockerHostNewRGRadioButton = new Button(rgComposite, SWT.RADIO);
        dockerHostNewRGRadioButton.setText("New resource group:");

        dockerHostRGTextField = new Text(rgComposite, SWT.BORDER);
        GridData gd_dockerHostRGTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostRGTextField.horizontalIndent = 3;
        gd_dockerHostRGTextField.widthHint = 200;
        dockerHostRGTextField.setLayoutData(gd_dockerHostRGTextField);

        dockerHostSelectRGRadioButton = new Button(rgComposite, SWT.RADIO);
        dockerHostSelectRGRadioButton.setText("Existing resource group:");

        dockerHostSelectRGComboBox = new Combo(rgComposite, SWT.READ_ONLY);
        GridData gd_dockerHostSelectRGComboBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostSelectRGComboBox.widthHint = 220;
        dockerHostSelectRGComboBox.setLayoutData(gd_dockerHostSelectRGComboBox);

        networkTableItem = new TabItem(hostDetailsTabFolder, SWT.NONE);
        networkTableItem.setText("Network");

        Composite networkComposite = new Composite(hostDetailsTabFolder, SWT.NONE);
        networkTableItem.setControl(networkComposite);
        networkComposite.setLayout(new GridLayout(2, false));

        dockerHostNewVNetRadioButton = new Button(networkComposite, SWT.RADIO);
        dockerHostNewVNetRadioButton.setText("New virtual network");

        dockerHostNewVNetNameTextField = new Text(networkComposite, SWT.BORDER);
        GridData gd_dockerHostNewVNetNameTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostNewVNetNameTextField.horizontalIndent = 3;
        gd_dockerHostNewVNetNameTextField.widthHint = 200;
        dockerHostNewVNetNameTextField.setLayoutData(gd_dockerHostNewVNetNameTextField);

        Label lblAddressSpacecdir = new Label(networkComposite, SWT.NONE);
        GridData gd_lblAddressSpacecdir = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAddressSpacecdir.horizontalIndent = 18;
        lblAddressSpacecdir.setLayoutData(gd_lblAddressSpacecdir);
        lblAddressSpacecdir.setText("Address space (CDIR):");

        dockerHostNewVNetAddrSpaceTextField = new Text(networkComposite, SWT.BORDER);
        GridData gd_dockerHostNewVNetAddrSpaceTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostNewVNetAddrSpaceTextField.horizontalIndent = 3;
        gd_dockerHostNewVNetAddrSpaceTextField.widthHint = 200;
        dockerHostNewVNetAddrSpaceTextField.setLayoutData(gd_dockerHostNewVNetAddrSpaceTextField);

        dockerHostSelectVNetRadioButton = new Button(networkComposite, SWT.RADIO);
        dockerHostSelectVNetRadioButton.setText("Existing virtual network:");

        dockerHostSelectVnetComboBox = new Combo(networkComposite, SWT.READ_ONLY);
        GridData gd_dockerHostSelectVnetComboBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostSelectVnetComboBox.widthHint = 220;
        dockerHostSelectVnetComboBox.setLayoutData(gd_dockerHostSelectVnetComboBox);

        Label lblSubnet = new Label(networkComposite, SWT.NONE);
        GridData gd_lblSubnet = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblSubnet.horizontalIndent = 18;
        lblSubnet.setLayoutData(gd_lblSubnet);
        lblSubnet.setText("Subnet:");

        dockerHostSelectSubnetComboBox = new Combo(networkComposite, SWT.READ_ONLY);
        GridData gd_dockerHostSelectSubnetComboBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostSelectSubnetComboBox.widthHint = 220;
        dockerHostSelectSubnetComboBox.setLayoutData(gd_dockerHostSelectSubnetComboBox);

        storageTableItem = new TabItem(hostDetailsTabFolder, SWT.NONE);
        storageTableItem.setText("Storage");

        Composite storageComposite = new Composite(hostDetailsTabFolder, SWT.NONE);
        storageTableItem.setControl(storageComposite);
        storageComposite.setLayout(new GridLayout(2, false));

        dockerHostNewStorageRadioButton = new Button(storageComposite, SWT.RADIO);
        dockerHostNewStorageRadioButton.setText("New storage account:");

        dockerNewStorageTextField = new Text(storageComposite, SWT.BORDER);
        GridData gd_dockerNewStorageTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerNewStorageTextField.horizontalIndent = 3;
        gd_dockerNewStorageTextField.widthHint = 200;
        dockerNewStorageTextField.setLayoutData(gd_dockerNewStorageTextField);

        dockerHostSelectStorageRadioButton = new Button(storageComposite, SWT.RADIO);
        dockerHostSelectStorageRadioButton.setText("Existing storage account:");

        dockerSelectStorageComboBox = new Combo(storageComposite, SWT.READ_ONLY);
        GridData gd_dockerSelectStorageComboBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerSelectStorageComboBox.widthHint = 220;
        dockerSelectStorageComboBox.setLayoutData(gd_dockerSelectStorageComboBox);

        FormToolkit toolkit = new FormToolkit(mainContainer.getDisplay());
        toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
                HyperlinkSettings.UNDERLINE_HOVER);
        managedForm = new ManagedForm(mainContainer);
        errMsgForm = managedForm.getForm();
        errMsgForm.setVisible(false);
//        errMsgForm.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
//        errMsgForm.setBackground(mainContainer.getBackground());
        errDispatcher = managedForm.getMessageManager();

        initUIMainContainer(mainContainer);
    }

    private void initUIMainContainer(Composite mainContainer) {
        updateHostNameTextField(mainContainer);
        updateDockerSubscriptionComboBox(mainContainer);

        updateDockerHostVMSize(mainContainer);
        updateDockerLocationGroup(mainContainer);
        updateDockerHostOSTypeComboBox(mainContainer);
        updateDockerHostRGGroup(mainContainer);
        updateDockerHostVnetGroup(mainContainer);
        updateDockerHostStorageGroup(mainContainer);
    }

    private void updateHostNameTextField(Composite mainContainer) {
        dockerHostNameTextField.setText(newHost.name);
        dockerHostNameTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostNameTip());
        dockerHostNameTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (AzureDockerValidationUtils.validateDockerHostName(((Text) event.getSource()).getText())) {
                    errDispatcher.removeMessage("dockerHostNameTextField", dockerHostNameTextField);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerHostNameTextField", AzureDockerValidationUtils.getDockerHostNameTip(), null, IMessageProvider.ERROR, dockerHostNameTextField);
                    setErrorMessage("Invalid virtual machine name");
                    setPageComplete(false);
                }
            }
        });
    }

    private void updateDockerSubscriptionComboBox(Composite mainContainer) {
        dockerSubscriptionComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                if (selection.size() > 0){
                    AzureDockerSubscription currentSubscription = (AzureDockerSubscription) selection.getFirstElement();
                    dockerSubscriptionIdTextField.setText(currentSubscription != null ? currentSubscription.id : "");
                    wizard.setSubscription(currentSubscription);
                    errDispatcher.removeMessage("dockerSubscriptionCombo", dockerSubscriptionCombo);
                    updateDockerLocationComboBox(mainContainer, currentSubscription);
                    updateDockerHostSelectRGComboBox(mainContainer, currentSubscription);
                    String region = (String) dockerLocationComboBox.getText();
                    Region regionObj = Region.findByLabelOrName(region);
                    updateDockerSelectVnetComboBox( mainContainer, currentSubscription, regionObj != null ? regionObj.name() : region);
                    updateDockerSelectStorageComboBox(mainContainer, currentSubscription);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerSubscriptionCombo", "No active subscriptions found", null, IMessageProvider.ERROR, dockerSubscriptionCombo);
                    setPageComplete(false);
                }
            }
        });
        dockerSubscriptionComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        dockerSubscriptionComboViewer.setInput(dockerManager.getSubscriptionsList());

        if (dockerManager.getSubscriptionsList() != null && dockerManager.getSubscriptionsList().size() > 0) {
            dockerSubscriptionCombo.select(0);
            dockerSubscriptionIdTextField.setText(((AzureDockerSubscription) ((StructuredSelection) dockerSubscriptionComboViewer.getSelection()).getFirstElement()).id);
            wizard.setSubscription((AzureDockerSubscription) ((StructuredSelection) dockerSubscriptionComboViewer.getSelection()).getFirstElement());
        }
    }

    private void updateDockerLocationGroup(Composite mainContainer) {
        AzureDockerSubscription currentSubscription = getCurrentSubscription();
        dockerLocationComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String region = dockerLocationComboBox.getText();
                if (!region.equals(SELECT_REGION)) {
                    Region regionObj = Region.findByLabelOrName(region);
                    String selectedRegion = regionObj != null ? regionObj.name() : region;
                    if (preferredLocation == null && selectedRegion != null) {
                        // remove the SELECT_REGION entry (first entry in the
                        // list)
                        dockerLocationComboBox.remove(SELECT_REGION);
                    }
                    preferredLocation = selectedRegion;
                    updateDockerSelectVnetComboBox(mainContainer, currentSubscription, selectedRegion);
                    setPageComplete(doValidate());
                } else {
                    updateDockerSelectVnetComboBox(mainContainer, currentSubscription, null);
                    setPageComplete(false);
                }
                updateDockerHostVMSizeComboBox(mainContainer, dockerHostVMPreferredSizesCheckBox.getSelection());
            }
        });
        updateDockerLocationComboBox(mainContainer, currentSubscription);
    }

    private void updateDockerLocationComboBox(Composite mainContainer, AzureDockerSubscription currentSubscription) {
        if (currentSubscription != null && currentSubscription.locations != null) {
            dockerLocationComboBox.removeAll();
            if (currentSubscription.locations.size() > 0) {
                String previousSelection = preferredLocation;
                preferredLocation = null;
                for (String region : currentSubscription.locations) {
                    Region regionObj = Region.findByLabelOrName(region);
                    dockerLocationComboBox.add(regionObj != null ? regionObj.label() : region);
                    if ((previousSelection != null && region.equals(previousSelection))
                            || (newHost.hostVM.region != null && region.equals(newHost.hostVM.region))) {
                        preferredLocation = region;
                        dockerLocationComboBox.select(dockerLocationComboBox.getItemCount() - 1);
                    }
                }
                if (preferredLocation == null) {
                    dockerLocationComboBox.add(SELECT_REGION, 0);
                    dockerLocationComboBox.select(0);
                    setPageComplete(false);
                }
            }
            updateDockerHostVMSizeComboBox(mainContainer, dockerHostVMPreferredSizesCheckBox.getSelection());
        }
    }

    private void updateDockerHostOSTypeComboBox(Composite mainContainer) {
        int index = 0;
        for (KnownDockerVirtualMachineImage knownDockerVirtualMachineImage : KnownDockerVirtualMachineImage.values()) {
            String vmImage = knownDockerVirtualMachineImage.name();
            dockerHostOSTypeComboBox.add(vmImage);
            if (vmImage.equals(newHost.hostOSType.name()))
                dockerHostOSTypeComboBox.select(index);
            index++;
        }
    }

    private void updateDockerHostVMSize(Composite mainContainer) {
        dockerHostVMPreferredSizesCheckBox.setSelection(true);
        dockerHostVMPreferredSizesCheckBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateDockerHostVMSizeComboBox(mainContainer, dockerHostVMPreferredSizesCheckBox.getSelection());
                setPageComplete(doValidate());
            }
        });

        dockerHostVMSizeComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateDockerSelectStorageComboBox(mainContainer, getCurrentSubscription());
                setPageComplete(doValidate());
            }
        });
        updateDockerHostVMSizeComboBox(mainContainer, true);
    }

    private void updateDockerHostVMSizeComboBox(Composite mainContainer, boolean preferredSizesOnly) {
        dockerHostVMSizeComboBox.deselectAll();
        dockerHostVMSizeComboBox.removeAll();
        dockerHostVMSizeComboBox.redraw();
        if (preferredSizesOnly) {
            int index = 0;
            for (KnownDockerVirtualMachineSizes knownDockerVirtualMachineSize : KnownDockerVirtualMachineSizes.values()) {
                dockerHostVMSizeComboBox.add(knownDockerVirtualMachineSize.name());
                if (newHost.hostVM.vmSize.equals(knownDockerVirtualMachineSize.name())) {
                    dockerHostVMSizeComboBox.select(index);
                }
                index++;
            }
            if (index == 0 || !newHost.hostVM.vmSize.equals((String) dockerHostVMSizeComboBox.getText())) {
                dockerHostVMSizeComboBox.add(newHost.hostVM.vmSize, 0);
                dockerHostVMSizeComboBox.select(0);
            }
            updateDockerSelectStorageComboBox(mainContainer, getCurrentSubscription());
        } else {
            dockerHostVMSizeComboBox.add("<Loading...>", 0);
            dockerHostVMSizeComboBox.select(0);
            dockerHostVMSizeComboBox.redraw();
            Azure azureClient = getCurrentSubscription().azureClient;
            DefaultLoader.getIdeHelper().runInBackground(null, "Loading VM sizes...", false, true, "", new Runnable() {
                @Override
                public void run() {
                    PagedList<VirtualMachineSize> sizes = null;
                    try {
                        sizes = azureClient.virtualMachines().sizes().listByRegion(preferredLocation);
                        Collections.sort(sizes, new Comparator<VirtualMachineSize>() {
                            @Override
                            public int compare(VirtualMachineSize size1, VirtualMachineSize size2) {
                                if (size1.name().contains("Basic") && size2.name().contains("Basic")) {
                                    return size1.name().compareTo(size2.name());
                                } else if (size1.name().contains("Basic")) {
                                    return -1;
                                } else if (size2.name().contains("Basic")) {
                                    return 1;
                                }

                                int coreCompare = Integer.valueOf(size1.numberOfCores())
                                        .compareTo(size2.numberOfCores());

                                if (coreCompare == 0) {
                                    return Integer.valueOf(size1.memoryInMB()).compareTo(size2.memoryInMB());
                                } else {
                                    return coreCompare;
                                }
                            }
                        });
                    } catch (Exception notHandled) {
                    }
                    PagedList<VirtualMachineSize> sortedSizes = sizes;

                    DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            dockerHostVMSizeComboBox.deselectAll();
                            dockerHostVMSizeComboBox.removeAll();
                            if (sortedSizes != null) {
                                int index = 0;
                                for (VirtualMachineSize vmSize : sortedSizes) {
                                    dockerHostVMSizeComboBox.add(vmSize.name());
                                    if (vmSize.name().equals(newHost.hostVM.vmSize))
                                        dockerHostVMSizeComboBox.select(index);
                                    index++;
                                }
                            }
                            if (sortedSizes.size() != 0 && !newHost.hostVM.vmSize.equals((String) dockerHostVMSizeComboBox.getText())) {
                                dockerHostVMSizeComboBox.add(newHost.hostVM.vmSize, 0);
                                dockerHostVMSizeComboBox.select(0);
                            }
                            updateDockerSelectStorageComboBox(mainContainer, getCurrentSubscription());
                            dockerHostVMSizeComboBox.redraw();
                        }
                    });
                }
            });
        }
    }

    private void updateDockerHostRGGroup(Composite mainContainer) {
        dockerHostNewRGRadioButton.setSelection(true);
        dockerHostNewRGRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerHostRGTextField.setEnabled(true);
                dockerHostSelectRGComboBox.setEnabled(false);
                setPageComplete(doValidate());
            }
        });
        dockerHostSelectRGRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerHostRGTextField.setEnabled(false);
                dockerHostSelectRGComboBox.setEnabled(true);
                setPageComplete(doValidate());
            }
        });
        dockerHostRGTextField.setText(newHost.hostVM.resourceGroupName);
        dockerHostRGTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostResourceGroupNameTip());
        dockerHostRGTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent event) {
                if (AzureDockerValidationUtils.validateDockerHostResourceGroupName(((Text) event.getSource()).getText())) {
                    errDispatcher.removeMessage("dockerHostRGTextField", dockerHostRGTextField);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerHostRGTextField", AzureDockerValidationUtils.getDockerHostResourceGroupNameTip(), null, IMessageProvider.ERROR, dockerHostRGTextField);
                    setErrorMessage("Invalid resource group name");
                    setPageComplete(false);
                }
            }
        });

        updateDockerHostSelectRGComboBox(mainContainer, getCurrentSubscription());
    }

    private void updateDockerHostSelectRGComboBox(Composite mainContainer, AzureDockerSubscription currentSubscription) {
        dockerHostSelectRGComboBox.deselectAll();
        dockerHostSelectRGComboBox.removeAll();
        for (String rgName : dockerManager.getResourceGroups(currentSubscription)) {
            dockerHostSelectRGComboBox.add(rgName);
        }
        if (dockerHostSelectRGComboBox.getItemCount() > 0) {
            dockerHostSelectRGComboBox.select(0);
        }
        dockerHostSelectRGComboBox.redraw();
    }

    private void updateDockerHostVnetGroup(Composite mainContainer) {
        dockerHostNewVNetRadioButton.setSelection(true);
        dockerHostNewVNetRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerHostNewVNetNameTextField.setEnabled(true);
                dockerHostNewVNetAddrSpaceTextField.setEnabled(true);
                dockerHostSelectVnetComboBox.setEnabled(false);
                dockerHostSelectSubnetComboBox.setEnabled(false);
                setPageComplete(doValidate());
            }
        });
        dockerHostNewVNetNameTextField.setToolTipText(AzureDockerValidationUtils.getDockerVnetNameTip());
        dockerHostNewVNetNameTextField.setText(newHost.hostVM.vnetName);
        dockerHostNewVNetNameTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent event) {
                if (AzureDockerValidationUtils.validateDockerVnetName(((Text) event.getSource()).getText())) {
                    errDispatcher.removeMessage("dockerHostNewVNetNameTextField", dockerHostNewVNetNameTextField);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerHostNewVNetNameTextField", AzureDockerValidationUtils.getDockerVnetNameTip(), null, IMessageProvider.ERROR, dockerHostNewVNetNameTextField);
                    setErrorMessage("Invalid network name");
                    setPageComplete(false);
                }
            }
        });
        dockerHostNewVNetAddrSpaceTextField.setToolTipText(AzureDockerValidationUtils.getDockerVnetAddrspaceTip());
        dockerHostNewVNetAddrSpaceTextField.setText(newHost.hostVM.vnetAddressSpace);
        dockerHostNewVNetAddrSpaceTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent event) {
                if (AzureDockerValidationUtils.validateDockerVnetAddrSpace(((Text) event.getSource()).getText())) {
                    errDispatcher.removeMessage("dockerHostNewVNetAddrSpaceTextField", dockerHostNewVNetAddrSpaceTextField);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerHostNewVNetAddrSpaceTextField", AzureDockerValidationUtils.getDockerVnetAddrspaceTip(), null, IMessageProvider.ERROR, dockerHostNewVNetAddrSpaceTextField);
                    setErrorMessage("Invalid address space");
                    setPageComplete(false);
                }
            }
        });
        dockerHostSelectVNetRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerHostNewVNetNameTextField.setEnabled(false);
                dockerHostNewVNetAddrSpaceTextField.setEnabled(false);
                dockerHostSelectVnetComboBox.setEnabled(true);
                dockerHostSelectSubnetComboBox.setEnabled(true);
                setPageComplete(doValidate());
            }
        });
        dockerHostSelectVnetComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateDockerSelectSubnetComboBox(mainContainer, (AzureDockerVnet) dockerHostSelectVnetComboBox.getData(dockerHostSelectVnetComboBox.getText()));
                setPageComplete(doValidate());
            }
        });
        dockerHostSelectSubnetComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setPageComplete(doValidate());
            }
        });

        dockerHostSelectVnetComboBox.setEnabled(false);
        dockerHostSelectSubnetComboBox.setEnabled(false);
        String region = (String) dockerLocationComboBox.getText();
        Region regionObj = Region.findByLabelOrName(region);
        updateDockerSelectVnetComboBox( mainContainer, getCurrentSubscription(), regionObj != null ? regionObj.name() : region);
    }

    private void updateDockerSelectVnetComboBox(Composite mainContainer, AzureDockerSubscription currentSubscription, String region) {
        dockerHostSelectVnetComboBox.deselectAll();
        dockerHostSelectVnetComboBox.removeAll();
        if (currentSubscription != null && region != null) {
            List<AzureDockerVnet> dockerVnets = dockerManager.getNetworksAndSubnets(currentSubscription);
            for (AzureDockerVnet vnet : dockerVnets) {
                if (region != null && vnet.region.equals(region)) {
                    dockerHostSelectVnetComboBox.add(vnet.name);
                    dockerHostSelectVnetComboBox.setData(vnet.name, vnet);
                }
            }
        }
        dockerHostSelectVnetComboBox.redraw();
        if (dockerHostSelectVnetComboBox.getItemCount() > 0) {
            dockerHostSelectVnetComboBox.select(0);
            updateDockerSelectSubnetComboBox(mainContainer, (AzureDockerVnet) dockerHostSelectVnetComboBox.getData(dockerHostSelectVnetComboBox.getText()));
        } else {
            updateDockerSelectSubnetComboBox(mainContainer, null);
        }
    }

    private void updateDockerSelectSubnetComboBox(Composite mainContainer, AzureDockerVnet vnet) {
        dockerHostSelectSubnetComboBox.deselectAll();
        dockerHostSelectSubnetComboBox.removeAll();
        if (vnet != null) {
            for (String subnetName : vnet.subnets) {
                dockerHostSelectSubnetComboBox.add(subnetName);
            }
        }
        if (dockerHostSelectSubnetComboBox.getItemCount() > 0) {
            dockerHostSelectSubnetComboBox.select(0);
        }
        dockerHostSelectSubnetComboBox.redraw();
    }

    private void updateDockerHostStorageGroup(Composite mainContainer) {
        dockerHostNewStorageRadioButton.setSelection(true);
        dockerHostNewStorageRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerNewStorageTextField.setEnabled(true);
                dockerSelectStorageComboBox.setEnabled(false);
                setPageComplete(doValidate());
            }
        });
        dockerHostSelectStorageRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerNewStorageTextField.setEnabled(false);
                dockerSelectStorageComboBox.setEnabled(true);
                setPageComplete(doValidate());
            }
        });
        dockerNewStorageTextField.setText(newHost.hostVM.storageAccountName);
        dockerNewStorageTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostStorageNameTip());
        dockerNewStorageTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent event) {
                if (AzureDockerValidationUtils.validateDockerHostStorageName(((Text) event.getSource()).getText())) {
                    errDispatcher.removeMessage("dockerNewStorageTextField", dockerNewStorageTextField);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerNewStorageTextField", AzureDockerValidationUtils.getDockerHostStorageNameTip(), null, IMessageProvider.ERROR, dockerNewStorageTextField);
                    setErrorMessage("Invalid storage account name");
                    setPageComplete(false);
                }
            }
        });
        dockerSelectStorageComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setPageComplete(doValidate());
            }
        });
        dockerSelectStorageComboBox.setEnabled(false);

        updateDockerSelectStorageComboBox(mainContainer, getCurrentSubscription());
    }


    private void updateDockerSelectStorageComboBox(Composite mainContainer, AzureDockerSubscription currentSubscription) {
        dockerSelectStorageComboBox.deselectAll();
        dockerSelectStorageComboBox.removeAll();
        String vmImageSize = (String) dockerHostVMSizeComboBox.getText();
        if (vmImageSize != null) {
            List<String> storageAccountsList = dockerManager.getAvailableStorageAccounts(currentSubscription.id, "Standard");
            if (vmImageSize.contains("_D"))
                storageAccountsList.addAll(dockerManager.getAvailableStorageAccounts(currentSubscription.id, "Premium"));
            for (String storageAccName : storageAccountsList) {
                dockerSelectStorageComboBox.add(storageAccName);
            }
        }
        if (dockerSelectStorageComboBox.getItemCount() > 0) {
            dockerSelectStorageComboBox.select(0);
        }
        dockerSelectStorageComboBox.redraw();
    }


//    @Override
//    public IWizardPage getNextPage() {
//        return null;
////        return super.getNextPage();
//    }
//    @Override
//    public boolean isPageComplete() {
////        if(doValidate()) {
//            return super.isPageComplete();
////        } else {
////            return false;
////        }
//    }
//
//    @Override
//    public boolean canFlipToNextPage() {
//        getWizard().getContainer().updateButtons();
////        setPageComplete(false);
//        return ;
//    }
    private boolean validateDockerHostName() {
        // Docker virtual machine name
        String hostName = dockerHostNameTextField.getText();
        if (hostName == null || hostName.isEmpty() || !AzureDockerValidationUtils.validateDockerHostName(hostName)) {
            String errMsg = "Invalid virtual machine name";
            errDispatcher.addMessage("dockerHostNameTextField", AzureDockerValidationUtils.getDockerHostNameTip(), null, IMessageProvider.ERROR, dockerHostNameTextField);
            setErrorMessage(errMsg);
            return false;
        } else {
            newHost.name = hostName;
            newHost.hostVM.name = hostName;
            newHost.certVault.hostName = hostName;
            newHost.hostVM.publicIpName = hostName + "-pip";
            errDispatcher.removeMessage("dockerHostNameTextField", dockerHostNameTextField);
            setErrorMessage(null);
            return true;
        }
    }

    private boolean validateDockerSubscription() {
        // Subscription
        AzureDockerSubscription currentSubscription = getCurrentSubscription();
        if (currentSubscription == null || currentSubscription.id == null || currentSubscription.id.isEmpty()) {
            String errMsg = "Subscription not found";
            errDispatcher.addMessage("dockerSelectStorageComboBox", errMsg, null, IMessageProvider.ERROR, dockerSelectStorageComboBox);
            setErrorMessage(errMsg);
            return false;
        } else {
            newHost.sid = currentSubscription.id;
            errDispatcher.removeMessage("dockerSelectStorageComboBox", dockerSelectStorageComboBox);
            setErrorMessage(null);
            return true;
        }
    }

    private boolean validateDockerLocation() {
        // Location/region
        String region = (String) dockerLocationComboBox.getText();
        if (preferredLocation == null || region == null || region.isEmpty() || region.equals(SELECT_REGION)) {
            String errMsg = "Region not found";
            errDispatcher.addMessage("dockerLocationComboBox", errMsg, null, IMessageProvider.ERROR, dockerLocationComboBox);
            setErrorMessage(errMsg);
            return false;
        } else {
            newHost.hostVM.region = preferredLocation;
            String dnsSuffixFormat = "%s.%s.cloudapp.azure.com";
            // TODO: since this feature will be removed later, just fix it here for mooncake.
            try {
                AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                if (azureManager != null && azureManager.getEnvironment().equals(Environment.CHINA)) {
                    dnsSuffixFormat = "%s.%s.cloudapp.chinacloudapi.cn";
                }
            } catch (Exception e) {}
            newHost.hostVM.dnsName = String.format(dnsSuffixFormat, newHost.hostVM.name, newHost.hostVM.region);
            newHost.apiUrl = newHost.hostVM.dnsName;
            errDispatcher.removeMessage("dockerLocationComboBox", dockerLocationComboBox);
            setErrorMessage(null);
            return true;
        }
    }

    private boolean validateDockerOSType() {
        // OS type
        KnownDockerVirtualMachineImage osType = KnownDockerVirtualMachineImage.valueOf(dockerHostOSTypeComboBox.getText());
        if (osType == null) {
            hostDetailsTabFolder.setSelection(0);
            String errMsg = "OS type not set";
            errDispatcher.addMessage("dockerHostOSTypeComboBox", errMsg, null, IMessageProvider.ERROR, dockerHostOSTypeComboBox);
            setErrorMessage(errMsg);
            return false;
        } else {
            newHost.hostOSType = DockerHost.DockerHostOSType.valueOf(osType.toString());
            newHost.hostVM.osHost = osType.getAzureOSHost();
            errDispatcher.removeMessage("dockerHostOSTypeComboBox", dockerHostOSTypeComboBox);
            setErrorMessage(null);
            return true;
        }
    }

    private boolean validateDockerVMSize() {
        // Docker virtual machine size
        String vmSize = (String) dockerHostVMSizeComboBox.getText();
        if (vmSize == null || vmSize.isEmpty()) {
            hostDetailsTabFolder.setSelection(0);
            String errMsg = "Virtual machine size not set";
            errDispatcher.addMessage("dockerHostVMSizeComboBox", errMsg, null, IMessageProvider.ERROR, dockerHostVMSizeComboBox);
            setErrorMessage(errMsg);
            return false;
        } else {
            newHost.hostVM.vmSize = vmSize;
            errDispatcher.removeMessage("dockerHostVMSizeComboBox", dockerHostVMSizeComboBox);
            setErrorMessage(null);
            return true;
        }
    }

    private boolean validateDockerRG() {
        // Docker resource group name
        if (dockerHostNewRGRadioButton.getSelection()) {
            // New resource group
            String rgName = dockerHostRGTextField.getText();
            if (rgName == null || rgName.isEmpty() || !AzureDockerValidationUtils.validateDockerHostResourceGroupName(rgName)) {
                hostDetailsTabFolder.setSelection(1);
                errDispatcher.addMessage("dockerHostRGTextField", AzureDockerValidationUtils.getDockerHostResourceGroupNameTip(), null, IMessageProvider.ERROR, dockerHostRGTextField);
                setErrorMessage("Invalid resource group name");
                return false;
            } else {
                errDispatcher.removeMessage("dockerHostRGTextField", dockerHostRGTextField);
                setErrorMessage(null);
                newHost.hostVM.resourceGroupName = rgName;
                return true;
            }
        } else {
            // Existing resource group
            String rgName = (String) dockerHostSelectRGComboBox.getText();
            if (rgName == null || rgName.isEmpty()) {
                hostDetailsTabFolder.setSelection(1);
                String errMsg = "Resource group not set";
                errDispatcher.addMessage("dockerHostSelectRGComboBox", errMsg, null, IMessageProvider.ERROR, dockerHostSelectRGComboBox);
                setErrorMessage(errMsg);
                return false;
            } else {
                // Add "@" to mark this as an existing resource group
                newHost.hostVM.resourceGroupName = rgName + "@";
                errDispatcher.removeMessage("dockerHostSelectRGComboBox", dockerHostSelectRGComboBox);
                setErrorMessage(null);
                return true;
            }
        }
    }

    private boolean validateDockerVnet() {
        // Docker virtual network name
        if (dockerHostNewVNetRadioButton.getSelection()) {
            // New virtual network
            String vnetName = dockerHostNewVNetNameTextField.getText();
            if (vnetName == null || vnetName.isEmpty() || !AzureDockerValidationUtils.validateDockerVnetName(vnetName)) {
                hostDetailsTabFolder.setSelection(2);
                errDispatcher.addMessage("dockerHostNewVNetNameTextField", AzureDockerValidationUtils.getDockerVnetNameTip(), null, IMessageProvider.ERROR, dockerHostNewVNetNameTextField);
                setErrorMessage("Invalid network name");
                return false;
            } else {
                errDispatcher.removeMessage("dockerHostNewVNetNameTextField", dockerHostNewVNetNameTextField);
                setErrorMessage(null);

                String vnetAddrSpace = dockerHostNewVNetAddrSpaceTextField.getText();
                if (vnetAddrSpace == null || vnetAddrSpace.isEmpty() || !AzureDockerValidationUtils.validateDockerVnetAddrSpace(vnetAddrSpace)) {
                    hostDetailsTabFolder.setSelection(2);
                    errDispatcher.addMessage("dockerHostNewVNetAddrSpaceTextField", AzureDockerValidationUtils.getDockerVnetAddrspaceTip(), null, IMessageProvider.ERROR, dockerHostNewVNetAddrSpaceTextField);
                    setErrorMessage("Invalid address space");
                    return false;
                } else {
                    errDispatcher.removeMessage("dockerHostNewVNetAddrSpaceTextField", dockerHostNewVNetAddrSpaceTextField);
                    setErrorMessage(null);
                    newHost.hostVM.vnetName = vnetName;
                    newHost.hostVM.vnetAddressSpace = vnetAddrSpace;
                    newHost.hostVM.subnetName = "subnet1";
                    return true;
                }
            }
        } else {
            // Existing virtual network and subnet
            AzureDockerVnet vnet = (AzureDockerVnet) dockerHostSelectVnetComboBox.getData(dockerHostSelectVnetComboBox.getText());
            if (vnet == null || vnet.name == null || vnet.name.isEmpty()) {
                hostDetailsTabFolder.setSelection(2);
                String errMsg = "Network not set";
                errDispatcher.addMessage("dockerHostSelectVnetComboBox", errMsg, null, IMessageProvider.ERROR, dockerHostSelectVnetComboBox);
                setErrorMessage(errMsg);
                return false;
            } else {
                errDispatcher.removeMessage("dockerHostSelectVnetComboBox", dockerHostSelectVnetComboBox);
                setErrorMessage(null);
                String subnet = (String) dockerHostSelectSubnetComboBox.getText();
                if (subnet == null || subnet.isEmpty()) {
                    hostDetailsTabFolder.setSelection(2);
                    errDispatcher.addMessage("dockerHostSelectSubnetComboBox", AzureDockerValidationUtils.getDockerVnetAddrspaceTip(), null, IMessageProvider.ERROR, dockerHostSelectSubnetComboBox);
                    setErrorMessage("Subnet not set");
                    return false;
                } else {
                    errDispatcher.removeMessage("dockerHostSelectSubnetComboBox", dockerHostSelectSubnetComboBox);
                    setErrorMessage(null);
                    // Add "@resourceGroupName" to mark this as an existing virtual
                    // network
                    newHost.hostVM.vnetName = vnet.name + "@" + vnet.resourceGroup;
                    newHost.hostVM.vnetAddressSpace = vnet.addrSpace;
                    newHost.hostVM.subnetName = subnet;
                    return true;
                }
            }
        }
    }

    private boolean validateDockerStorage() {
        // Docker storage account
        String vmSize = (String) dockerHostVMSizeComboBox.getText();
        String storageName;
        if (dockerHostNewStorageRadioButton.getSelection()) {
            // New storage account
            storageName = dockerNewStorageTextField.getText();
            if (storageName == null || storageName.isEmpty() || vmSize == null || vmSize.isEmpty() || !AzureDockerValidationUtils.validateDockerHostStorageName(storageName, getCurrentSubscription())) {
                hostDetailsTabFolder.setSelection(3);
                errDispatcher.addMessage("dockerNewStorageTextField", AzureDockerValidationUtils.getDockerHostStorageNameTip(), null, IMessageProvider.ERROR, dockerNewStorageTextField);
                setErrorMessage("Invalid storage account name");
                return false;
            } else {
                errDispatcher.removeMessage("dockerNewStorageTextField", dockerNewStorageTextField);
                setErrorMessage(null);
                newHost.hostVM.storageAccountName = storageName;
                newHost.hostVM.storageAccountType = AzureDockerUtils.getStorageTypeForVMSize(vmSize);
                return true;
            }
        } else {
            // Existing resource group
            storageName = (String) dockerSelectStorageComboBox.getText();
            if (storageName == null || storageName.isEmpty() || vmSize == null || vmSize.isEmpty()) {
                hostDetailsTabFolder.setSelection(3);
                String errMsg = "Storage account not set";
                errDispatcher.addMessage("dockerSelectStorageComboBox", errMsg, null, IMessageProvider.ERROR, dockerSelectStorageComboBox);
                setErrorMessage(errMsg);
                return false;
            } else {
                errDispatcher.removeMessage("dockerSelectStorageComboBox", dockerSelectStorageComboBox);
                setErrorMessage(null);
                // Add "@" to mark this as an existing storage account
                newHost.hostVM.storageAccountName = storageName + "@";
                newHost.hostVM.storageAccountType = AzureDockerUtils.getStorageTypeForVMSize(vmSize);
                return true;
            }
        }
    }

    public boolean doValidate() {
        return
                validateDockerHostName() &&
                validateDockerSubscription() &&
                validateDockerLocation() &&
                validateDockerOSType() &&
                validateDockerVMSize() &&
                validateDockerRG() &&
                validateDockerVnet() &&
                validateDockerStorage();
    }


    private AzureDockerSubscription getCurrentSubscription() {
        return (AzureDockerSubscription) ((StructuredSelection) dockerSubscriptionComboViewer.getSelection()).getFirstElement();
    }
}
