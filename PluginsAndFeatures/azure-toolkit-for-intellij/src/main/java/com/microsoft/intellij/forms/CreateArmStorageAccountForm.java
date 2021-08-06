/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.forms;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.management.storage.AccessTier;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.SkuTier;
import com.microsoft.azure.toolkit.intellij.appservice.region.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.appservice.subscription.SubscriptionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.AzureGroup;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.helpers.LinkListener;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import com.microsoft.tooling.msservices.model.ReplicationTypes;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Objects;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_STORAGE_ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STORAGE;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class CreateArmStorageAccountForm extends AzureDialogWrapper {
    private JPanel contentPane;
    private com.microsoft.azure.toolkit.intellij.appservice.subscription.SubscriptionComboBox subscriptionComboBox;
    private JTextField nameTextField;
    private com.microsoft.azure.toolkit.intellij.appservice.region.RegionComboBox regionComboBox;
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
    private Subscription subscription;
    private com.microsoft.tooling.msservices.model.storage.StorageAccount newStorageAccount; // use this field only when creating from 'Create vm'
    private Project project;

    private static final String PRICING_LINK = "http://go.microsoft.com/fwlink/?LinkID=400838";

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
        final ItemListener updateListener = e -> {
            final boolean isNewGroup = createNewRadioButton.isSelected();
            resourceGrpField.setEnabled(isNewGroup);
            resourceGrpCombo.setEnabled(!isNewGroup);
            validateEmptyFields();
        };
        createNewRadioButton.addItemListener(updateListener);

        pricingLabel.addMouseListener(new LinkListener(PRICING_LINK));

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

        ItemListener validateListener = itemEvent -> validateEmptyFields();

        regionComboBox.addItemListener(validateListener);
        resourceGrpCombo.addItemListener(validateListener);
        resourceGrpCombo.setName("ResourceGroup");

        accountKindCombo.setRenderer(new ListCellRendererWrapper<Kind>() {
            @Override
            public void customize(JList list, Kind kind, int i, boolean b, boolean b1) {
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
            public void customize(JList list, Boolean enabled, int i, boolean b, boolean b1) {
                setText(enabled ? "Enabled" : "Disables");
            }
        });
        encriptonComboBox.setSelectedItem(Boolean.FALSE);

        init();

        subscriptionComboBox.addItemListener(itemEvent -> loadRegionAndGroups());
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    private void createUIComponents() {
        this.subscriptionComboBox = new SubscriptionComboBox();
        this.regionComboBox = new RegionComboBox();
    }

    private void validateEmptyFields() {
        boolean allFieldsCompleted = !(nameTextField.getText().isEmpty() ||
            regionComboBox.getValue() == null ||
            (createNewRadioButton.isSelected() && resourceGrpField.getText().trim().isEmpty()) ||
            (useExistingRadioButton.isSelected() && resourceGrpCombo.getSelectedObjects().length == 0));

        setOKActionEnabled(allFieldsCompleted);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (nameTextField.getText().length() < 3 || nameTextField.getText().length() > 24 || !nameTextField.getText().matches("[a-z0-9]+")) {
            return new ValidationInfo("Invalid storage account name. The name should be between 3 and 24 characters long and \n" +
                    "can contain only lowercase letters and numbers.", nameTextField);
        }

        return null;
    }

    @Override
    protected void doOKAction() {
        // creating from Azure Explorer directly
        setSubscription((Subscription) subscriptionComboBox.getSelectedItem());
        if (subscription == null) {
            final AzureString title = AzureOperationBundle.title("storage.create_account", nameTextField.getText());
            AzureTaskManager.getInstance().runInBackground(new AzureTask(project, title, false, () -> {
                final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
                progressIndicator.setIndeterminate(true);
                createStorageAccount();
            }));
            sendTelemetry(OK_EXIT_CODE);
            close(DialogWrapper.OK_EXIT_CODE, true);
        } else { //creating from 'create vm'
            newStorageAccount =
                    new com.microsoft.tooling.msservices.model.storage.StorageAccount(nameTextField.getText(), subscription.getId());
            boolean isNewResourceGroup = createNewRadioButton.isSelected();
            final String resourceGroupName = isNewResourceGroup ? resourceGrpField.getText() : resourceGrpCombo.getSelectedItem().toString();
            newStorageAccount.setResourceGroupName(resourceGroupName);
            newStorageAccount.setNewResourceGroup(isNewResourceGroup);
            newStorageAccount.setType(replicationComboBox.getSelectedItem().toString());
            newStorageAccount.setLocation(((Region) regionComboBox.getSelectedItem()).getName());
            newStorageAccount.setKind((Kind) accountKindCombo.getSelectedItem());
            newStorageAccount.setAccessTier((AccessTier) accessTeirComboBox.getSelectedItem());

            if (onCreate != null) {
                onCreate.run();
            }
            sendTelemetry(OK_EXIT_CODE);
            close(DialogWrapper.OK_EXIT_CODE, true);
        }
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
            AzureSDKManager.createStorageAccount(((Subscription) subscriptionComboBox.getSelectedItem()).getId(),
                                                 nameTextField.getText(),
                                                 ((Region) regionComboBox.getSelectedItem()).getName(),
                                                 isNewResourceGroup,
                                                 resourceGroupName,
                                                 (Kind) accountKindCombo.getSelectedItem(),
                                                 (AccessTier) accessTeirComboBox.getSelectedItem(),
                                                 (Boolean) encriptonComboBox.getSelectedItem(),
                                                 replicationComboBox.getSelectedItem().toString());
            // update resource groups cache if new resource group was created when creating storage account
            if (createNewRadioButton.isSelected()) {
                AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                // not signed in; does not matter what we return as storage account already created
                if (azureManager == null) {
                    return true;
                }
            }
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (onCreate != null) {
                    onCreate.run();
                }
            });
            return true;
        } catch (Exception e) {
            String msg = "An error occurred while attempting to create the specified storage account in subscription "
                    + ((Subscription) subscriptionComboBox.getSelectedItem()).getId() + ".\n"
                    + String.format(message("webappExpMsg"), e.getMessage());
            final AzureTask.Modality modality = AzureTask.Modality.ANY;
            AzureTaskManager.getInstance().runAndWait(() -> DefaultLoader.getUIHelper().showException(msg, e, message("errTtl"), false, true), modality);
            EventUtil.logError(operation, ErrorType.userError, e, null, null);
            AzurePlugin.log(msg, e);
        } finally {
            operation.complete();
        }
        return false;
    }

    public void fillFields(final Subscription subscription, Region region) {
        if (subscription == null) {
            accountKindCombo.setModel(new DefaultComboBoxModel(Kind.values().toArray()));
            accountKindCombo.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    fillPerformanceComboBox();
                    fillReplicationTypes();
                    boolean isBlobKind = e.getItem().equals(Kind.BLOB_STORAGE);
                    accessTeirComboBox.setVisible(isBlobKind);
                    accessTierLabel.setVisible(isBlobKind);
                }
            });
            accessTeirComboBox.setModel(new DefaultComboBoxModel(AccessTier.values()));

            subscriptionComboBox.setEnabled(true);
            if (subscription != null) {
                subscriptionComboBox.setValue(subscription);
            }

            try {
                AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                // not signed in
                if (azureManager == null) {
                    return;
                }

                if (subscription != null) {
                    loadRegionAndGroups();
                }
            } catch (Exception ex) {
                DefaultLoader.getUIHelper().logError("An error occurred when trying to load Subscriptions\n\n" + ex.getMessage(), ex);
            }

        } else { // if you create SA while creating VM
            this.subscription = subscription;
            subscriptionComboBox.setValue(subscription, true);
            accountKindCombo.addItem(Kind.STORAGE); // only General purpose accounts supported for VMs
            accountKindCombo.setEnabled(false);
            accessTeirComboBox.setVisible(false); // Access tier is not available for General purpose accounts
            accessTierLabel.setVisible(false);
            regionComboBox.setEnabled(false);
            regionComboBox.setValue(region, true);
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
            public void customize(JList list, ReplicationTypes replicationTypes, int i, boolean b, boolean b1) {
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
        if (Objects.equals(performanceComboBox.getSelectedItem(), SkuTier.STANDARD)) {
            // Create storage account from Azure Explorer
            final ReplicationTypes[] types = {
                ReplicationTypes.Standard_LRS,
                ReplicationTypes.Standard_GRS,
                ReplicationTypes.Standard_RAGRS
            };
            if (regionComboBox.isEnabled()) {
                if (Objects.equals(accountKindCombo.getSelectedItem(), Kind.BLOB_STORAGE)) {
                    replicationComboBox.setModel(new DefaultComboBoxModel(types));
                } else {
                    final ReplicationTypes[] replicationTypes = {
                        ReplicationTypes.Standard_ZRS,
                        ReplicationTypes.Standard_LRS,
                        ReplicationTypes.Standard_GRS,
                        ReplicationTypes.Standard_RAGRS
                    };
                    replicationComboBox.setModel(new DefaultComboBoxModel(replicationTypes));
                    replicationComboBox.setSelectedItem(ReplicationTypes.Standard_RAGRS);
                }

            } else {
                // Create storage account from VM creation
                replicationComboBox.setModel(new DefaultComboBoxModel(types));
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

    private void loadRegionAndGroups() {
        Subscription selectedSubscription = subscriptionComboBox.getValue();
        this.regionComboBox.setSubscription(selectedSubscription);
        if (selectedSubscription == null) {
            resourceGrpCombo.removeAllItems();
        } else {
            List<ResourceGroup> groups = Azure.az(AzureGroup.class).list(selectedSubscription.getId());
            resourceGrpCombo.setModel(new DefaultComboBoxModel<>(groups.stream().map(ResourceGroup::getName).sorted().toArray(String[]::new)));
        }
    }
}
