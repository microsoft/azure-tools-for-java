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
package com.microsoft.intellij.forms;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.management.resources.Location;
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

    public CreateVirtualNetworkForm(Project project, String subscriptionId, Location region, String vmName) {
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
//                            ApplicationManager.getApplication().invokeLater(new Runnable() {
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
