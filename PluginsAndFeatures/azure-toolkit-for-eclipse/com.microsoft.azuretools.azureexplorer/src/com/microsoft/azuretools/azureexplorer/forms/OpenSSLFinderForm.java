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
package com.microsoft.azuretools.azureexplorer.forms;


import com.microsoft.azuretools.core.components.AzureDialogWrapper;
import com.microsoft.azuretools.core.ui.commoncontrols.Messages;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import java.io.File;

public class OpenSSLFinderForm extends AzureDialogWrapper {
    private Button buttonOK;
    private Button buttonCancel;
    private Label notFoundLabel;
    private Label txtFileLabel;
    private Text txtFile;
    private Button btnBrowse;

    public OpenSSLFinderForm(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Choose OpenSSL executable");
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        GridData gridData = new GridData();
        gridData.verticalAlignment = SWT.FILL;
        gridData.horizontalAlignment = SWT.FILL;
        parent.setLayoutData(gridData);
        Control ctrl = super.createButtonBar(parent);
        buttonOK = getButton(IDialogConstants.OK_ID);
        buttonOK.setEnabled(false);
        return ctrl;
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, false);
        container.setLayout(gridLayout);

        notFoundLabel = new Label(container, SWT.LEFT);
        GridData gridData = new GridData();
        gridData.verticalIndent = 10;
        gridData.horizontalSpan = 2;
        notFoundLabel.setLayoutData(gridData);
        notFoundLabel.setText("OpenSSL was not found. An OpenSSL installation is required \n to use the Azure Explorer Plugin.");

        txtFileLabel = new Label(container, SWT.LEFT);
        gridData = new GridData();
        gridData.verticalIndent = 10;
        gridData.horizontalSpan = 2;
        txtFileLabel.setLayoutData(gridData);
        txtFileLabel.setText("Please select the location of the Open SSL executable:");

        txtFile =  new Text(container, SWT.SINGLE | SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        txtFile.setLayoutData(gridData);
        txtFile.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (buttonOK != null) {
                    String path = txtFile.getText().trim();
                    if (path.isEmpty()) {
                        buttonOK.setEnabled(false);
                    } else {
                        File file = new File(path);
                        if (file.exists()/* && file.isFile()*/) {
                            buttonOK.setEnabled(true);
                        } else {
                            buttonOK.setEnabled(false);
                        }
                    }
                }
            }
        });

        btnBrowse = new Button(container, SWT.PUSH | SWT.CENTER);
        gridData = new GridData();
        gridData.widthHint = 100;
//        gridData.verticalIndent = 10;
//        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        btnBrowse.setText(Messages.newCertDlgBrwsBtn);
        btnBrowse.setLayoutData(gridData);
        btnBrowse.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                browseBtnListener();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
            }
        });

        return super.createContents(parent);
    }

    /**
     * Browse button for publish settings file
     * selection using file system.
     */
    private void browseBtnListener() {
        FileDialog dialog = new FileDialog(this.getShell());
//        DirectoryDialog dialog = new DirectoryDialog(this.getShell());
        dialog.setText("Choose OpenSSL executable");
        String file = dialog.open();
        if (file != null) {
            txtFile.setText(file);
        }
    }

    @Override
    protected void okPressed() {
        if (txtFile.getText() == null || txtFile.getText().isEmpty() || !(new File(txtFile.getText()).exists()) || !isOpenSSLExecutable()) {
            DefaultLoader.getUIHelper().showError("Must select the OpenSSL executable location.", "OpenSSL");
        } else {
            DefaultLoader.getIdeHelper().setProperty("MSOpenSSLPath", (new File(txtFile.getText())).getParent());
            super.okPressed();
        }
    }

    private boolean isOpenSSLExecutable() {
        String fileName = (new File(txtFile.getText())).getName().toLowerCase();
        return "openssl".equals(fileName) || "openssl.exe".equals(fileName);
    }

    @Override
    protected void cancelPressed() {
        super.cancelPressed();
    }
}
