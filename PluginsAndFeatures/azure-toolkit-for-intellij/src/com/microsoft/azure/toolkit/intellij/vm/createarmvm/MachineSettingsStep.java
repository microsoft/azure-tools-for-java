/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.createarmvm;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.intellij.util.Consumer;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.KnownLinuxVirtualMachineImage;
import com.microsoft.azure.management.compute.OperatingSystemTypes;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.intellij.ui.components.AzureWizardStep;
import com.microsoft.azure.toolkit.intellij.vm.VMWizardModel;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jdesktop.swingx.JXHyperlink;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URI;
import java.util.Map;


public class MachineSettingsStep extends AzureWizardStep<VMWizardModel> implements TelemetryProperties {
    private JPanel rootPanel;
    private JList createVmStepsList;
    private JTextField vmNameTextField;
    private JComboBox<String> vmSizeComboBox;
    private JTextField vmUserTextField;
    private JPasswordField vmPasswordField;
    private JPasswordField confirmPasswordField;
    private JCheckBox passwordCheckBox;
    private JButton certificateButton;
    private JTextField certificateField;
    private JCheckBox certificateCheckBox;
    private JPanel certificatePanel;
    private JPanel passwordPanel;
    private JXHyperlink pricingLink;

    Project project;
    VMWizardModel model;

    private Azure azure;

