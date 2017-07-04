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

import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.azuretools.container.presenters.StepOnePagePresenter;
import com.microsoft.azuretools.container.views.PublishWizardPageView;
import com.microsoft.azuretools.container.views.StepOnePageView;
import com.microsoft.azuretools.core.components.AzureWizardPage;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.custom.ScrolledComposite;

public class StepOnePage extends AzureWizardPage implements StepOnePageView, PublishWizardPageView {
    private static final String TEXT_TITLE = "Push Docker Image to Azure Container Registry";
    private static final String TEXT_DESCRIPTION = "Complete the credential of your Azure Container Registry";
    private Text txtRegistryUrl;
    private Text txtUsername;
    private Text txtPassword;
    private StyledText styledText;
    private final FormToolkit formToolkit = new FormToolkit(Display.getDefault());
    private final StepOnePagePresenter<StepOnePage> presenter;

    private void initilize() {
        presenter.onLoadRegistryInfo();
    }

    private void setWidgetsEnabledStatus(boolean enableStatus) {
        txtRegistryUrl.setEditable(enableStatus);
        txtUsername.setEditable(enableStatus);
        txtPassword.setEditable(enableStatus);

        ((PublishWizardDialog) this.getContainer()).setNextEnabled(enableStatus);
        ((PublishWizardDialog) this.getContainer()).setFinishEnabled(enableStatus);
        ((PublishWizardDialog) this.getContainer()).setCancelEnabled(enableStatus);
    }

    private void showInformation(String string) {
        if (string == null) {
            return;
        }
        styledText.append(String.format("[%s]\t%s\n", (new Date()).toString(), string));
    }

    /**
     * Create the wizard.
     */
    public StepOnePage() {
        super("StepOnePage");
        presenter = new StepOnePagePresenter<StepOnePage>();
        presenter.onAttachView(this);
        setTitle(TEXT_TITLE);
        setDescription(TEXT_DESCRIPTION);
    }

    /**
     * Create contents of the wizard.
     * 
     * @param parent
     */
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        GridLayout gl_container = new GridLayout(2, false);
        gl_container.verticalSpacing = 20;
        gl_container.marginWidth = 10;
        gl_container.marginTop = 20;
        gl_container.marginBottom = 20;
        gl_container.marginRight = 40;
        gl_container.marginLeft = 40;
        container.setLayout(gl_container);
        
        Font boldFont = new Font(this.getShell().getDisplay(), new FontData("Segoe UI", 9, SWT.BOLD));
        
        Label lblServerUrl = new Label(container, SWT.NONE);
        lblServerUrl.setFont(boldFont);
        GridData gd_lblServerUrl = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblServerUrl.widthHint = 75;
        lblServerUrl.setLayoutData(gd_lblServerUrl);
        lblServerUrl.setText("Server URL");

        txtRegistryUrl = new Text(container, SWT.BORDER);
        txtRegistryUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblUsername = new Label(container, SWT.NONE);
        lblUsername.setFont(boldFont);
        GridData gd_lblUsername = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblUsername.widthHint = 75;
        lblUsername.setLayoutData(gd_lblUsername);
        lblUsername.setText("Username");

        txtUsername = new Text(container, SWT.BORDER);
        txtUsername.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblPassword = new Label(container, SWT.NONE);
        lblPassword.setFont(boldFont);
        GridData gd_lblPassword = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblPassword.widthHint = 75;
        lblPassword.setLayoutData(gd_lblPassword);
        lblPassword.setText("Password");

        txtPassword = new Text(container, SWT.BORDER | SWT.PASSWORD);
        txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        new Label(container, SWT.NONE);

        ScrolledComposite cmpoInformation = new ScrolledComposite(container, SWT.BORDER | SWT.V_SCROLL);
        cmpoInformation.setAlwaysShowScrollBars(true);
        cmpoInformation.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        formToolkit.adapt(cmpoInformation);
        formToolkit.paintBordersFor(cmpoInformation);
        cmpoInformation.setExpandHorizontal(true);
        cmpoInformation.setExpandVertical(true);

        styledText = new StyledText(cmpoInformation, SWT.BORDER | SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP);
        formToolkit.adapt(styledText);
        formToolkit.paintBordersFor(styledText);
        cmpoInformation.setContent(styledText);
        cmpoInformation.setMinSize(styledText.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        // always scroll styledText to end
        styledText.addListener(SWT.Modify, event -> styledText.setTopIndex(styledText.getLineCount() - 1));

        Point size = getShell().computeSize(600, 450);
        getShell().setSize(size);
        
        initilize();
    }

    @Override
    public void dispose() {
        presenter.onDetachView();
        super.dispose();
    }

    @Override
    public void onWizardNextPressed() {
        setWidgetsEnabledStatus(false);
        presenter.onPushLatestImageToRegistry(txtRegistryUrl.getText(), txtUsername.getText(), txtPassword.getText());
    }

    @Override
    public void onWizardFinishPressed() {
        return;
    }

    @Override
    public void fillRegistryInfo(String registryUrl, String username, String password) {
        txtRegistryUrl.setText(registryUrl != null ? registryUrl : "");
        txtUsername.setText(username != null ? username : "");
        txtPassword.setText(password != null ? password : "");
    }

    @Override
    public void onRequestPending() {
        showInformation("Try pushing image ...");
        setWidgetsEnabledStatus(false);
    }

    @Override
    public void onRequestSucceed() {
        showInformation("Task OK");
        setWidgetsEnabledStatus(true);
        ((PublishWizardDialog) this.getContainer()).doNextPressed();
    }

    @Override
    public void onRequestFail(String errorMsg) {
        if(errorMsg != null) {
            showInformation(errorMsg);
        }
        showInformation("Task FAIL");
        setWidgetsEnabledStatus(true);
    }

}
