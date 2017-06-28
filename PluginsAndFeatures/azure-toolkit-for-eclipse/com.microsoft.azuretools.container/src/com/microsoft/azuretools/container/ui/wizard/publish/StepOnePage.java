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
import com.microsoft.azuretools.core.components.AzureWizardPage;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.custom.StyledText;

import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Button;

public class StepOnePage extends AzureWizardPage {
    private Text txtRegistryUrl;
    private Text txtUsername;
    private Text txtPassword;
    private StyledText styledText;
    private final FormToolkit formToolkit = new FormToolkit(Display.getDefault());
    private final StepOnePagePresenter<StepOnePage> presenter;
    private Button btnValidate;

    // Call Presenter
    public void loadRegistryInfo() {
        presenter.onLoadRegistryInfo();
    }

    // View Actions
    public void setWidgetsEnabledStatus(boolean enableStatus) {
        btnValidate.setEnabled(enableStatus);

        txtRegistryUrl.setEditable(enableStatus);
        txtUsername.setEditable(enableStatus);
        txtPassword.setEditable(enableStatus);
    }

    public void fillRegistryInfo(String registryUrl, String username, String password) {
        txtRegistryUrl.setText(registryUrl != null ? registryUrl : "");
        txtUsername.setText(username != null ? username : "");
        txtPassword.setText(password != null ? password : "");
    }

    public void showInfomation(String string) {
        if (string == null) {
            return;
        }
        styledText.append(String.format("[%s]\t%s\n", (new Date()).toString(), string));
    }

    /**
     * Create the wizard.
     */
    public StepOnePage() {
        super("wizardPage");
        presenter = new StepOnePagePresenter<StepOnePage>();
        presenter.onAttachView(this);

        setTitle("Setting Private Docker Repo Credential");
        setDescription("TBD");
    }

    /**
     * Create contents of the wizard.
     * 
     * @param parent
     */
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        container.setLayout(new GridLayout(1, false));

        Composite cmpoDockerRepoCredential = new Composite(container, SWT.NONE);
        GridData gd_cmpoDockerRepoCredential = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        gd_cmpoDockerRepoCredential.widthHint = 550;
        cmpoDockerRepoCredential.setLayoutData(gd_cmpoDockerRepoCredential);
        cmpoDockerRepoCredential.setLayout(new GridLayout(2, false));

        Label lblServerUrl = new Label(cmpoDockerRepoCredential, SWT.NONE);
        lblServerUrl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblServerUrl.setText("Server URL");

        txtRegistryUrl = new Text(cmpoDockerRepoCredential, SWT.BORDER);
        txtRegistryUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblUsername = new Label(cmpoDockerRepoCredential, SWT.NONE);
        lblUsername.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblUsername.setText("Username");

        txtUsername = new Text(cmpoDockerRepoCredential, SWT.BORDER);
        txtUsername.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblPassword = new Label(cmpoDockerRepoCredential, SWT.NONE);
        lblPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblPassword.setText("Password");

        txtPassword = new Text(cmpoDockerRepoCredential, SWT.BORDER | SWT.PASSWORD);
        txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        new Label(cmpoDockerRepoCredential, SWT.NONE);

        btnValidate = new Button(cmpoDockerRepoCredential, SWT.NONE);
        btnValidate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        formToolkit.adapt(btnValidate, true, true);
        btnValidate.setText("Validate");
        btnValidate.addListener(SWT.Selection, event -> onBtnValidateSelection());

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

        loadRegistryInfo();
        setPageComplete(false);
    }

    private void onBtnValidateSelection() {
        setWidgetsEnabledStatus(false);
        sendButtonClickedTelemetry("onBtnValidateSelection");
        presenter.onPushLatestImageToRegistry(txtRegistryUrl.getText(), txtUsername.getText(), txtPassword.getText());
    }

    @Override
    public void dispose() {
        presenter.onDetachView();
        super.dispose();
    }
}
