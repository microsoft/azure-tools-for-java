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

package com.microsoft.azuretools.core.ui;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;

public class SrvPriSettingsDialog extends AzureTitleAreaDialogWrapper {
    private Table table;
    private Text textDestinationFolderPath;

    private DirectoryDialog dirDialog;

    // user data of interest
    private List<SubscriptionDetail> sdl;
    private String strDestinationFolderPath;

    public String getDestinationFolder() {
        return strDestinationFolderPath;
    }

    public List<SubscriptionDetail> getSubscriptionDetails() {
        return sdl;
    }

    public static SrvPriSettingsDialog go(Shell parentShell, List<SubscriptionDetail> sdl) {
        SrvPriSettingsDialog d = new SrvPriSettingsDialog(parentShell);
        d.sdl = sdl;
        d.create();

        if (d.open() == Window.OK) {
            return d;
        }
        return null;
    }

    /**
     * Create the dialog.
     * @param parentShell
     */
    private SrvPriSettingsDialog(Shell parentShell) {
        super(parentShell);
        setHelpAvailable(false);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
    }

    /**
     * Create contents of the dialog.
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage("Create Authentication Files");
        setTitle("Create Authentication Files");
        getShell().setText("Create Authentication Files");
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        Label lblInfo = new Label(container, SWT.WRAP);
        GridData gdLblInfo = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        gdLblInfo.heightHint = 82;
        lblInfo.setLayoutData(gdLblInfo);
        lblInfo.setText("A new Active Directory service principal representing this IDE will be created as needed " +
                "and as allowed by your access permissions.\nThe service principal will be granted Contributor-level access to each selected subscription.");

        Label lblNewLabel = new Label(container, SWT.NONE);
        lblNewLabel.setEnabled(true);
        lblNewLabel.setText("Select the subscriptions to create credentials for:");

        table = new Table(container, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION);
        table.setEnabled(true);
        GridData gdTable = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gdTable.heightHint = 300;
        table.setLayoutData(gdTable);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableColumn tblclmnSubscriptionName = new TableColumn(table, SWT.NONE);
        tblclmnSubscriptionName.setWidth(250);
        tblclmnSubscriptionName.setText("Subscription Name");

        TableColumn tblclmnSubscriptionId = new TableColumn(table, SWT.NONE);
        tblclmnSubscriptionId.setWidth(300);
        tblclmnSubscriptionId.setText("Subscription ID");

        for (SubscriptionDetail sd : sdl) {
            TableItem item = new TableItem(table, SWT.NULL);
            item.setText(new String[] {sd.getSubscriptionName(), sd.getSubscriptionId()});
            item.setChecked(sd.isSelected());
        }

        Group grpDestinationFolder = new Group(container, SWT.NONE);
        grpDestinationFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        grpDestinationFolder.setLayout(new GridLayout(3, false));

        Label lblDestinationFolder = new Label(grpDestinationFolder, SWT.NONE);
        lblDestinationFolder.setText("Destination folder:");

        textDestinationFolderPath = new Text(grpDestinationFolder, SWT.BORDER | SWT.READ_ONLY);
        textDestinationFolderPath.setEditable(false);
        textDestinationFolderPath.setEnabled(true);
        textDestinationFolderPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        String initDirPath = System.getProperty("user.home");
        textDestinationFolderPath.setText(initDirPath);
        dirDialog = new DirectoryDialog(this.getShell());
        dirDialog.setFilterPath(initDirPath);
        dirDialog.setText("Select Destination Folder");

        Button btnBrowse = new Button(grpDestinationFolder, SWT.NONE);
        btnBrowse.setEnabled(true);
        btnBrowse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String path = dirDialog.open();
                if (path == null) {
                    return;
                }
                textDestinationFolderPath.setText(path);
            }
        });
        btnBrowse.setText("Browse...");

        return area;
    }

    /**
     * Create contents of the button bar.
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button btnOk = createButton(parent, IDialogConstants.FINISH_ID, IDialogConstants.OK_LABEL, true);
        btnOk.setEnabled(true);
        btnOk.setText("Start");
        btnOk.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onOk();
            }
        });
        Button btnCancel = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        btnCancel.setEnabled(true);
        btnCancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            }
        });
    }

    private void onOk() {
        TableItem[] tia = table.getItems();
        int chekedCount = 0;
        for (TableItem ti : tia) {
            if (ti.getChecked()) {
                chekedCount++;
            }
        }

        if (chekedCount == 0) {
            this.setErrorMessage("Please select at least one subscription");
            return;
        }

        for (int i = 0; i < tia.length; ++i) {
            this.sdl.get(i).setSelected(tia[i].getChecked());
        }

        strDestinationFolderPath = textDestinationFolderPath.getText();

        super.okPressed();
    }
}
