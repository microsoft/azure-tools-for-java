/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.container.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

class WebAppTableComposite extends Composite {
    Table tblWebApps;
    Button btnRefresh;

    /**
     * Create the composite.
     */
    public WebAppTableComposite(Composite parent, int style) {
        super(parent, style);
        setLayout(new GridLayout(2, false));

        tblWebApps = new Table(this, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gdTblWebApps = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gdTblWebApps.minimumHeight = 200;
        tblWebApps.setLayoutData(gdTblWebApps);
        tblWebApps.setSize(517, 200);
        tblWebApps.setLinesVisible(true);
        tblWebApps.setHeaderVisible(true);

        TableColumn tableColumn = new TableColumn(tblWebApps, SWT.LEFT);
        tableColumn.setWidth(250);
        tableColumn.setText("Name");

        TableColumn tableColumn1 = new TableColumn(tblWebApps, SWT.LEFT);
        tableColumn1.setWidth(250);
        tableColumn1.setText("Resource group");

        btnRefresh = new Button(this, SWT.NONE);
        btnRefresh.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
        btnRefresh.setSize(73, 28);
        btnRefresh.addListener(SWT.Selection, event -> onBtnNewSelection());
        btnRefresh.setText("Refresh");

    }

    private void onBtnNewSelection() {

    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

}
