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

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.core.components.AzureDialogWrapper;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
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

public class CreateTableForm extends AzureDialogWrapper {
    private static String NAMING_GUIDELINES_LINK = "<a href=\"http://go.microsoft.com/fwlink/?LinkId=267429\">Naming Guidelines</a>";
    private Button buttonOK;
    private Label nameLabel;
    private Text nameTextField;
    private Link namingGuidelinesLink;
    private ClientStorageAccount storageAccount;
    private Runnable onCreate;

    public CreateTableForm(Shell parentShell, ClientStorageAccount storageAccount) {
        super(parentShell);
        parentShell.setText("Create table");
        this.storageAccount = storageAccount;
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        container.setLayout(gridLayout);
        GridData gridData = new GridData();
        gridData.widthHint = 350;
        container.setLayoutData(gridData);

        nameLabel = new Label(container, SWT.LEFT);
        nameLabel.setText("Enter a name for the new table");
        nameTextField = new Text(container, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        nameTextField.setLayoutData(gridData);
        namingGuidelinesLink = new Link(container, SWT.LEFT);
        namingGuidelinesLink.setText(NAMING_GUIDELINES_LINK);
        namingGuidelinesLink.setLayoutData(gridData);
        namingGuidelinesLink.addSelectionListener(new SelectionAdapter() {
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

        nameTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent modifyEvent) {
                changedName();
            }
        });
//        contentPane.registerKeyboardAction(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                onCancel();
//            }
//        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        return super.createContents(parent);
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control ctrl = super.createButtonBar(parent);
        buttonOK = getButton(IDialogConstants.OK_ID);
        buttonOK.setEnabled(false);
        return ctrl;
    }

    private void changedName() {
        buttonOK.setEnabled(nameTextField.getText().length() > 0);
    }

    @Override
    protected void okPressed() {
        final String name = nameTextField.getText();
        if (!name.matches("^[A-Za-z][A-Za-z0-9]{2,62}$")) {
            DefaultLoader.getUIHelper().showError("Table names must start with a letter, and can contain only letters and numbers.\n" +
                    "Queue names must be from 3 through 63 characters long.", "Azure Explorer");
            return;
        }
        DefaultLoader.getIdeHelper().runInBackground(null, "Creating table...", false, true, "Creating table...", new Runnable() {
            public void run() {
                /*try {
                    for (com.microsoft.tooling.msservices.model.storage.Table table : StorageClientSDKManager.getManager().getTables(storageAccount)) {
                        if (table.getName().equals(name)) {
                            DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    DefaultLoader.getUIHelper().showError("A table with the specified name already exists.", "Service Explorer");
                                }
                            });

                            return;
                        }
                    }

                    com.microsoft.tooling.msservices.model.storage.Table table = new com.microsoft.tooling.msservices.model.storage.Table(name, "");
                    StorageClientSDKManager.getManager().createTable(storageAccount, table);

                    if (onCreate != null) {
                        DefaultLoader.getIdeHelper().invokeLater(onCreate);
                    }
                } catch (AzureCmdException e) {
                    DefaultLoader.getUIHelper().showException("Error creating table", e, "Service explorer", false, true);
                }*/
            }
        });
        super.okPressed();
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }
}
