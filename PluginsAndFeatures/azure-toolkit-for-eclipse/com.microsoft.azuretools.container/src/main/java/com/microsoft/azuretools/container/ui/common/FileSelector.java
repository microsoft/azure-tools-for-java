/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.container.ui.common;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class FileSelector extends Composite {
    private Text txtFilePath;

    /**
     * Create the composite.
     */
    public FileSelector(Composite parent, int style, boolean isDir, String btnText, String basePath, String textLabel) {
        super(parent, style);
        GridLayout gridLayout = new GridLayout(3, false);
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        setLayout(gridLayout);

        Label lblNewLabel = new Label(this, SWT.NONE);
        lblNewLabel.setText(textLabel);

        txtFilePath = new Text(this, SWT.BORDER);
        txtFilePath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));


        Button btnFileSelector = new Button(this, SWT.NONE);
        btnFileSelector.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true, 1, 1));
        btnFileSelector.setText(btnText);

        btnFileSelector.addListener(SWT.Selection, event -> {
            if (isDir) {
                DirectoryDialog dirDialog = new DirectoryDialog(parent.getShell(), SWT.OPEN);
                dirDialog.setFilterPath(basePath);
                String firstPath = dirDialog.open();
                if (firstPath != null) {
                    txtFilePath.setText(firstPath);
                }
            } else {
                FileDialog fileDialog = new FileDialog(parent.getShell(), SWT.OPEN);
                fileDialog.setFilterPath(basePath);
                String firstFile = fileDialog.open();
                if (firstFile != null) {
                    txtFilePath.setText(firstFile);
                }
            }

        });

    }

    /**
     * @wbp.parser.constructor
     */
    public FileSelector(Composite parent, int style, boolean isDir, String btnText, String basePath) {
        this(parent, style, isDir, btnText, basePath, "");
    }

    public void setFilePath(String filePath) {
        txtFilePath.setText(filePath);
    }

    public String getFilePath() {
        return txtFilePath.getText();
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }
}
