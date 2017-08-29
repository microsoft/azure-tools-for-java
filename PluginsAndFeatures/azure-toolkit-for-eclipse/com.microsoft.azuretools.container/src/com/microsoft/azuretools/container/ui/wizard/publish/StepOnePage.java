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

import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.presenters.StepOnePagePresenter;
import com.microsoft.azuretools.container.views.PublishWizardPageView;
import com.microsoft.azuretools.container.views.StepOnePageView;
import com.microsoft.azuretools.core.components.AzureWizardPage;
import com.microsoft.azuretools.core.ui.views.AzureDeploymentProgressNotification;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StepOnePage extends AzureWizardPage implements StepOnePageView, PublishWizardPageView {
    private static final String TEXT_TITLE = "Push Docker Image to Azure Container Registry";
    private static final String TEXT_DESCRIPTION = "Complete the credential of your Azure Container Registry";
    private Text txtRegistryUrl;
    private Text txtUsername;
    private Text txtPassword;
    private StyledText styledText;
    private final FormToolkit formToolkit = new FormToolkit(Display.getDefault());
    private final StepOnePagePresenter<StepOnePage> presenter;

    // org.eclipse.jface.dialogs.DialogPage
    @Override
    public void dispose() {
        presenter.onDetachView();
        super.dispose();
    }

    // com.microsoft.azuretools.core.mvp.ui.base.MvpView
    @Override
    public void onErrorWithException(String message, Exception ex) {
        ConsoleLogger.error(String.format("%s\n%s", message, ex.getMessage()));
        this.showInformation(String.format("%s\n%s", message, ex.getMessage()));
    }

    // com.microsoft.azuretools.container.views.PublishWizardPageView
    @Override
    public void onWizardNextPressed() {
        setWidgetsEnabledStatus(false);
        presenter.onPushLatestImageToRegistry(txtRegistryUrl.getText(), txtUsername.getText(), txtPassword.getText());
    }

    @Override
    public void onWizardFinishPressed() {
        return;
    }

    // com.microsoft.azuretools.container.views.StepOnePageView
    @Override
    public void fillRegistryInfo(String registryUrl, String username, String password) {
        txtRegistryUrl.setText(registryUrl != null ? registryUrl : "");
        txtUsername.setText(username != null ? username : "");
        txtPassword.setText(password != null ? password : "");
    }

    @Override
    public void onRequestPending(Object payload) {
        showInformation("Try pushing image ...");
        setWidgetsEnabledStatus(false);
        AzureDeploymentProgressNotification.createAzureDeploymentProgressNotification(payload.toString(),
                payload.toString(), null, null, "Start");
    }

    @Override
    public void onRequestSucceed(Object payload) {
        showInformation("Task OK");
        setWidgetsEnabledStatus(true);
        ((PublishWizardDialog) this.getContainer()).doNextPressed();
        AzureDeploymentProgressNotification.createAzureDeploymentProgressNotification(payload.toString(),
                payload.toString(), null, null, "Success");
    }

    @Override
    public void onRequestFail(Object payload) {
        showInformation("Task FAIL");
        setWidgetsEnabledStatus(true);
        AzureDeploymentProgressNotification.createAzureDeploymentProgressNotification(payload.toString(),
                payload.toString(), null, null, "Fail");
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
     *            parent composite
     */
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        GridLayout gridLayoutContainer = new GridLayout(2, false);
        gridLayoutContainer.verticalSpacing = 20;
        gridLayoutContainer.marginWidth = 10;
        gridLayoutContainer.marginTop = 20;
        gridLayoutContainer.marginBottom = 20;
        gridLayoutContainer.marginRight = 40;
        gridLayoutContainer.marginLeft = 40;
        container.setLayout(gridLayoutContainer);

        Font boldFont = new Font(this.getShell().getDisplay(), new FontData("Segoe UI", 9, SWT.BOLD));

        Label lblServerUrl = new Label(container, SWT.NONE);
        lblServerUrl.setFont(boldFont);
        GridData gridDataLblServerUrl = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gridDataLblServerUrl.widthHint = 75;
        lblServerUrl.setLayoutData(gridDataLblServerUrl);
        lblServerUrl.setText("Server URL");

        txtRegistryUrl = new Text(container, SWT.BORDER);
        txtRegistryUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblUsername = new Label(container, SWT.NONE);
        lblUsername.setFont(boldFont);
        GridData gridDataLblUsername = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gridDataLblUsername.widthHint = 75;
        lblUsername.setLayoutData(gridDataLblUsername);
        lblUsername.setText("Username");

        txtUsername = new Text(container, SWT.BORDER);
        txtUsername.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblPassword = new Label(container, SWT.NONE);
        lblPassword.setFont(boldFont);
        GridData gridDataLblPassword = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gridDataLblPassword.widthHint = 75;
        lblPassword.setLayoutData(gridDataLblPassword);
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

    // private helpers
    private void initilize() {
        presenter.onLoadRegistryInfo();
    }

    private void setWidgetsEnabledStatus(boolean enableStatus) {
        txtRegistryUrl.setEditable(enableStatus);
        txtUsername.setEditable(enableStatus);
        txtPassword.setEditable(enableStatus);
        ((PublishWizardDialog) this.getContainer()).setProgressBarVisible(!enableStatus);
        ((PublishWizardDialog) this.getContainer()).setNextEnabled(enableStatus);
        ((PublishWizardDialog) this.getContainer()).setFinishEnabled(enableStatus);
        ((PublishWizardDialog) this.getContainer()).setCancelEnabled(enableStatus);
    }

    private void showInformation(String string) {
        if (string == null) {
            return;
        }
        DateFormat df = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        String date = df.format(new Date());
        styledText.append(String.format("[%s]\t%s\n", date, string));
    }
}
