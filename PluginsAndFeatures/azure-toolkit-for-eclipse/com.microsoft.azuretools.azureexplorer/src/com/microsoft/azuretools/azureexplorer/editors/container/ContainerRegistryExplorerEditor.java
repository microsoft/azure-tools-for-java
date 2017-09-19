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

package com.microsoft.azuretools.azureexplorer.editors.container;

import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.ui.containerregistry.ContainerRegistryProperty;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryPropertyMvpView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryPropertyViewPresenter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import java.util.List;

public class ContainerRegistryExplorerEditor extends EditorPart implements ContainerRegistryPropertyMvpView {

    public static final String ID = "com.microsoft.azuretools.azureexplorer.editors.container.ContainerRegistryExplorerEditor";

    private final ContainerRegistryPropertyViewPresenter<ContainerRegistryExplorerEditor> containerExplorerPresenter;

    private static final int VERTICAL_SPACING = 10;
    private static final int HORIZONTAL_SPACING = 30;
    private static final int COLUMN_NUM = 2;

    private static final String LABEL_NAME = "Registry Name";
    private static final String LABEL_TYPE = "Type";
    private static final String LABEL_RES_GRP = "Resource Group";
    private static final String LABEL_SUBSCRIPTION = "Subscription Id";
    private static final String LABEL_REGION = "Region";
    private static final String LABEL_LOGIN_SERVER_URL = "Login Server URL";
    private static final String LABEL_ADMIN_USER_ENABLED = "Admin User Enabled";
    private static final String LABEL_USER_NAME = "User Name";
    private static final String LABEL_PASSWORD = "Password";
    private static final String LABEL_PASSWORD2 = "Password2";
    private static final String LOADING = "<Loading...>";
    private static final String COPY_TO_CLIPBOARD = "<a>Copy to Clipboard</a>";

    private String password = "";
    private String password2 = "";
    private String registryId;

    private ScrolledComposite scrolledComposite;
    private Composite container;
    private Text txtRegistryName;
    private Text txtType;
    private Text txtResGrp;
    private Text txtSubscriptionId;
    private Text txtRegion;
    private Text txtLoginServerUrl;
    private Text txtUserName;
    private Label lblUserName;
    private Label lblPrimaryPassword;
    private Label lblSecondaryPassword;
    private Link lnkPrimaryPassword;
    private Link lnkSecondaryPassword;
    private Composite compAdminUserBtn;
    private Button btnEnable;
    private Button btnDisable;

    private boolean isAdminEnabled;

    private String subscriptionId;

    public ContainerRegistryExplorerEditor() {
        this.containerExplorerPresenter = new ContainerRegistryPropertyViewPresenter<ContainerRegistryExplorerEditor>();
        this.containerExplorerPresenter.onAttachView(this);
    }

