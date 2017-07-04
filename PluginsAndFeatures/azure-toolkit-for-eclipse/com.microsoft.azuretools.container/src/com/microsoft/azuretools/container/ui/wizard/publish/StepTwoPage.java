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

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.presenters.StepTwoPagePresenter;

import com.microsoft.azuretools.container.views.PublishWizardPageView;
import com.microsoft.azuretools.container.views.StepTwoPageView;

import com.microsoft.azuretools.core.components.AzureWizardPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.FillLayout;

import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroup;

import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabItem;

public class StepTwoPage extends AzureWizardPage implements StepTwoPageView, PublishWizardPageView {
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

    public void fillTable(List<SiteInner> wal) {
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

        setTitle("Deploy to Azure Web App on Linux");
        setDescription("Select existing or create new Web App to deploy");
    }

    /**
     * Create contents of the wizard.
     * 
     * @param parent
     */
    public void createControl(Composite parent) {
        Font boldFont = new Font(Display.getCurrent(), new FontData("Segoe UI", 9, SWT.BOLD));
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        GridLayout gl_container = new GridLayout(1, false);
        gl_container.marginWidth = 10;
        gl_container.marginTop = 10;
        gl_container.marginRight = 20;
        gl_container.marginBottom = 10;
        gl_container.marginLeft = 20;
        container.setLayout(gl_container);

        tabFolder = new TabFolder(container, SWT.NONE);
        tabFolder.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1));
        formToolkit.adapt(tabFolder);
        formToolkit.paintBordersFor(tabFolder);
        tabFolder.addListener(SWT.Selection, event -> onTabFolderSelection());

        tbtmUpdate = new TabItem(tabFolder, SWT.NONE);
        tbtmUpdate.setText(TEXT_TAB_UPDATE);

        Composite composite_1 = new Composite(tabFolder, SWT.NONE);
        tbtmUpdate.setControl(composite_1);
        formToolkit.paintBordersFor(composite_1);
        composite_1.setLayout(new GridLayout(1, false));

        Composite cmpoWebAppOnLinux = formToolkit.createComposite(composite_1, SWT.NONE);
        GridData gd_cmpoWebAppOnLinux = new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1);
        gd_cmpoWebAppOnLinux.widthHint = 512;
        gd_cmpoWebAppOnLinux.heightHint = 255;
        cmpoWebAppOnLinux.setLayoutData(gd_cmpoWebAppOnLinux);
        formToolkit.paintBordersFor(cmpoWebAppOnLinux);
        cmpoWebAppOnLinux.setLayout(new GridLayout(2, false));

        Composite cmpoWebAppsTable = formToolkit.createComposite(cmpoWebAppOnLinux, SWT.NONE);
        GridData gd_cmpoWebAppsTable = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_cmpoWebAppsTable.widthHint = 400;
        cmpoWebAppsTable.setLayoutData(gd_cmpoWebAppsTable);
        cmpoWebAppsTable.setLayout(new FillLayout(SWT.HORIZONTAL));
        formToolkit.paintBordersFor(cmpoWebAppsTable);

        tableWebApps = new Table(cmpoWebAppsTable, SWT.BORDER | SWT.FULL_SELECTION);
        tableWebApps.setHeaderVisible(true);
        tableWebApps.setLinesVisible(true);
        formToolkit.adapt(tableWebApps);
        formToolkit.paintBordersFor(tableWebApps);
        tableWebApps.addListener(SWT.Selection, ecent -> onTableWebAppsSelection());

        TableColumn tblclmnName = new TableColumn(tableWebApps, SWT.LEFT);
        tblclmnName.setWidth(180);
        tblclmnName.setText("Name");

        TableColumn tblclmnResourceGroup = new TableColumn(tableWebApps, SWT.LEFT);
        tblclmnResourceGroup.setWidth(220);
        tblclmnResourceGroup.setText("Resource group");

        Composite cmpoActionButtons = formToolkit.createComposite(cmpoWebAppOnLinux, SWT.NONE);
        cmpoActionButtons.setLayout(new GridLayout(1, false));
        cmpoActionButtons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        formToolkit.paintBordersFor(cmpoActionButtons);

        btnRefresh = new Button(cmpoActionButtons, SWT.NONE);
        GridData gd_btnRefresh = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_btnRefresh.widthHint = 75;
        btnRefresh.setLayoutData(gd_btnRefresh);
        formToolkit.adapt(btnRefresh, true, true);
        btnRefresh.setText(TEXT_BUTTON_REFRESH);
        btnRefresh.addListener(SWT.Selection, event -> onBtnRefreshSelection());

        Composite cmpoInformation = formToolkit.createComposite(composite_1, SWT.NONE);
        cmpoInformation.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1));
        formToolkit.paintBordersFor(cmpoInformation);

        tbtmCreate = new TabItem(tabFolder, SWT.NONE);
        tbtmCreate.setText(TEXT_TAB_CREATE);

        Composite composite = new Composite(tabFolder, SWT.NONE);
        tbtmCreate.setControl(composite);
        formToolkit.paintBordersFor(composite);

        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

        Group grpAppService = new Group(composite, SWT.NONE);
        GridData gd_grpAppService = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_grpAppService.heightHint = 75;
        grpAppService.setLayoutData(gd_grpAppService);
        GridLayout gl_grpAppService = new GridLayout(3, false);
        gl_grpAppService.verticalSpacing = 15;
        gl_grpAppService.marginWidth = 10;
        grpAppService.setLayout(gl_grpAppService);

        Label lblAppName = new Label(grpAppService, SWT.NONE);
        lblAppName.setFont(boldFont);
        GridData gd_lblAppName = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAppName.widthHint = 75;
        lblAppName.setLayoutData(gd_lblAppName);
        lblAppName.setText("Enter name");

        textAppName = new Text(grpAppService, SWT.BORDER);
        textAppName.addListener(SWT.Modify, event -> onTextAppNameModify());
        textAppName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textAppName.setMessage("<enter name>");

        Label lblazurewebsitescom = new Label(grpAppService, SWT.NONE);
        lblazurewebsitescom.setText(".azurewebsites.net");

        Label lblSubscription = new Label(grpAppService, SWT.NONE);
        lblSubscription.setFont(boldFont);
        lblSubscription.setText("Subscription");

        comboSubscription = new Combo(grpAppService, SWT.READ_ONLY);
        comboSubscription.addListener(SWT.Selection, event -> onComboSubscriptionSelection());
        comboSubscription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String date = df.format(new Date());
        textAppName.setText("linuxwebapp-" + date);

        Label lblResourceGroup = new Label(grpAppService, SWT.NONE);
        lblResourceGroup.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        lblResourceGroup.setFont(boldFont);
        formToolkit.adapt(lblResourceGroup, true, true);
        lblResourceGroup.setText("Resource Group:");

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

        Point size = getShell().computeSize(600, 450);
        getShell().setSize(size);

        initialize();
    }

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

    @Override
    public void dispose() {
        presenter.onDetachView();
        super.dispose();
    }

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

    @Override
    public void onRequestPending() {
        setWidgetsEnabledStatus(false);
        ((PublishWizardDialog) this.getContainer()).setButtonsEnabled(false);
    }

    @Override
    public void onRequestSucceed() {
        setWidgetsEnabledStatus(true);
        ((PublishWizardDialog) this.getContainer()).updateButtons();
    }

    @Override
    public void onRequestFail(String errorMsg) {
        if(errorMsg != null) {
            ConsoleLogger.error(errorMsg);
        }
        setWidgetsEnabledStatus(true);
        ((PublishWizardDialog) this.getContainer()).updateButtons();
        ((PublishWizardDialog) this.getContainer()).doCancelPressed();
    }

    public void setWidgetsEnabledStatus(boolean enableStatus) {
        btnRefresh.setEnabled(enableStatus);
        tabFolder.setEnabled(enableStatus);
    }

    @Override
    public void fillSubscriptions(List<SubscriptionDetail> sdl) {
        if (sdl == null || sdl.size() <= 0) {
            System.out.println("sdl is null");
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
            System.out.println("rgl is null");
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
        ConsoleLogger.info("Web App on Linux Created");
        ((PublishWizardDialog) this.getContainer()).doFinishPressed();
    }

    @Override
    public void fillWebApps(List<SiteInner> wal) {
        setDescription("List of Web App on Linux");
        fillTable(wal);
    }

    public void showLoading() {
        tableWebApps.removeAll();
        TableItem placeholderItem = new TableItem(tableWebApps, SWT.NULL);
        placeholderItem.setText("Loading...");
    }
}
