/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.forms;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.model.vm.VirtualNetwork;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class CreateVirtualNetworkForm extends AzureDialogWrapper {
    private JPanel contentPane;
    private JTextField nameField;
    private JTextField addressSpaceField;
    private JTextField subnetNameField;
    private JTextField subnetAddressRangeField;
    private JTextField regionField;

    private Runnable onCreate;
    private VirtualNetwork network;
    private String subscriptionId;
    private Project project;

    public CreateVirtualNetworkForm(Project project, String subscriptionId, Region region, String vmName) {
        super(project, true);

        this.project = project;
        this.subscriptionId = subscriptionId;
        nameField.setText(vmName + "-vnet");

        setModal(true);
        setTitle("Create Virtual Network");

        DocumentListener docListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                validateFields();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                validateFields();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                validateFields();
            }
        };

        nameField.getDocument().addDocumentListener(docListener);
        addressSpaceField.getDocument().addDocumentListener(docListener);
        subnetNameField.getDocument().addDocumentListener(docListener);
        subnetAddressRangeField.getDocument().addDocumentListener(docListener);

        regionField.setText(region.toString());

        validateFields();
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        network = new VirtualNetwork(nameField.getText().trim(), addressSpaceField.getText().trim(), subnetNameField.getText().trim(),
                subnetAddressRangeField.getText().trim());
        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            @Override
            public void run() {
                if (onCreate != null) {
                    onCreate.run();
                }
            }
        });
        sendTelemetry(OK_EXIT_CODE);
        close(DialogWrapper.OK_EXIT_CODE, true);

//        ProgressManager.getInstance().run(
//                new Task.Modal(project, "Creating virtual network", true) {
//                    @Override
//                    public void run(@NotNull ProgressIndicator indicator) {
//                        indicator.setIndeterminate(true);
//                        boolean success = createVirtualNetwork();
//                        if (success) {
//                            AzureTaskManager.getInstance().runLater(new Runnable() {
//                                @Override
//                                public void run() {
//                                    close(DialogWrapper.OK_EXIT_CODE, true);
//                                }
//                            }, ModalityState.any());
//
//                        }
//                    }
//                }
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

    private void validateFields() {
        boolean allFieldsCompleted = !(
                nameField.getText().isEmpty() || addressSpaceField.getText().isEmpty()
                        || subnetNameField.getText().isEmpty() || subnetAddressRangeField.getText().isEmpty());
        setOKActionEnabled(allFieldsCompleted);
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }

    public VirtualNetwork getNetwork() {
        return network;
    }
}
