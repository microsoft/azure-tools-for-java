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

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.compute.KnownLinuxVirtualMachineImage;
import com.microsoft.azure.management.compute.OperatingSystemTypes;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.utils.PluginUtil;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

import java.net.URL;
import java.util.*;


public class MachineSettingsStep extends WizardPage {
    private static String LOADING = "<Loading...>";
    private Label vmNameLabel;
    private Text vmNameTextField;
    private Label vmSizeLabel;
    private Combo vmSizeComboBox;
    private Link pricingLink;
    private Label vmUserLabel;
    private Text vmUserTextField;
    private Label vmPasswordLabel;
    private Text vmPasswordField;
    private Label confirmPasswordLabel;
    private Text confirmPasswordField;
    private Button passwordCheckBox;
    private Label certificateLabel;
    private Button certificateButton;
    private Text certificateField;
    private Button certificateCheckBox;
    //    private JPanel certificatePanel;
    //    private JPanel passwordPanel;
    private CreateVMWizard wizard;

    private boolean inSetPageComplete = false;

    public MachineSettingsStep(CreateVMWizard wizard) {
        super("Create new Virtual Machine", "Virtual Machine Basic Settings", Activator.getImageDescriptor("icons/large/Azure.png"));
        this.wizard = wizard;
    }

    @Override
    public void createControl(Composite parent) {
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.verticalAlignment = SWT.FILL;
        Composite container = new Composite(parent, 0);
        GridLayout gridLayout = new GridLayout(1, false);
        container.setLayout(gridLayout);
        container.setLayoutData(gridData);

        createSettings(container);

        this.setControl(container);
    }

    private void createSettings(Composite container) {
        final Composite composite = new Composite(container, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = GridData.BEGINNING;
        gridData.grabExcessHorizontalSpace = true;
        gridData.widthHint = 250;
        composite.setLayout(gridLayout);
        composite.setLayoutData(gridData);

        vmNameLabel = new Label(composite, SWT.LEFT);
        vmNameLabel.setText("Virtual Machine Name:");
        vmNameTextField = new Text(composite, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        vmNameTextField.setLayoutData(gridData);

        vmSizeLabel = new Label(composite, SWT.LEFT);
        vmSizeLabel.setText("Size:");

        Composite sizeContainer = new Composite(composite, SWT.NONE);
        gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        sizeContainer.setLayout(gridLayout);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        sizeContainer.setLayoutData(gridData);

        vmSizeComboBox = new Combo(sizeContainer, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        vmSizeComboBox.setLayoutData(gridData);

        pricingLink = new Link(sizeContainer, SWT.NONE);
        pricingLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser()
                            .openURL(new URL("https://azure.microsoft.com/en-us/pricing/details/virtual-machines/linux/"));
                } catch (Exception ex) {
                    DefaultLoader.getUIHelper().logError(ex.getMessage(), ex);
                }
            }
        });
        pricingLink.setText("<a>Pricing</a>");
        gridData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
        pricingLink.setLayoutData(gridData);