    public MachineSettingsStep(VMWizardModel model, Project project) {
        super("Virtual Machine Basic Settings", null, null);

        this.project = project;
        this.model = model;

        this.model.configStepList(createVmStepsList, 2);

        DocumentListener documentListener = new DocumentListener() {
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

        vmNameTextField.getDocument().addDocumentListener(documentListener);
        vmUserTextField.getDocument().addDocumentListener(documentListener);
        certificateField.getDocument().addDocumentListener(documentListener);
        vmPasswordField.getDocument().addDocumentListener(documentListener);
        confirmPasswordField.getDocument().addDocumentListener(documentListener);

        certificateCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                for (Component component : certificatePanel.getComponents()) {
                    component.setEnabled(certificateCheckBox.isSelected());
                }

                certificatePanel.setEnabled(certificateCheckBox.isSelected());

                validateEmptyFields();
            }
        });

        passwordCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                for (Component component : passwordPanel.getComponents()) {
                    component.setEnabled(passwordCheckBox.isSelected());
                }

                passwordPanel.setEnabled(passwordCheckBox.isSelected());
                if (!passwordCheckBox.isSelected()) {
                    vmPasswordField.setText("");
                    confirmPasswordField.setText("");
                }
                validateEmptyFields();
            }
        });

        certificateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
                    @Override
                    public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                        try {
                            return file.isDirectory() || (file.getExtension() != null && file.getExtension().equalsIgnoreCase("pub"));
                        } catch (Throwable t) {
                            return super.isFileVisible(file, showHiddenFiles);
                        }
                    }

                    @Override
                    public boolean isFileSelectable(VirtualFile file) {
                        return (file.getExtension() != null && file.getExtension().equalsIgnoreCase("pub"));
                    }
                };

                fileChooserDescriptor.setTitle("Choose Certificate File");

                FileChooser.chooseFile(fileChooserDescriptor, null, null, new Consumer<VirtualFile>() {
                    @Override
                    public void consume(VirtualFile virtualFile) {
                        if (virtualFile != null) {
                            certificateField.setText(virtualFile.getPath());
                        }
                    }
                });
            }
        });
        pricingLink.setURI(URI.create("https://azure.microsoft.com/en-us/pricing/details/virtual-machines/linux/"));
        pricingLink.setText("Pricing");
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();

        boolean isLinux;

        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            azure = azureManager.getAzure(model.getSubscription().getId());
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError("An error occurred when trying to authenticate\n\n" + ex.getMessage(), ex);
        }
        if (model.isKnownMachineImage()) {
            isLinux = model.getKnownMachineImage() instanceof KnownLinuxVirtualMachineImage;
        } else {
            isLinux = model.getVirtualMachineImage().osDiskImage().operatingSystem() == OperatingSystemTypes.LINUX;
        }
        if (isLinux) {
            certificateCheckBox.setEnabled(true);
            passwordCheckBox.setEnabled(true);
            certificateCheckBox.setSelected(false);
            passwordCheckBox.setSelected(true);
        } else {
            certificateCheckBox.setSelected(false);
            passwordCheckBox.setSelected(true);
            certificateCheckBox.setEnabled(false);
            passwordCheckBox.setEnabled(false);
        }

        validateEmptyFields();

        if (model.getRegion() != null && (vmSizeComboBox.getItemCount() == 0 || vmSizeComboBox.getItemAt(0).contains("<Loading...>"))) {
            vmSizeComboBox.setModel(new DefaultComboBoxModel(new String[]{"<Loading...>"}));

            final AzureString title = AzureOperationBundle.title("vm.list_sizes.region", model.getRegion().getName());
            AzureTaskManager.getInstance().runInBackground(new AzureTask(project, title, false, () -> {
                final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
                progressIndicator.setIndeterminate(true);

                PagedList<com.microsoft.azure.management.compute.VirtualMachineSize> sizes =
                    azure.virtualMachines().sizes().listByRegion(model.getRegion().getName());
                sizes.sort((t0, t1) -> {
                    if (t0.name().contains("Basic") && t1.name().contains("Basic")) {
                        return t0.name().compareTo(t1.name());
                    } else if (t0.name().contains("Basic")) {
                        return -1;
                    } else if (t1.name().contains("Basic")) {
                        return 1;
                    }

                    int coreCompare = Integer.valueOf(t0.numberOfCores()).compareTo(t1.numberOfCores());

                    if (coreCompare == 0) {
                        return Integer.valueOf(t0.memoryInMB()).compareTo(t1.memoryInMB());
                    } else {
                        return coreCompare;
                    }
                });

                AzureTaskManager.getInstance().runAndWait(() -> {
                    vmSizeComboBox.setModel(new DefaultComboBoxModel<>(sizes.stream().map(VirtualMachineSize::name).toArray(String[]::new)));
                    selectDefaultSize();
                }, AzureTask.Modality.ANY);
            }));
        } else {
            selectDefaultSize();
        }

        return rootPanel;
    }

    @Override
    public WizardStep onNext(VMWizardModel model) {

        String name = vmNameTextField.getText();

        if (name.length() > 15 || name.length() < 3) {
            DefaultLoader.getUIHelper().showError("Invalid virtual machine name. The name must be between 3 and 15 "
                                                          + "character long.", "Error creating the virtual machine");
            return this;
        }

        if (!name.matches("^[A-Za-z][A-Za-z0-9-]+[A-Za-z0-9]$")) {
            DefaultLoader.getUIHelper().showError(
                    "Invalid virtual machine name. The name must start with a letter, \ncontain only letters, " +
                            "numbers, and hyphens, and end with a letter or number.",
                    "Error creating the virtual machine");
            return this;
        }

        String password = passwordCheckBox.isSelected() ? new String(vmPasswordField.getPassword()) : "";

        if (passwordCheckBox.isSelected()) {
            String conf = new String(confirmPasswordField.getPassword());

            if (!password.equals(conf)) {
                DefaultLoader.getUIHelper().showError("Password confirmation should match password", "Error creating "
                        + "the service");
                return this;
            }

            if (!password.matches("(?=^.{8,255}$)((?=.*\\d)(?=.*[A-Z])(?=.*[a-z])|(?=.*\\d)(?=.*[^A-Za-z0-9])"
                                          + "(?=.*[a-z])|(?=.*[^A-Za-z0-9])(?=.*[A-Z])(?=.*[a-z])|"
                                          + "(?=.*\\d)(?=.*[A-Z])(?=.*[^A-Za-z0-9]))^.*")) {
                DefaultLoader.getUIHelper().showError(
                        "The password does not conform to complexity requirements.\nIt should be at least eight "
                                + "characters long and contain a mixture of upper case, lower case, digits and "
                                + "symbols.", "Error creating the virtual machine");
                return this;
            }
        }

        String certificate = certificateCheckBox.isSelected() ? certificateField.getText() : "";

        model.setName(name);
        model.setSize((String) vmSizeComboBox.getSelectedItem());
        model.setUserName(vmUserTextField.getText());
        model.setPassword(password);
        model.setCertificate(certificate);

        WizardStep wizardStep = super.onNext(model);
        return wizardStep;
    }

    private void selectDefaultSize() {
    }

    private void validateEmptyFields() {
        boolean allFieldsCompleted = !(
                vmNameTextField.getText().isEmpty()
                        || vmUserTextField.getText().isEmpty()
                        || !(passwordCheckBox.isSelected() || certificateCheckBox.isSelected())
                        || (passwordCheckBox.isSelected() &&
                        (vmPasswordField.getPassword().length == 0
                                || confirmPasswordField.getPassword().length == 0))
                        || (certificateCheckBox.isSelected() && certificateField.getText().isEmpty()));

        model.getCurrentNavigationState().NEXT.setEnabled(allFieldsCompleted);
    }

    @Override
    public Map<String, String> toProperties() {
        return model.toProperties();
    }
}
