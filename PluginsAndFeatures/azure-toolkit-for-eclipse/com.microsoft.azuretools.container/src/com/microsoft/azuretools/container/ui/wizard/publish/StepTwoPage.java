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

import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.DockerRuntime;
import com.microsoft.azuretools.container.presenters.StepTwoPagePresenter;
import com.microsoft.azuretools.container.utils.RequestPayload;
import com.microsoft.azuretools.container.views.PublishWizardPageView;
import com.microsoft.azuretools.container.views.StepTwoPageView;
import com.microsoft.azuretools.core.components.AzureWizardPage;
import com.microsoft.azuretools.core.ui.views.AzureDeploymentProgressNotification;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class StepTwoPage extends AzureWizardPage implements StepTwoPageView, PublishWizardPageView {
    private static final String MESSAGE_DEPLOY_SUCCESS = "Web App on Linux successfully deployed.";
    private static final String TEXT_DESCRIPTION = "Select existing or create new Web App to deploy";
    private static final String TEXT_TITLE = "Deploy to Azure Web App on Linux";
    private static final String TEXT_BUTTON_REFRESH = "Refresh List";
    private static final String TEXT_TAB_UPDATE = "Use Existing";
    private static final String TEXT_TAB_CREATE = "Create New";
    private static final String REGEX_VALID_APP_NAME = "^[\\w-]*\\w+$";
    private static final String REGEX_VALID_RG_NAME = "^[\\w-]*\\w+$";

    private final FormToolkit formToolkit = new FormToolkit(Display.getDefault());
    private Table tableWebApps;
    private final StepTwoPagePresenter<StepTwoPageView> presenter;
    private Button btnRefresh;

    private Text textAppName;
    private Combo comboResourceGroup;
    private Combo comboSubscription;
    private Text textResourceGroupName;
    private Button btnResourceGroupCreateNew;
    private Button btnResourceGroupUseExisting;
    private TabItem tbtmCreate;
    private TabItem tbtmUpdate;
    private TabFolder tabFolder;

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
        MessageDialog.openError(getShell(), message, ex.getMessage());
    }

    // com.microsoft.azuretools.container.views.PublishWizardPageView
    @Override
    public void onWizardNextPressed() {
        return;
    }

    @Override
    public void onWizardFinishPressed() {
        int index = tabFolder.getSelectionIndex();
        if (tabFolder.getItem(index).getText().equals(TEXT_TAB_CREATE)) {
            deployToNewWebApp();
        } else if (tabFolder.getItem(index).getText().equals(TEXT_TAB_UPDATE)) {
            deployToExisitingWebApp();
        }
    }

    // com.microsoft.azuretools.container.views.StepTwoPageView
    @Override
    public void onRequestPending(Object payload) {
        setWidgetsEnabledStatus(false);
        ((PublishWizardDialog) this.getContainer()).setButtonsEnabled(false);
        AzureDeploymentProgressNotification.createAzureDeploymentProgressNotification(payload.toString(),
                payload.toString(), null, null, "Start");
    }

    @Override
    public void onRequestSucceed(Object payload) {
        setWidgetsEnabledStatus(true);
        ((PublishWizardDialog) this.getContainer()).setButtonsEnabled(true);
        ((PublishWizardDialog) this.getContainer()).updateButtons();
        setPageComplete(pageCompleteStatus());
        AzureDeploymentProgressNotification.createAzureDeploymentProgressNotification(((RequestPayload) payload).name(),
                ((RequestPayload) payload).name(), ((RequestPayload) payload).url(), null, "Success");
    }

    @Override
    public void onRequestFail(Object payload) {
        setWidgetsEnabledStatus(true);
        ((PublishWizardDialog) this.getContainer()).setButtonsEnabled(true);
        ((PublishWizardDialog) this.getContainer()).updateButtons();
        setPageComplete(pageCompleteStatus());
        AzureDeploymentProgressNotification.createAzureDeploymentProgressNotification(payload.toString(),
                payload.toString(), null, null, "Fail");
    }

    @Override
    public void fillSubscriptions(List<SubscriptionDetail> sdl) {
        if (sdl == null || sdl.size() <= 0) {
            return;
        }
        comboSubscription.removeAll();
        for (SubscriptionDetail sd : sdl) {
            comboSubscription.add(sd.getSubscriptionName());
        }
        if (comboSubscription.getItemCount() > 0) {
            comboSubscription.select(0);
        }
    }

    @Override
    public void fillResourceGroups(List<ResourceGroup> rgl) {
        if (rgl == null || rgl.size() <= 0) {
            return;
        }
        comboResourceGroup.removeAll();
        for (ResourceGroup rg : rgl) {
            comboResourceGroup.add(rg.name());
        }
        if (comboResourceGroup.getItemCount() > 0) {
            comboResourceGroup.select(0);
        }
    }

    @Override
    public void finishDeploy() {
        ConsoleLogger.info(MESSAGE_DEPLOY_SUCCESS);
        ((PublishWizardDialog) this.getContainer()).doFinishPressed();
    }

    /**
     * Fill Web Apps on Linux into the table.
     * 
     * @param wal
     *            list of Web Apps on Linux
     */
    @Override
    public void fillWebApps(List<SiteInner> wal) {
        tableWebApps.removeAll();
        for (SiteInner si : wal) {
            TableItem it = new TableItem(tableWebApps, SWT.NULL);
            it.setText(new String[] { si.name(), si.resourceGroup() });
        }
    }

    /**
     * Create the wizard.
     */
    public StepTwoPage() {
        super("StepTwoPage");
        presenter = new StepTwoPagePresenter<StepTwoPageView>();
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
        GridLayout gridLayoutContainer = new GridLayout(1, false);
        gridLayoutContainer.verticalSpacing = 0;
        gridLayoutContainer.marginHeight = 0;
        gridLayoutContainer.marginWidth = 0;
        container.setLayout(gridLayoutContainer);

        tabFolder = new TabFolder(container, SWT.NONE);
        GridData gd_tabFolder = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_tabFolder.minimumWidth = 580;
        tabFolder.setLayoutData(gd_tabFolder);
        formToolkit.adapt(tabFolder);
        formToolkit.paintBordersFor(tabFolder);
        tabFolder.addListener(SWT.Selection, event -> onTabFolderSelection());

        tbtmUpdate = new TabItem(tabFolder, SWT.NONE);
        tbtmUpdate.setText(TEXT_TAB_UPDATE);

        Composite compositeExisting = new Composite(tabFolder, SWT.NONE);
        tbtmUpdate.setControl(compositeExisting);
        formToolkit.paintBordersFor(compositeExisting);
        compositeExisting.setLayout(new GridLayout(1, false));

        Composite cmpoWebAppOnLinux = formToolkit.createComposite(compositeExisting, SWT.NONE);
        GridData gridDataCmpoWebAppOnLinux = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gridDataCmpoWebAppOnLinux.minimumWidth = 530;
        gridDataCmpoWebAppOnLinux.widthHint = 569;
        gridDataCmpoWebAppOnLinux.heightHint = 255;
        cmpoWebAppOnLinux.setLayoutData(gridDataCmpoWebAppOnLinux);
        formToolkit.paintBordersFor(cmpoWebAppOnLinux);
        cmpoWebAppOnLinux.setLayout(new GridLayout(2, false));

        Composite cmpoWebAppsTable = formToolkit.createComposite(cmpoWebAppOnLinux, SWT.NONE);
        cmpoWebAppsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        cmpoWebAppsTable.setLayout(new FillLayout(SWT.HORIZONTAL));
        formToolkit.paintBordersFor(cmpoWebAppsTable);

        tableWebApps = new Table(cmpoWebAppsTable, SWT.BORDER | SWT.FULL_SELECTION);
        tableWebApps.setHeaderVisible(true);
        tableWebApps.setLinesVisible(true);
        formToolkit.adapt(tableWebApps);
        formToolkit.paintBordersFor(tableWebApps);
        tableWebApps.addListener(SWT.Selection, ecent -> onTableWebAppsSelection());

        TableColumn tblclmnName = new TableColumn(tableWebApps, SWT.LEFT);
        tblclmnName.setWidth(200);
        tblclmnName.setText("Name");

        TableColumn tblclmnResourceGroup = new TableColumn(tableWebApps, SWT.LEFT);
        tblclmnResourceGroup.setWidth(275);
        tblclmnResourceGroup.setText("Resource group");

        Composite cmpoActionButtons = formToolkit.createComposite(cmpoWebAppOnLinux, SWT.NONE);
        GridLayout gl_cmpoActionButtons = new GridLayout(1, false);
        gl_cmpoActionButtons.verticalSpacing = 0;
        gl_cmpoActionButtons.marginWidth = 0;
        gl_cmpoActionButtons.marginHeight = 0;
        cmpoActionButtons.setLayout(gl_cmpoActionButtons);
        cmpoActionButtons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
        formToolkit.paintBordersFor(cmpoActionButtons);

        btnRefresh = new Button(cmpoActionButtons, SWT.NONE);

        btnRefresh.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));
        formToolkit.adapt(btnRefresh, true, true);
        btnRefresh.setText(TEXT_BUTTON_REFRESH);
        btnRefresh.addListener(SWT.Selection, event -> onBtnRefreshSelection());

        tbtmCreate = new TabItem(tabFolder, SWT.NONE);
        tbtmCreate.setText(TEXT_TAB_CREATE);

        Composite compositeNew = new Composite(tabFolder, SWT.NONE);
        tbtmCreate.setControl(compositeNew);
        formToolkit.paintBordersFor(compositeNew);

        GridLayout gridLayoutComposite = new GridLayout(1, false);
        gridLayoutComposite.marginHeight = 0;
        compositeNew.setLayout(gridLayoutComposite);
        compositeNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

        Group grpAppService = new Group(compositeNew, SWT.NONE);
        GridData gridDataGrpAppService = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gridDataGrpAppService.heightHint = 75;
        grpAppService.setLayoutData(gridDataGrpAppService);
        GridLayout gridLayoutGrpAppService = new GridLayout(3, false);
        gridLayoutGrpAppService.verticalSpacing = 15;
        gridLayoutGrpAppService.marginWidth = 10;
        grpAppService.setLayout(gridLayoutGrpAppService);

        Label lblAppName = new Label(grpAppService, SWT.NONE);
        GridData gridDataLblAppName = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gridDataLblAppName.widthHint = 75;
        lblAppName.setLayoutData(gridDataLblAppName);
        lblAppName.setText("Enter name");

        textAppName = new Text(grpAppService, SWT.BORDER);
        textAppName.addListener(SWT.Modify, event -> onTextAppNameModify());
        textAppName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textAppName.setMessage("<enter name>");

        Label lblazurewebsitescom = new Label(grpAppService, SWT.NONE);
        lblazurewebsitescom.setText(".azurewebsites.net");

        Label lblSubscription = new Label(grpAppService, SWT.NONE);
        lblSubscription.setText("Subscription");

        comboSubscription = new Combo(grpAppService, SWT.READ_ONLY);
        comboSubscription.addListener(SWT.Selection, event -> onComboSubscriptionSelection());
        comboSubscription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String date = df.format(new Date());
        textAppName.setText("linuxwebapp-" + date);

        Label lblResourceGroup = new Label(grpAppService, SWT.NONE);
        lblResourceGroup.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        formToolkit.adapt(lblResourceGroup, true, true);
        lblResourceGroup.setText("Resource Group:");
        lblResourceGroup.setBackground(grpAppService.getBackground());

        btnResourceGroupCreateNew = new Button(grpAppService, SWT.RADIO);
        btnResourceGroupCreateNew.addListener(SWT.Selection, event -> radioResourceGroupLogic());
        btnResourceGroupCreateNew.setSelection(true);
        btnResourceGroupCreateNew.setText("Create New");

        textResourceGroupName = new Text(grpAppService, SWT.BORDER);
        textResourceGroupName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        textResourceGroupName.addListener(SWT.Modify, event -> onTextResourceGroupNameModify());
        textResourceGroupName.setBounds(0, 0, 64, 19);
        textResourceGroupName.setMessage("<enter name>");
        textResourceGroupName.setText("rg-webapp-" + date);

        btnResourceGroupUseExisting = new Button(grpAppService, SWT.RADIO);
        btnResourceGroupUseExisting.addListener(SWT.Selection, event -> radioResourceGroupLogic());
        btnResourceGroupUseExisting.setText("Use Existing");

        comboResourceGroup = new Combo(grpAppService, SWT.READ_ONLY);
        comboResourceGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        comboResourceGroup.setEnabled(false);
        comboResourceGroup.setBounds(0, 0, 26, 22);

        initialize();
    }

    // private helpers
    private void onTableWebAppsSelection() {
        setPageComplete(pageCompleteStatus());
    }

    private void onTabFolderSelection() {
        setPageComplete(pageCompleteStatus());
    }

    private void onBtnRefreshSelection() {
        sendButtonClickedTelemetry("onBtnRefreshSelection");
        showLoading();
        presenter.onRefreshWebAppsOnLinux();
    }

    private void onComboSubscriptionSelection() {
        int index = comboSubscription.getSelectionIndex();
        presenter.onChangeSubscription(index);
        setPageComplete(pageCompleteStatus());
    }

    private void onTextAppNameModify() {
        if (validateAppName(textAppName.getText())) {
            textAppName.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
        } else {
            textAppName.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
        }
        setPageComplete(pageCompleteStatus());
    }

    private void onTextResourceGroupNameModify() {
        if (validateResourceGroupName(textResourceGroupName.getText())) {
            textResourceGroupName.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
        } else {
            textResourceGroupName.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
        }
        setPageComplete(pageCompleteStatus());
    }

    private void radioResourceGroupLogic() {
        boolean enabled = btnResourceGroupCreateNew.getSelection();
        textResourceGroupName.setEnabled(enabled);
        comboResourceGroup.setEnabled(!enabled);
        setPageComplete(pageCompleteStatus());
    }

    private void initialize() {
        setPageComplete(false);
        showLoading();
        presenter.onListWebAppsOnLinux();
    }

    private boolean pageCompleteStatus() {
        int index = tabFolder.getSelectionIndex();
        if (tabFolder.getItem(index).getText().equals(TEXT_TAB_CREATE)) {
            return isTabCreateComplete();
        } else if (tabFolder.getItem(index).getText().equals(TEXT_TAB_UPDATE)) {
            return isTabUpdateComplete();
        } else {
            return super.isPageComplete();
        }
    }

    private boolean isTabUpdateComplete() {
        return this.tableWebApps.getSelectionIndex() >= 0;
    }

    private boolean isTabCreateComplete() {
        boolean a = this.comboSubscription.getSelectionIndex() >= 0;
        boolean d = validateAppName(this.textAppName.getText());
        boolean b = btnResourceGroupCreateNew.getSelection()
                && validateResourceGroupName(this.textResourceGroupName.getText());
        boolean c = this.btnResourceGroupUseExisting.getSelection() && this.comboResourceGroup.getSelectionIndex() >= 0;
        return a && d && (b || c);
    }

    private boolean validateAppName(String text) {
        return text.matches(REGEX_VALID_APP_NAME);
    }

    private boolean validateResourceGroupName(String text) {
        return text.matches(REGEX_VALID_RG_NAME);
    }

    private void deployToExisitingWebApp() {
        presenter.onDeployToExisitingWebApp(tableWebApps.getSelectionIndex());
    }

    private void deployToNewWebApp() {
        if (btnResourceGroupCreateNew.getSelection()) {
            try {
                presenter.onDeployToNewWebApp(textAppName.getText(), comboSubscription.getSelectionIndex(),
                        textResourceGroupName.getText(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (btnResourceGroupUseExisting.getSelection()) {
            try {
                presenter.onDeployToNewWebApp(textAppName.getText(), comboSubscription.getSelectionIndex(),
                        comboResourceGroup.getText(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setWidgetsEnabledStatus(boolean enableStatus) {
        tabFolder.setEnabled(enableStatus);
        ((PublishWizardDialog) this.getContainer()).setProgressBarVisible(!enableStatus);
    }

    private void showLoading() {
        tableWebApps.removeAll();
        TableItem placeholderItem = new TableItem(tableWebApps, SWT.NULL);
        placeholderItem.setText("Loading...");
    }
}