        vmUserLabel = new Label(composite, SWT.LEFT);
        vmUserLabel.setText("User name:");
        vmUserTextField = new Text(composite, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        vmUserTextField.setLayoutData(gridData);

        certificateCheckBox = new Button(composite, SWT.CHECK);
        certificateCheckBox.setText("Upload compatible SSH key");
        certificateCheckBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                certificateCheckBoxSelected(certificateCheckBox.getSelection());
            }
        });

        createCertificatePanel(composite);

        passwordCheckBox = new Button(composite, SWT.CHECK);
        passwordCheckBox.setText("Provide a password");
        passwordCheckBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                vmPasswordLabel.setEnabled(passwordCheckBox.getSelection());
                vmPasswordField.setEnabled(passwordCheckBox.getSelection());
                confirmPasswordLabel.setEnabled(passwordCheckBox.getSelection());
                confirmPasswordField.setEnabled(passwordCheckBox.getSelection());

                validateEmptyFields();
            }
        });

        vmPasswordLabel = new Label(composite, SWT.LEFT);
        vmPasswordLabel.setText("Password:");
        vmPasswordField = new Text(composite, SWT.PASSWORD | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        vmPasswordField.setLayoutData(gridData);
        confirmPasswordLabel = new Label(composite, SWT.LEFT);
        confirmPasswordLabel.setText("Confirm:");
        confirmPasswordField = new Text(composite, SWT.PASSWORD | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        confirmPasswordField.setLayoutData(gridData);

        ModifyListener modifyListener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent modifyEvent) {
                validateEmptyFields();
            }
        };
        vmNameTextField.addModifyListener(modifyListener);
        vmUserTextField.addModifyListener(modifyListener);
        certificateField.addModifyListener(modifyListener);
        vmPasswordField.addModifyListener(modifyListener);
        confirmPasswordField.addModifyListener(modifyListener);
    }

    private GridData getGridData(int columns) {
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        gridData.horizontalSpan = columns;
        return gridData;
    }

    private void certificateCheckBoxSelected(boolean selected) {
        certificateLabel.setEnabled(selected);
        certificateField.setEnabled(selected);
        certificateButton.setEnabled(selected);

        validateEmptyFields();
    }

    private void createCertificatePanel(Composite composite) {
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.widthHint = 250;
        Composite panel = new Composite(composite, SWT.NONE);
        panel.setLayout(gridLayout);
        panel.setLayoutData(gridData);

        certificateLabel = new Label(panel, SWT.LEFT);
        certificateLabel.setText("Certificate:");
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        certificateLabel.setLayoutData(gridData);

        certificateField = new Text(panel, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        certificateField.setLayoutData(gridData);
        certificateButton = new Button(panel, SWT.PUSH);
        certificateButton.setText("...");
        certificateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                FileDialog dialog = new FileDialog(PluginUtil.getParentShell());
                String [] extensions = {"*.pub", "*.PUB"};
                dialog.setFilterExtensions(extensions);
                String certPath = dialog.open();
                if (certPath != null) {
                    certificateField.setText(certPath);
                }
            }
        });
    }

    @Override
    public String getTitle() {
        boolean isLinux;
        if (wizard.isKnownMachineImage()) {
            isLinux = wizard.getKnownMachineImage() instanceof KnownLinuxVirtualMachineImage;
        } else {
            isLinux = wizard.getVirtualMachineImage().osDiskImage().operatingSystem().equals(OperatingSystemTypes.LINUX);
        }

        if (isLinux) {
            certificateCheckBox.setEnabled(true);
            passwordCheckBox.setEnabled(true);
            certificateCheckBoxSelected(false);
            passwordCheckBox.setSelection(true);
        } else {
            certificateCheckBoxSelected(false);
            passwordCheckBox.setSelection(true);
            certificateCheckBox.setEnabled(false);
            passwordCheckBox.setEnabled(false);
        }

        validateEmptyFields();

        //        imageDescription.setText(wizard.getHtmlFromVMImage(virtualMachineImage));

        if (vmSizeComboBox.getItemCount() == 0) {
            vmSizeComboBox.setItems(new String[] { LOADING });
            DefaultLoader.getIdeHelper().runInBackground(null, "Loading VM sizes...", false, true, "", new Runnable() {
                @Override
                public void run() {
                    PagedList<com.microsoft.azure.management.compute.VirtualMachineSize> sizes = wizard.getAzure()
                            .virtualMachines().sizes().listByRegion(wizard.getRegion().name());
                    Collections.sort(sizes, new Comparator<VirtualMachineSize>() {
                        @Override
                        public int compare(VirtualMachineSize t0, VirtualMachineSize t1) {
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
                        }
                    });
                    DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            vmSizeComboBox.removeAll();
                            for (VirtualMachineSize size : sizes) {
                                vmSizeComboBox.add(size.name());
                                vmSizeComboBox.setData(size.name(), size);
                            }
                            if (sizes.size() > 0) {
                                vmSizeComboBox.select(0);
                            }
                            //selectDefaultSize();
                        }
                    });
                }
            });
        } else {
            selectDefaultSize();
        }
        return super.getTitle();
    }

    @Override
    public IWizardPage getNextPage() {
        if (!inSetPageComplete) {
            String name = vmNameTextField.getText();

            if (name.length() > 15 || name.length() < 3) {
                DefaultLoader.getUIHelper().showError("Invalid virtual machine name. " +
                        "The name must be between 3 and 15 character long.", "Error creating the virtual machine");
                return null;
            }

            if (!name.matches("^[A-Za-z][A-Za-z0-9-]+[A-Za-z0-9]$")) {
                DefaultLoader.getUIHelper().showError("Invalid virtual machine name. The name must start with a letter, \n" +
                        "contain only letters, numbers, and hyphens, " +
                        "and end with a letter or number.", "Error creating the virtual machine");
                return null;
            }

            String password = passwordCheckBox.getSelection() ? vmPasswordField.getText() : "";

            if (passwordCheckBox.getSelection()) {
                String conf = confirmPasswordField.getText();

                if (!password.equals(conf)) {
                    DefaultLoader.getUIHelper().showError("Password confirmation should match password", "Error creating the service");
                    return null;
                }

                if (!password.matches("(?=^.{8,255}$)((?=.*\\d)(?=.*[A-Z])(?=.*[a-z])|(?=.*\\d)(?=.*[^A-Za-z0-9])" +
                        "(?=.*[a-z])|(?=.*[^A-Za-z0-9])(?=.*[A-Z])(?=.*[a-z])|(?=.*\\d)(?=.*[A-Z])(?=.*[^A-Za-z0-9]))^.*")) {
                    DefaultLoader.getUIHelper().showError("The password does not conform to complexity requirements.\n" +
                            "It should be at least eight characters long and contain a mixture of upper case, lower case, " +
                            "digits and symbols.", "Error creating the virtual machine");
                    return null;
                }
            }

            String certificate = certificateCheckBox.getSelection() ? certificateField.getText() : "";

            wizard.setName(name);
            wizard.setSize((VirtualMachineSize) vmSizeComboBox.getData(vmSizeComboBox.getText()));
            wizard.setUserName(vmUserTextField.getText());
            wizard.setPassword(password);
            wizard.setCertificate(certificate);
        }
        return super.getNextPage();
    }

    private void selectDefaultSize() {
    //TODO
        /*DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                String recommendedVMSize = ((CreateVMWizard) wizard).getVirtualMachineImage().getRecommendedVMSize().isEmpty()
                        ? "Small"
                        : ((CreateVMWizard) wizard).getVirtualMachineImage().recommendedVMSize();
                for (String sizeLabel : vmSizeComboBox.getItems()) {
                    VirtualMachineSize size = (VirtualMachineSize) vmSizeComboBox.getData(sizeLabel);
                    if (size != null && size.name().equals(recommendedVMSize)) {
                        vmSizeComboBox.setText(sizeLabel);
                    }
                }
            }
        });*/
    }

    private void validateEmptyFields() {

        boolean allFieldsCompleted = !(
                vmNameTextField.getText().isEmpty()
                        || vmUserTextField.getText().isEmpty()
                        || !(passwordCheckBox.getSelection() || certificateCheckBox.getSelection())
                        || (passwordCheckBox.getSelection() &&
                        (vmPasswordField.getText().length() == 0
                                || confirmPasswordField.getText().length() == 0))
                        || (vmSizeComboBox.getText() == null || vmSizeComboBox.getText().isEmpty() || LOADING.equals(vmSizeComboBox.getText()))
                        || (certificateCheckBox.getSelection() && certificateField.getText().isEmpty()));
        inSetPageComplete = true;
        setPageComplete(allFieldsCompleted);
        inSetPageComplete = false;
    }
}
