/**
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.forms;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.storage.AccessTier;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.SkuTier;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.helpers.LinkListener;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import com.microsoft.tooling.msservices.model.ReplicationTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_STORAGE_ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STORAGE;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class CreateArmStorageAccountForm extends AzureDialogWrapper {
    private JPanel contentPane;
    private JComboBox<SubscriptionDetail> subscriptionComboBox;
    private JTextField nameTextField;
    private JComboBox<Location> regionComboBox;
    private JComboBox replicationComboBox;
    private JLabel pricingLabel;
    private JLabel userInfoLabel;
    private JRadioButton createNewRadioButton;
    private JRadioButton useExistingRadioButton;
    private JTextField resourceGrpField;
    //private JComboBox resourceGrpCombo;
    private JComboBox accountKindCombo;
    private JComboBox performanceComboBox;
    private JComboBox accessTeirComboBox;
    private JLabel accessTierLabel;
    private JComboBox encriptonComboBox;
    private JComboBox resourceGrpCombo;
    private JLabel encriptonLabel;

    private Runnable onCreate;
    private SubscriptionDetail subscription;
    private com.microsoft.tooling.msservices.model.storage.StorageAccount newStorageAccount; // use this field only when creating from 'Create vm'
    private Project project;

    private static final String PRICING_LINK = "https://go.microsoft.com/fwlink/?LinkID=400838";

    public CreateArmStorageAccountForm(Project project) {
        super(project, true);

        this.project = project;

        setModal(true);
        setTitle("Create Storage Account");

        // this option is not supported by SDK yet
        encriptonComboBox.setVisible(false);
        encriptonLabel.setVisible(false);

        final ButtonGroup resourceGroup = new ButtonGroup();
        resourceGroup.add(createNewRadioButton);
        resourceGroup.add(useExistingRadioButton);
        final ItemListener updateListener = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final boolean isNewGroup = createNewRadioButton.isSelected();
                resourceGrpField.setEnabled(isNewGroup);
                resourceGrpCombo.setEnabled(!isNewGroup);
                validateEmptyFields();
            }
        };
        createNewRadioButton.addItemListener(updateListener);

        pricingLabel.addMouseListener(new LinkListener(PRICING_LINK));

        regionComboBox.setRenderer(new ListCellRendererWrapper<Object>() {

            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o != null && (o instanceof Location)) {
                    setText("  " + ((Location)o).displayName());
                }
            }
        });
        DocumentListener docListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }
        };

        nameTextField.getDocument().addDocumentListener(docListener);
        resourceGrpField.getDocument().addDocumentListener(docListener);

        ItemListener validateListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                validateEmptyFields();
            }
        };

        regionComboBox.addItemListener(validateListener);
        resourceGrpCombo.addItemListener(validateListener);
        resourceGrpCombo.setName("ResourceGroup");
        regionComboBox.addItemListener(e -> loadGroups());

        accountKindCombo.setRenderer(new ListCellRendererWrapper<Kind>() {
            @Override
            public void customize(JList jList, Kind kind, int i, boolean b, boolean b1) {
                if (kind == null) {
                    return;
                } else if (kind == Kind.STORAGE) {
                    setText("General Purpose v1");
                } else if (kind == Kind.STORAGE_V2) {
                    setText("General Purpose v2");
                } else if (kind == Kind.BLOB_STORAGE) {
                    setText("Blob Storage");
                }
            }
        });

        encriptonComboBox.setModel(new DefaultComboBoxModel(new Boolean[] {true, false}));
        encriptonComboBox.setRenderer(new ListCellRendererWrapper<Boolean>() {
            @Override
            public void customize(JList jList, Boolean aBoolean, int i, boolean b, boolean b1) {
                setText(aBoolean ? "Enabled" : "Disables");
            }
        });
        encriptonComboBox.setSelectedItem(Boolean.FALSE);

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    private void validateEmptyFields() {
        boolean allFieldsCompleted = !(
                nameTextField.getText().isEmpty() || regionComboBox.getSelectedObjects().length == 0
        || (createNewRadioButton.isSelected() && resourceGrpField.getText().trim().isEmpty())
        || (useExistingRadioButton.isSelected() && resourceGrpCombo.getSelectedObjects().length == 0));

        setOKActionEnabled(allFieldsCompleted);
    }


    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (nameTextField.getText().length() < 3
                || nameTextField.getText().length() > 24
                || !nameTextField.getText().matches("[a-z0-9]+")) {
            return new ValidationInfo("Invalid storage account name. The name should be between 3 and 24 characters long and \n" +
                    "can contain only lowercase letters and numbers.", nameTextField);
        }

        return null;
    }

    @Override
    protected void doOKAction() {


//        final String name = nameTextField.getText();
//        final String region = regionComboBox.getSelectedItem().toString();
//        final String replication = replicationComboBox.getSelectedItem().toString();
//        final boolean isNewResourceGroup = createNewRadioButton.isSelected();
//        final String resourceGroupName = isNewResourceGroup ? resourceGrpField.getText() : resourceGrpCombo.getSelectedItem().toString();
//        storageAccount = new ArmStorageAccount(name, subscription.getSubscriptionId(), null);
//        storageAccount.setType(replication);
//        storageAccount.setLocation(region);
//        storageAccount.setNewResourceGroup(isNewResourceGroup);
//        storageAccount.setResourceGroupName(resourceGroupName);
//        storageAccount.setKind((Kind) accoountKindCombo.getSelectedItem());
//        storageAccount.setAccessTier((AccessTier)accessTeirComboBox.getSelectedItem());
//        storageAccount.setEnableEncription((Boolean)encriptonComboBox.getSelectedItem());
        // creating from Azure Explorer directly
        setSubscription((SubscriptionDetail)subscriptionComboBox.getSelectedItem());
        if (subscription == null) {
            ProgressManager.getInstance().run(new Task.Backgroundable(project,
                    "Creating storage account " + nameTextField.getText() + "...", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    createStorageAccount();
                }
            });
            sendTelemetry(OK_EXIT_CODE);
            close(DialogWrapper.OK_EXIT_CODE, true);
        } else { //creating from 'create vm'
            newStorageAccount =
                    new com.microsoft.tooling.msservices.model.storage.StorageAccount(nameTextField.getText(), subscription.getSubscriptionId());
            boolean isNewResourceGroup = createNewRadioButton.isSelected();
            final String resourceGroupName = isNewResourceGroup ? resourceGrpField.getText() : resourceGrpCombo.getSelectedItem().toString();
            newStorageAccount.setResourceGroupName(resourceGroupName);
            newStorageAccount.setNewResourceGroup(isNewResourceGroup);
            newStorageAccount.setType(replicationComboBox.getSelectedItem().toString());
            newStorageAccount.setLocation(((Location) regionComboBox.getSelectedItem()).name());
            newStorageAccount.setKind((Kind) accountKindCombo.getSelectedItem());
            newStorageAccount.setAccessTier((AccessTier)accessTeirComboBox.getSelectedItem());

            if (onCreate != null) {
                onCreate.run();
            }
            sendTelemetry(OK_EXIT_CODE);
            close(DialogWrapper.OK_EXIT_CODE, true);
        }
//        ProgressManager.getInstance().run(
//            new Task.Modal(project, "Creating storage account", true) {
//                @Override
//                public void run(@NotNull ProgressIndicator indicator) {
//                    indicator.setIndeterminate(true);
//                    boolean success = createStorageAccount();
//                    if (success) {
//                        ApplicationManager.getApplication().invokeLater(new Runnable() {
//                            @Override
//                            public void run() {
//                                close(DialogWrapper.OK_EXIT_CODE, true);
//                            }
//                        }, ModalityState.any());
//
//                    }
//                }
//            }
//        );
    }

    @Override
    public void doCancelAction() {
        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            @Override
            public void run() {
                if (onCreate != null) {
                    onCreate.run();
                }
            }
        });
        super.doCancelAction();
    }

    private boolean createStorageAccount() {
        Operation operation = TelemetryManager.createOperation(STORAGE, CREATE_STORAGE_ACCOUNT);
        try {
            operation.start();
            boolean isNewResourceGroup = createNewRadioButton.isSelected();
            final String resourceGroupName = isNewResourceGroup ? resourceGrpField.getText() : resourceGrpCombo.getSelectedItem().toString();
            AzureSDKManager.createStorageAccount(((SubscriptionDetail) subscriptionComboBox.getSelectedItem()).getSubscriptionId(), nameTextField.getText(), ((Location) regionComboBox.getSelectedItem()).name(),
                    isNewResourceGroup, resourceGroupName, (Kind) accountKindCombo.getSelectedItem(), (AccessTier)accessTeirComboBox.getSelectedItem(),
                    (Boolean)encriptonComboBox.getSelectedItem(), replicationComboBox.getSelectedItem().toString());
            // update resource groups cache if new resource group was created when creating storage account
            if (createNewRadioButton.isSelected()) {
                AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                // not signed in; does not matter what we return as storage account already created
                if (azureManager == null) {
                    return true;
                }
                SubscriptionDetail subscriptionDetail = (SubscriptionDetail) subscriptionComboBox.getSelectedItem();
                ResourceGroup rg = azureManager.getAzure(subscriptionDetail.getSubscriptionId()).resourceGroups().getByName(resourceGroupName);
                AzureModelController.addNewResourceGroup(subscriptionDetail, rg);
            }
            DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (onCreate != null) {
                        onCreate.run();
                    }
                }
            });
            return true;
        } catch (Throwable e) {
            String msg = "An error occurred while attempting to create the specified storage account in subscription "
                    + ((SubscriptionDetail) subscriptionComboBox.getSelectedItem()).getSubscriptionId() + ".\n"
                    + String.format(message("webappExpMsg"), e.getMessage());
            DefaultLoader.getIdeHelper().invokeAndWait(() -> DefaultLoader.getUIHelper().showException(msg, e, message("errTtl"), false, true));
            EventUtil.logError(operation, ErrorType.userError, e, null, null);
            AzurePlugin.log(msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            operation.complete();
        }
    }

    public void fillFields(final SubscriptionDetail subscription, Location region) {
        if (subscription == null) {
//            loadRegions();
            accountKindCombo.setModel(new DefaultComboBoxModel(Kind.values().toArray()));
            accountKindCombo.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        fillPerformanceComboBox();
                        fillReplicationTypes();
                        boolean isBlobKind = e.getItem().equals(Kind.BLOB_STORAGE);
                        accessTeirComboBox.setVisible(isBlobKind);
                        accessTierLabel.setVisible(isBlobKind);
                    }
                }
            });
            accessTeirComboBox.setModel(new DefaultComboBoxModel(AccessTier.values()));

            subscriptionComboBox.setEnabled(true);

            try {
                AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                // not signed in
                if (azureManager == null) {
                    return;
                }
                SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
                List<SubscriptionDetail> subscriptionDetails = subscriptionManager.getSubscriptionDetails();
                List<SubscriptionDetail> selectedSubscriptions = subscriptionDetails.stream().filter(SubscriptionDetail::isSelected).collect(Collectors.toList());

                subscriptionComboBox.setModel(new DefaultComboBoxModel<>(selectedSubscriptions.toArray(new SubscriptionDetail[selectedSubscriptions.size()])));
                if (selectedSubscriptions.size() > 0) {
                    loadRegions();
                }
            } catch (Exception ex) {
                DefaultLoader.getUIHelper().logError("An error occurred when trying to load Subscriptions\n\n" + ex.getMessage(), ex);
            }

            subscriptionComboBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    loadRegions();
                }
            });
        } else { // if you create SA while creating VM
            this.subscription = subscription;
            subscriptionComboBox.addItem(subscription);
            accountKindCombo.addItem(Kind.STORAGE); // only General purpose accounts supported for VMs
            accountKindCombo.setEnabled(false);
            accessTeirComboBox.setVisible(false); // Access tier is not available for General purpose accounts
            accessTierLabel.setVisible(false);
            regionComboBox.addItem(region);
            regionComboBox.setEnabled(false);
            loadGroups();
        }
        //performanceComboBox.setModel(new DefaultComboBoxModel(SkuTier.values()));
        fillPerformanceComboBox();
        performanceComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    fillReplicationTypes();
                }
            }
        });

        replicationComboBox.setRenderer(new ListCellRendererWrapper<ReplicationTypes>() {
            @Override
            public void customize(JList jList, ReplicationTypes replicationTypes, int i, boolean b, boolean b1) {
                if (replicationTypes != null) {
                    setText(replicationTypes.getDescription());
                }
            }
        });
        fillReplicationTypes();
    }

    private void fillPerformanceComboBox() {
        if (accountKindCombo.getSelectedItem().equals(Kind.BLOB_STORAGE)) {
            performanceComboBox.setModel(new DefaultComboBoxModel(new SkuTier[] {SkuTier.STANDARD}));
        } else {
            performanceComboBox.setModel(new DefaultComboBoxModel(SkuTier.values()));
        }
    }

    private void fillReplicationTypes() {
        if (performanceComboBox.getSelectedItem().equals(SkuTier.STANDARD)) {
            // Create storage account from Azure Explorer
            if (regionComboBox.isEnabled()) {
                if (accountKindCombo.getSelectedItem().equals(Kind.BLOB_STORAGE)) {
                    replicationComboBox.setModel(
                            new DefaultComboBoxModel(new ReplicationTypes[] {
                                    ReplicationTypes.Standard_LRS,
                                    ReplicationTypes.Standard_GRS,
                                    ReplicationTypes.Standard_RAGRS}));

                } else {
                    replicationComboBox.setModel(
                            new DefaultComboBoxModel(new ReplicationTypes[] {
                                    ReplicationTypes.Standard_ZRS,
                                    ReplicationTypes.Standard_LRS,
                                    ReplicationTypes.Standard_GRS,
                                    ReplicationTypes.Standard_RAGRS}));
                    replicationComboBox.setSelectedItem(ReplicationTypes.Standard_RAGRS);
                }

            } else {
                // Create storage account from VM creation
                replicationComboBox.setModel(
                        new DefaultComboBoxModel(new ReplicationTypes[] {ReplicationTypes.Standard_LRS, ReplicationTypes.Standard_GRS, ReplicationTypes.Standard_RAGRS}));
            }
        } else {
            replicationComboBox.setModel(new DefaultComboBoxModel(new ReplicationTypes[] {ReplicationTypes.Premium_LRS}));
        }
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }

    public com.microsoft.tooling.msservices.model.storage.StorageAccount getStorageAccount() {
        return newStorageAccount;
    }

    public void loadRegions() {
        Map<SubscriptionDetail, List<Location>> subscription2Location = AzureModel.getInstance().getSubscriptionToLocationMap();
        if (subscription2Location == null || subscription2Location.get(subscriptionComboBox.getSelectedItem()) == null) {
            ProgressManager.getInstance().run(new Task.Modal(project,"Loading Available Locations...", false) {
                @Override
                public void run(ProgressIndicator indicator) {
                    try {
                        AzureModelController.updateSubscriptionMaps(null);
                        fillRegions();
                    } catch (Exception ex) {
                        AzurePlugin.log("Error loading locations", ex);
                    }
                }
            });
        } else {
            fillRegions();
        }
    }

    private void fillRegions() {
        List<Location> locations = AzureModel.getInstance().getSubscriptionToLocationMap().get(subscriptionComboBox.getSelectedItem())
                .stream().sorted(Comparator.comparing(Location::displayName)).collect(Collectors.toList());
        regionComboBox.setModel(new DefaultComboBoxModel(locations.toArray()));
        loadGroups();
    }

    private void loadGroups() {
        // Resource groups already initialized in cache when loading locations
        List<ResourceGroup> groups = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(subscriptionComboBox.getSelectedItem());
        List<String> sortedGroups = groups.stream().map(ResourceGroup::name).sorted().collect(Collectors.toList());
        resourceGrpCombo.setModel(new DefaultComboBoxModel<>(sortedGroups.toArray(new String[sortedGroups.size()])));
    }
}
