package com.microsoft.azuretools.container.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
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
     *
     * @param parent
     * @param style
     */
    public WebAppTableComposite(Composite parent, int style) {
        super(parent, style);
        setLayout(new FillLayout(SWT.HORIZONTAL));

        Composite composite_1 = new Composite(this, SWT.NONE);
        composite_1.setLayout(new GridLayout(2, false));

        tblWebApps = new Table(composite_1, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gd_tblWebApps = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_tblWebApps.minimumHeight = 200;
        gd_tblWebApps.widthHint = 331;
        tblWebApps.setLayoutData(gd_tblWebApps);
        tblWebApps.setLinesVisible(true);
        tblWebApps.setHeaderVisible(true);

        TableColumn tableColumn = new TableColumn(tblWebApps, SWT.LEFT);
        tableColumn.setWidth(250);
        tableColumn.setText("Name");

        TableColumn tableColumn_1 = new TableColumn(tblWebApps, SWT.LEFT);
        tableColumn_1.setWidth(250);
        tableColumn_1.setText("Resource group");

        btnRefresh = new Button(composite_1, SWT.NONE);
        btnRefresh.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, true, 1, 1));
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
