/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.forms;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class OpenSSLFinderForm extends AzureDialogWrapper {
    private JPanel contentPane;
    private JTextField txtFile;
    private JButton btnBrowse;

    public OpenSSLFinderForm(Project project) {
        super(project, true);
        setModal(true);


        btnBrowse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

                FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
                    @Override
                    public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                        try {
                            return file.isDirectory() || file.getNameWithoutExtension().toLowerCase().equals("openssl");
                        } catch (Throwable t) {
                            return super.isFileVisible(file, showHiddenFiles);
                        }
                    }

                    @Override
                    public boolean isFileSelectable(VirtualFile file) {
                        return file.getNameWithoutExtension().toLowerCase().equals("openssl");
                    }
                };

                fileChooserDescriptor.setTitle("Choose OpenSSL executable");

                FileChooser.chooseFile(fileChooserDescriptor, null, null, new Consumer<VirtualFile>() {
                    @Override
                    public void consume(VirtualFile virtualFile) {
                        if (virtualFile != null) {
                            txtFile.setText(virtualFile.getParent().getPath());
                        }
                    }
                });
            }
        });

        init();
    }

    @Override
    protected void doOKAction() {
        DefaultLoader.getIdeHelper().setProperty("MSOpenSSLPath", txtFile.getText());

        sendTelemetry(OK_EXIT_CODE);
        close(DialogWrapper.OK_EXIT_CODE, true);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (txtFile.getText() == null || txtFile.getText().isEmpty()) {
            return new ValidationInfo("Must select the OpenSSL executable location.");
        }

        return null;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
