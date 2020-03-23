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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.helpers.LinkListener;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class UploadBlobFileForm extends AzureDialogWrapper {
    private JPanel contentPane;
    private JLabel blobFolderLink;
    private JTextField nameTextField;
    private JButton browseButton;
    private JTextField folderTextField;

    private String folder;
    private File selectedFile;
    private Runnable uploadSelected;

    private static String linkBlob = "http://go.microsoft.com/fwlink/?LinkID=512749";

    public UploadBlobFileForm(Project project) {
        super(project, true);

        setModal(true);
        setTitle("Upload Blob File");
        validateForm();
        blobFolderLink.addMouseListener(new LinkListener(linkBlob));
        nameTextField.setEditable(false);

        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                FileChooserDescriptor fileDiscriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
                fileDiscriptor.setTitle("Upload blob");
                final VirtualFile file = FileChooser.chooseFile(fileDiscriptor, null, null);
                if (file != null) {
                    nameTextField.setText(file.getPath());
                    selectedFile = new File(nameTextField.getText());
                    validateForm();
                }

            }
        });

        folderTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                folder = folderTextField.getText();
                validateForm();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                folder = folderTextField.getText();
                validateForm();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                folder = folderTextField.getText();
                validateForm();
            }
        });

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    private void validateForm() {
        setOKActionEnabled(selectedFile != null);
    }

    @Override
    protected void doOKAction() {

        try {
            folder = new URI(null, null, folder, null).getPath();
        } catch (URISyntaxException ignore) {
        }
        EventUtil.executeWithLog(TelemetryConstants.STORAGE, TelemetryConstants.UPLOAD_BLOB_FILE, (operation) -> {
            uploadSelected.run();
        });
        close(DialogWrapper.OK_EXIT_CODE, true);
    }


    public String getFolder() {
        return folder;
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public void setUploadSelected(Runnable uploadSelected) {
        this.uploadSelected = uploadSelected;
    }
}