    @Override
    public void createPartControl(Composite parent) {

        scrolledComposite = new ScrolledComposite(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);

        container = new Composite(scrolledComposite, SWT.NONE);
        GridLayout grdiLayout = new GridLayout(COLUMN_NUM, false);
        grdiLayout.verticalSpacing = VERTICAL_SPACING;
        grdiLayout.horizontalSpacing = HORIZONTAL_SPACING;
        container.setLayout(grdiLayout);

        Label lblRegistryName = new Label(container, SWT.NONE);
        lblRegistryName.setText(LABEL_NAME);

        txtRegistryName = new Text(container, SWT.READ_ONLY);
        txtRegistryName.setText(LOADING);

        Label lblType = new Label(container, SWT.NONE);
        lblType.setText(LABEL_TYPE);

        txtType = new Text(container, SWT.READ_ONLY);
        txtType.setText(LOADING);

        Label lblResourceGroup = new Label(container, SWT.NONE);
        lblResourceGroup.setText(LABEL_RES_GRP);

        txtResGrp = new Text(container, SWT.READ_ONLY);
        txtResGrp.setText(LOADING);

        Label lblSubscriptionId = new Label(container, SWT.NONE);
        lblSubscriptionId.setText(LABEL_SUBSCRIPTION);

        txtSubscriptionId = new Text(container, SWT.READ_ONLY);
        txtSubscriptionId.setText(LOADING);

        Label lblRegion = new Label(container, SWT.NONE);
        lblRegion.setText(LABEL_REGION);

        txtRegion = new Text(container, SWT.READ_ONLY);
        txtRegion.setText(LOADING);

        Label lblLoginServerUrl = new Label(container, SWT.NONE);
        lblLoginServerUrl.setText(LABEL_LOGIN_SERVER_URL);

        txtLoginServerUrl = new Text(container, SWT.READ_ONLY);
        txtLoginServerUrl.setText(LOADING);

        Label lblAdminUserEnabled = new Label(container, SWT.NONE);
        lblAdminUserEnabled.setText(LABEL_ADMIN_USER_ENABLED);

        compAdminUserBtn = new Composite(container, SWT.NONE);
        compAdminUserBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        GridLayout compositeLayout = new GridLayout(2, true);
        compositeLayout.marginWidth = 0;
        compositeLayout.marginHeight = 0;
        compositeLayout.horizontalSpacing = 0;
        compositeLayout.verticalSpacing = 0;
        compAdminUserBtn.setLayout(compositeLayout);

        btnEnable = new Button(compAdminUserBtn, SWT.NONE);
        btnEnable.setEnabled(false);
        btnEnable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnEnable.setText("Enable");

        btnDisable = new Button(compAdminUserBtn, SWT.NONE);
        btnDisable.setEnabled(false);
        btnDisable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnDisable.setText("Disable");

        lblUserName = new Label(container, SWT.NONE);
        lblUserName.setText(LABEL_USER_NAME);
        lblUserName.setVisible(false);

        txtUserName = new Text(container, SWT.READ_ONLY);
        txtUserName.setText(LOADING);
        txtUserName.setVisible(false);

        lblPrimaryPassword = new Label(container, SWT.NONE);
        lblPrimaryPassword.setText(LABEL_PASSWORD);
        lblPrimaryPassword.setVisible(false);

        lnkPrimaryPassword = new Link(container, SWT.NONE);
        lnkPrimaryPassword.setText(COPY_TO_CLIPBOARD);
        lnkPrimaryPassword.setVisible(false);

        lblSecondaryPassword = new Label(container, SWT.NONE);
        lblSecondaryPassword.setText(LABEL_PASSWORD2);
        lblSecondaryPassword.setVisible(false);

        lnkSecondaryPassword = new Link(container, SWT.NONE);
        lnkSecondaryPassword.setText(COPY_TO_CLIPBOARD);
        lnkSecondaryPassword.setVisible(false);

        setScrolledCompositeContent();
        setChildrenTransparent(container);

        lnkPrimaryPassword.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    Utils.copyToSystemClipboard(password);
                } catch (Exception e) {
                    onError(e.getMessage());
                }
            }
        });

        lnkSecondaryPassword.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    Utils.copyToSystemClipboard(password2);
                } catch (Exception e) {
                    onError(e.getMessage());
                }
            }
        });

        btnEnable.addListener(SWT.Selection, event -> onAdminUserBtnClick());
        btnDisable.addListener(SWT.Selection, event -> onAdminUserBtnClick());
    }

    @Override
    public void onReadProperty(String sid, String id) {
        containerExplorerPresenter.onGetRegistryProperty(sid, id);
    }

    @Override
    public void showProperty(ContainerRegistryProperty property) {
        registryId = property.getId();
        isAdminEnabled = property.isAdminEnabled();
        subscriptionId = property.getSubscriptionId();

        txtRegistryName.setText(property.getName());
        txtType.setText(property.getType());
        txtResGrp.setText(property.getGroupName());
        txtSubscriptionId.setText(subscriptionId);
        txtRegion.setText(property.getRegionName());
        txtLoginServerUrl.setText(property.getLoginServerUrl());

        if (isAdminEnabled) {
            txtUserName.setText(property.getUserName());
            password = property.getPassword();
            password2 = property.getPassword2();
        }
        compAdminUserBtn.setEnabled(true);
        updateAdminUserBtn(isAdminEnabled);
        lblUserName.setVisible(isAdminEnabled);
        txtUserName.setVisible(isAdminEnabled);
        lblPrimaryPassword.setVisible(isAdminEnabled);
        lnkPrimaryPassword.setVisible(isAdminEnabled);
        lblSecondaryPassword.setVisible(isAdminEnabled);
        lnkSecondaryPassword.setVisible(isAdminEnabled);

        container.layout();
        setScrolledCompositeContent();
        this.setPartName(property.getName());
    }

    @Override
    public void listRepo(List<String> repos) {
        // TODO Auto-generated method stub

    }

    @Override
    public void listTag(List<String> tags) {
        // TODO Auto-generated method stub

    }

    @Override
    public void dispose() {
        this.containerExplorerPresenter.onDetachView();
        super.dispose();
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        if (input instanceof ContainerRegistryExplorerEditorInput) {
            ContainerRegistryExplorerEditorInput containerInput = (ContainerRegistryExplorerEditorInput) input;
            this.setPartName(containerInput.getName());
            containerExplorerPresenter.onGetRegistryProperty(containerInput.getSubscriptionId(),
                    containerInput.getId());
        }

        IWorkbench workbench = PlatformUI.getWorkbench();
        final IWorkbenchPage activePage = workbench.getActiveWorkbenchWindow().getActivePage();
        workbench.addWorkbenchListener(new IWorkbenchListener() {
            @Override
            public boolean preShutdown(IWorkbench workbench, boolean forced) {
                activePage.closeEditor(ContainerRegistryExplorerEditor.this, true);
                return true;
            }

            @Override
            public void postShutdown(IWorkbench workbench) {
            }
        });
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void setFocus() {
    }

    @Override
    public void doSave(IProgressMonitor arg0) {
    }

    @Override
    public void doSaveAs() {
    }

    private void onAdminUserBtnClick() {
        compAdminUserBtn.setEnabled(false);
        btnEnable.setEnabled(false);
        btnDisable.setEnabled(false);
        this.containerExplorerPresenter.onEnableAdminUser(subscriptionId, registryId, !isAdminEnabled);
    }

    private void updateAdminUserBtn(boolean isAdminEnabled) {
        btnEnable.setEnabled(!isAdminEnabled);
        btnDisable.setEnabled(isAdminEnabled);
    }

    private void setScrolledCompositeContent() {
        scrolledComposite.setContent(container);
        scrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    private void setChildrenTransparent(Composite container) {
        if (container == null) {
            return;
        }
        Color transparentColor = container.getBackground();
        for (Control control : container.getChildren()) {
            if (control instanceof Text) {
                control.setBackground(transparentColor);
            }
        }
    }

}
