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
package com.microsoft.azuretools.docker.ui.dialogs;

import java.util.logging.Logger;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azuretools.core.components.AzureDialogWrapper;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class AzureSelectKeyVault extends AzureDialogWrapper {
    private static final Logger log =  Logger.getLogger(AzureSelectKeyVault.class.getName());

    private AzureDockerHostsManager dockerManager;
    private String keyvault;

    /**
     * Create the dialog.
     * @param parentShell
     */
    public AzureSelectKeyVault(Shell parentShell, AzureDockerHostsManager dockerManager) {
        super(parentShell);
        setShellStyle(SWT.RESIZE);

        this.dockerManager = dockerManager;
        this.keyvault = null;
    }

    /**
     * Create contents of the dialog.
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite mainContainer = (Composite) super.createDialogArea(parent);
        mainContainer.setLayout(new GridLayout(2, false));

        Label lblAzureKeyVaults = new Label(mainContainer, SWT.NONE);
        GridData gd_lblAzureKeyVaults = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_lblAzureKeyVaults.verticalIndent = 3;
        gd_lblAzureKeyVaults.horizontalIndent = 5;
        lblAzureKeyVaults.setLayoutData(gd_lblAzureKeyVaults);
        lblAzureKeyVaults.setText("Azure key vaults:");

        Combo dockerKeyvaultsComboBox = new Combo(mainContainer, SWT.NONE);
        dockerKeyvaultsComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                keyvault = (String) dockerKeyvaultsComboBox.getText();
            }
        });
        if (dockerManager != null && dockerManager.getDockerVaultsMap() != null) {
            for (String keyvaultName : dockerManager.getDockerVaultsMap().keySet()) {
                dockerKeyvaultsComboBox.add(keyvaultName);
            }
        }
        if (dockerKeyvaultsComboBox.getItemCount() > 0) {
            dockerKeyvaultsComboBox.select(0);
            keyvault = dockerKeyvaultsComboBox.getItem(0);
        }
        GridData gd_dockerKeyvaultsComboBox = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gd_dockerKeyvaultsComboBox.verticalIndent = 3;
        dockerKeyvaultsComboBox.setLayoutData(gd_dockerKeyvaultsComboBox);

        return mainContainer;
    }

    /**
     * Create contents of the button bar.
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(330, 120);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    public String getSelectedKeyvault() {
        return keyvault;
    }
}
