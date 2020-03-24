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

package com.microsoft.azuretools.core.ui.startup;

import java.awt.Checkbox;
import java.net.URL;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;


public class HDInsightScalaHelpDlg extends Dialog {
    private boolean isShowTips = true;

    public boolean isShowTipsStatus() {
        return isShowTips;
    }

    protected HDInsightScalaHelpDlg(Shell parentShell) {
        super(parentShell);
    }
    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.hdinsgihtPrefTil);
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control ctrl = super.createButtonBar(parent);
        return ctrl;
    }

    protected void okPressed() {
        PluginUtil.forceInstallPluginUsingMarketPlaceAsync(PluginUtil.scalaPluginSymbolicName, PluginUtil.scalaPluginMarketplaceURL, PluginUtil.scalaPluginManualInstallURL);

        super.okPressed();
    }

    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout(1, false);
        container.setLayout(gridLayout);

        Link urlLink = new Link(container, SWT.LEFT);
        urlLink.setText(Messages.hdinsightPerenceQueMsg);

        urlLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().
                    getExternalBrowser().openURL(new URL(event.text));
                } catch (Exception ex) {
                    Activator.getDefault().log(Messages.lnkOpenErrMsg, ex);
                }
            }
        });
        final Button checkButton = new Button(container, SWT.CHECK);
        checkButton.setText("Do not ask again");
        checkButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                isShowTips = !checkButton.getSelection();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
            }
        });

        return super.createContents(parent);
    }
}
