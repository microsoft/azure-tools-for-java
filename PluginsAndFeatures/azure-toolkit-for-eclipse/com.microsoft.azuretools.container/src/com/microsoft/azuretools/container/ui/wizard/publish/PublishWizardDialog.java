/**
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

package com.microsoft.azuretools.container.ui.wizard.publish;

import com.microsoft.azuretools.container.views.PublishWizardPageView;
import com.microsoft.azuretools.core.components.AzureWizardDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;


public class PublishWizardDialog extends AzureWizardDialog {
    private ProgressBar progressBar;
    
    public PublishWizardDialog(Shell parentShell, IWizard newWizard) {
        super(parentShell, newWizard);
        this.setHelpAvailable(false);
    }

    @Override
    protected void nextPressed() {
        ((PublishWizardPageView) this.getCurrentPage()).onWizardNextPressed();
    }

    @Override
    protected void finishPressed() {
        ((PublishWizardPageView) this.getCurrentPage()).onWizardFinishPressed();
    }

    public void doFinishPressed() {
        super.finishPressed();
    }

    public void doCancelPressed() {
        super.cancelPressed();
    }

    public void doNextPressed() {
        super.nextPressed();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayout gridLayoutContainer = new GridLayout(1, false);
        gridLayoutContainer.verticalSpacing = 0;
        gridLayoutContainer.marginHeight = 0;
        gridLayoutContainer.marginWidth = 0;
        container.setLayout(gridLayoutContainer);
        GridData gridDataContainer = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        container.setLayoutData(gridDataContainer);
        progressBar = new ProgressBar(container, SWT.INDETERMINATE);
        GridData gd_progressBar = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gd_progressBar.heightHint = 3;
        progressBar.setLayoutData(gd_progressBar);
        progressBar.setVisible(false);
        Composite content = (Composite) super.createDialogArea(container);
        GridData gridDataContent = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        content.setLayoutData(gridDataContent);
        return container;
    }

    public void setCancelEnabled(boolean enabled) {
        Button btnCancel = getButton(IDialogConstants.CANCEL_ID);
        btnCancel.setEnabled(enabled);
    }

    public void setBackEnabled(boolean enabled) {
        Button btnBack = getButton(IDialogConstants.BACK_ID);
        btnBack.setEnabled(enabled);
    }

    public void setNextEnabled(boolean enabled) {
        Button btnNext = getButton(IDialogConstants.NEXT_ID);
        btnNext.setEnabled(enabled);
    }

    public void setFinishEnabled(boolean enabled) {
        Button btnFinish = getButton(IDialogConstants.FINISH_ID);
        btnFinish.setEnabled(enabled);
    }

    /**
     * set enabled status of all buttons.
     * 
     * @param enableStatus
     */
    public void setButtonsEnabled(boolean enableStatus) {
        setFinishEnabled(enableStatus);
        setNextEnabled(enableStatus);
        setBackEnabled(enableStatus);
        setCancelEnabled(enableStatus);
    }

    public void setProgressBarVisible(boolean visible){
        progressBar.setVisible(visible);
    }

}
