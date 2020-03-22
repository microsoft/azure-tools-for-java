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
package com.microsoft.applicationinsights.preference;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.applicationinsights.util.AILibraryUtil;
import com.microsoft.azuretools.azurecommons.util.WAEclipseHelperMethods;
import com.microsoft.azuretools.core.applicationinsights.ApplicationInsightsPreferences;
import com.microsoft.azuretools.core.utils.PluginUtil;

/**
 * Class registers existing application insights instrumentation keys in local cache, without requiring access to the
 * subscription.
 */
public class ApplicationInsightsAddDialog extends TitleAreaDialog {

    private Text txtName;
    private Text txtKey;
    private Button okButton;

    public ApplicationInsightsAddDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.appTtl);
        Image image = AILibraryUtil.getImage();
        if (image != null) {
            setTitleImage(image);
        }
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control ctrl = super.createButtonBar(parent);
        okButton = getButton(IDialogConstants.OK_ID);
        okButton.setEnabled(false);
        return ctrl;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(Messages.addKeyTtl);
        setMessage(Messages.addKeyMsg);
        setHelpAvailable(false);

        GridLayout gridLayout = new GridLayout();
        GridData gridData = new GridData();
        gridLayout.numColumns = 2;
        gridLayout.marginBottom = 10;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(gridLayout);
        container.setLayoutData(gridData);
        createNameCmpnt(container);
        createKeyCmpnt(container);

        return super.createDialogArea(parent);
    }

    /**
     * Create application insights resource name UI and its listeners.
     */
    private void createNameCmpnt(Composite container) {
        Label lblName = new Label(container, SWT.LEFT);
        GridData gridData = gridDataForLbl();
        lblName.setLayoutData(gridData);
        lblName.setText(Messages.name);

        txtName = new Text(container, SWT.LEFT | SWT.BORDER);
        gridData = gridDataForText(180);
        txtName.setLayoutData(gridData);
        txtName.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                enableOkBtn();
            }
        });
    }

    /**
     * Create instrumentation key UI components and its listeners.
     */
    private void createKeyCmpnt(Composite container) {
        Label lblKey = new Label(container, SWT.LEFT);
        GridData gridData = gridDataForLbl();
        lblKey.setLayoutData(gridData);
        lblKey.setText(Messages.key);

        txtKey = new Text(container, SWT.LEFT | SWT.BORDER);
        gridData = gridDataForText(180);
        txtKey.setLayoutData(gridData);
        txtKey.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                enableOkBtn();
            }
        });
    }

    /**
     * Method creates grid data for label field.
     */
    private GridData gridDataForLbl() {
        GridData gridData = new GridData();
        gridData.horizontalIndent = 5;
        gridData.verticalIndent = 10;
        return gridData;
    }

    /**
     * Method creates grid data for text field.
     */
    private GridData gridDataForText(int width) {
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.END;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.widthHint = width;
        gridData.verticalIndent = 10;
        gridData.grabExcessHorizontalSpace = true;
        return gridData;
    }

    /**
     * Method enables or disables OK button. Disable OK button if resource name or instrumentation key is not specified.
     */
    private void enableOkBtn() {
        if (okButton != null) {
            if (txtName.getText().trim().isEmpty() || txtKey.getText().trim().isEmpty()) {
                okButton.setEnabled(false);
            } else {
                okButton.setEnabled(true);
            }
        }
    }

    @Override
    protected void okPressed() {
        boolean isValid = false;
        String key = txtKey.getText().trim();
        String name = txtName.getText().trim();
        int index = ApplicationInsightsResourceRegistry.getResourceIndexAsPerKey(key);
        if (index >= 0) {
            // registry already has an entry with same key. Show error
            ApplicationInsightsResource resource = ApplicationInsightsResourceRegistry.getAppInsightsResrcList()
                    .get(index);
            // error message can be more descriptive by adding subscription name after resource name.
            // might be useful in the scenarios where same resource name exists in different subscriptions
            PluginUtil.displayErrorDialog(getShell(), Messages.appTtl,
                    String.format(Messages.sameKeyErrMsg, resource.getResourceName()));
        } else {
            ArrayList<String> resourceNameList = ApplicationInsightsResourceRegistry.getResourcesNames();
            if (resourceNameList.contains(name)) {
                // registry already has entry with same name. Show error
                PluginUtil.displayErrorDialog(getShell(), Messages.appTtl, Messages.sameNameErrMsg);
            } else {
                // check instrumentation key is valid or not and show error if its invalid.
                if (WAEclipseHelperMethods.isValidInstrumentationKey(key)) {
                    ApplicationInsightsResource resourceToAdd = new ApplicationInsightsResource(name, key,
                            Messages.unknown, Messages.unknown, Messages.unknown, Messages.unknown, false);
                    ApplicationInsightsResourceRegistry.getAppInsightsResrcList().add(resourceToAdd);
                    ApplicationInsightsPreferences.save();
                    isValid = true;
                } else {
                    PluginUtil.displayErrorDialog(getShell(), Messages.appTtl, Messages.keyErrMsg);
                }
            }
        }
        if (isValid) {
            super.okPressed();
        }
    }
}
