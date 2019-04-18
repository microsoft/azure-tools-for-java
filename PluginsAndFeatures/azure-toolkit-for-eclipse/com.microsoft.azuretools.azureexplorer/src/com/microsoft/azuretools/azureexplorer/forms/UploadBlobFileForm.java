/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azuretools.azureexplorer.forms;

import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.core.components.AzureDialogWrapper;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class UploadBlobFileForm extends AzureDialogWrapper {
    private Link blobFolderLink;
    private Label nameLabel;
    private Text nameTextField;
    private Button browseButton;
    private Label folderLabel;
    private Text folderTextField;
    private Label folderNote;
    private Button buttonOK;

    private String folder;
    private File selectedFile;
    private Runnable uploadSelected;

    private static String LINK_BLOB = "<a href=\"http://go.microsoft.com/fwlink/?LinkID=512749\">What are blob folders?</a>";

    public UploadBlobFileForm(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Upload blob file");
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout(3, false);
        container.setLayout(gridLayout);
        GridData gridData = new GridData();
        gridData.widthHint = 350;
        container.setLayoutData(gridData);

        nameLabel = new Label(container, SWT.LEFT);
        nameLabel.setText("File Name:");
        nameTextField = new Text(container, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        nameTextField.setLayoutData(gridData);
        nameTextField.setEditable(false);
        browseButton = new Button(container, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                FileDialog dialog = new FileDialog(UploadBlobFileForm.this.getShell());
                dialog.setText("Upload blob");
                String file = dialog.open();
                if (file != null) {
                    selectedFile = new File(file);
                    nameTextField.setText(selectedFile.getAbsolutePath());

                    validateForm();
                }
            }
        });

        nameLabel = new Label(container, SWT.LEFT);
        nameLabel.setText("Folder (Optional)");
        folderTextField = new Text(container, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        gridData.horizontalSpan = 2;
        folderTextField.setLayoutData(gridData);
        folderTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent modifyEvent) {
                folder = folderTextField.getText();
                validateForm();
            }
        });

        blobFolderLink = new Link(container, SWT.LEFT);
        blobFolderLink.setText(LINK_BLOB);
        blobFolderLink.setLayoutData(gridData);
        blobFolderLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(event.text));
                } catch (Exception ex) {
					/*
					 * only logging the error in log file
					 * not showing anything to end user
					 */
                    Activator.getDefault().log("Error occurred while opening link in default browser.", ex);
                }
            }
        });
        return super.createContents(parent);
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control ctrl = super.createButtonBar(parent);
        buttonOK = getButton(IDialogConstants.OK_ID);
        buttonOK.setEnabled(false);
        return ctrl;
    }

    private void validateForm() {
        buttonOK.setEnabled(selectedFile.exists());
    }

    @Override
    protected void okPressed() {
        try {
            folder = new URI(null, null, folder, null).getPath();
        } catch (URISyntaxException ignore) {}
        EventUtil.executeWithLog(TelemetryConstants.STORAGE, TelemetryConstants.UPLOAD_BLOB_FILE, (operation) -> {
            uploadSelected.run();
        });
        super.okPressed();
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
