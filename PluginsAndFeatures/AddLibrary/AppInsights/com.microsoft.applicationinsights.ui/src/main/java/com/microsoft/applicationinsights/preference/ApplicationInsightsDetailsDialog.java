/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.applicationinsights.preference;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Class is intended for displaying the full information about the selected application insight resource.
 */
public class ApplicationInsightsDetailsDialog extends Dialog {
    ApplicationInsightsResource resource;

    protected ApplicationInsightsDetailsDialog(Shell parentShell, ApplicationInsightsResource resource) {
        super(parentShell);
        this.resource = resource;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.appTtl);
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control ctrl = super.createButtonBar(parent);
        Button okButton = getButton(IDialogConstants.OK_ID);
        okButton.setVisible(false);

        Button cancelButton = getButton(IDialogConstants.CANCEL_ID);
        cancelButton.setText("Close");
        return ctrl;
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, false);
        container.setLayout(gridLayout);
        GridData gridData = new GridData();
        gridData.widthHint = 400;
        container.setLayoutData(gridData);

        GridData txtGridData = new GridData();
        txtGridData.verticalIndent = 3;
        txtGridData.grabExcessHorizontalSpace = true;
        txtGridData.horizontalAlignment = SWT.FILL;

        Label lblName = new Label(container, SWT.LEFT);
        lblName.setText(Messages.name);
        Text txtName = new Text(container, SWT.BORDER | SWT.LEFT);
        txtName.setEditable(false);
        txtName.setLayoutData(txtGridData);

        Label lblKey = new Label(container, SWT.LEFT);
        lblKey.setText(Messages.instrKey + ":");
        Text txtKey = new Text(container, SWT.BORDER | SWT.LEFT);
        txtKey.setEditable(false);
        txtKey.setLayoutData(txtGridData);

        Label lblSub = new Label(container, SWT.LEFT);
        lblSub.setText(Messages.sub);
        Text txtSub = new Text(container, SWT.BORDER | SWT.LEFT);
        txtSub.setEditable(false);
        txtSub.setLayoutData(txtGridData);

        Label lblGrp = new Label(container, SWT.LEFT);
        lblGrp.setText(Messages.resGrp);
        Text txtGrp = new Text(container, SWT.BORDER | SWT.LEFT);
        txtGrp.setEditable(false);
        txtGrp.setLayoutData(txtGridData);

        Label lblReg = new Label(container, SWT.LEFT);
        lblReg.setText(Messages.region);
        Text txtReg = new Text(container, SWT.BORDER | SWT.LEFT);
        txtReg.setEditable(false);
        txtReg.setLayoutData(txtGridData);

        // populate values
        txtName.setText(resource.getResourceName());
        txtKey.setText(resource.getInstrumentationKey());
        txtSub.setText(resource.getSubscriptionName());
        txtGrp.setText(resource.getResourceGroup());
        txtReg.setText(resource.getLocation());

        return super.createContents(parent);
    }
}
