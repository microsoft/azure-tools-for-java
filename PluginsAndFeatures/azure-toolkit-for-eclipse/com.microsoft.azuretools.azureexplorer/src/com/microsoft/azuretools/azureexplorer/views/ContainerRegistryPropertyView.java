/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azuretools.azureexplorer.views;

import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.ui.containerregistry.ContainerRegistryProperty;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryPropertyMvpView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryPropertyViewPresenter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

public class ContainerRegistryPropertyView extends ViewPart implements ContainerRegistryPropertyMvpView {

    public static final String ID = "com.microsoft.azuretools.azureexplorer.views.ContainerRegistryPropertyView";

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

    private final ContainerRegistryPropertyViewPresenter<ContainerRegistryPropertyView> containerPropertyPresenter;

    private String password = "";
    private String password2 = "";

    private ScrolledComposite scrolledComposite;
    private Composite container;
    private Text txtRegistryName;
    private Text txtType;
    private Text txtResGrp;
    private Text txtSubscriptionId;
    private Text txtRegion;
    private Text txtLoginServerUrl;
    private Text txtAdminUserEnabled;
    private Text txtUserName;
    private Label lblUserName;
    private Label lblPrimaryPassword;
    private Label lblSecondaryPassword;
    private Link lnkPrimaryPassword;
    private Link lnkSecondaryPassword;

    public ContainerRegistryPropertyView() {
        this.containerPropertyPresenter = new ContainerRegistryPropertyViewPresenter<>();
        this.containerPropertyPresenter.onAttachView(this);
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

        txtAdminUserEnabled = new Text(container, SWT.READ_ONLY);
        txtAdminUserEnabled.setText(LOADING);

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
    }

    @Override
    public void setFocus() {}

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
        IWorkbench workbench = PlatformUI.getWorkbench();
        final IWorkbenchPage activePage = workbench.getActiveWorkbenchWindow().getActivePage();
        workbench.addWorkbenchListener(new IWorkbenchListener() {
            @Override
            public boolean preShutdown(IWorkbench workbench, boolean forced) {
                activePage.hideView(ContainerRegistryPropertyView.this);
                return true;
            }

            @Override
            public void postShutdown(IWorkbench workbench) { }
        });
    }

    @Override
    public void dispose() {
        this.containerPropertyPresenter.onDetachView();
        super.dispose();
    }

    @Override
    public void onReadProperty(String sid, String id) {
        containerPropertyPresenter.onGetRegistryProperty(sid, id);
    }

    @Override
    public void showProperty(ContainerRegistryProperty property) {
        txtRegistryName.setText(property.getName());
        txtType.setText(property.getType());
        txtResGrp.setText(property.getGroupName());
        txtSubscriptionId.setText(property.getSubscriptionId());
        txtRegion.setText(property.getRegionName());
        txtLoginServerUrl.setText(property.getLoginServerUrl());
        boolean isAdminEnabled = property.isAdminEnabled();
        txtAdminUserEnabled.setText(String.valueOf(isAdminEnabled));
        if (isAdminEnabled) {
            lblUserName.setVisible(true);
            txtUserName.setVisible(true);
            txtUserName.setText(property.getUserName());
            lblPrimaryPassword.setVisible(true);
            lnkPrimaryPassword.setVisible(true);
            lblSecondaryPassword.setVisible(true);
            lnkSecondaryPassword.setVisible(true);
            password = property.getPassword();
            password2 = property.getPassword2();
        }
        container.layout();
        setScrolledCompositeContent();
        this.setPartName(property.getName());
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
        for (Control control: container.getChildren()) {
            if (control instanceof Text) {
                control.setBackground(transparentColor);
            }
        }
    }
}
