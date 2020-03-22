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
package com.microsoft.azuretools.azureexplorer.views;

import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisCacheProperty;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisPropertyMvpView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisPropertyViewPresenter;

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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class RedisPropertyView extends ViewPart implements RedisPropertyMvpView {

    public static final String ID = "com.microsoft.azuretools.azureexplorer.views.RedisPropertyView";

    private static final int VERTICAL_SPACING = 10;
    private static final int HORIZONTAL_SPACING = 30;
    private static final int COLUMN_NUM = 2;


    private static final String LABEL_NAME = "Name";
    private static final String LABEL_TYPE = "Type";
    private static final String LABEL_RES_GRP = "Resource Group";
    private static final String LABEL_SUBSCRIPTION = "Subscription Id";
    private static final String LABEL_REGION = "Region";
    private static final String LABEL_HOST_NAME = "Host Name";
    private static final String LABEL_SSL = "SSL Port";
    private static final String LABEL_NONSSL = "Non-SSL Port(6379) Enabled";
    private static final String LABEL_VERSION = "Redis Cache Version";
    private static final String LABEL_PRIMARY_KEY = "Primary Key";
    private static final String LABEL_SECONDARY_KEY = "Secondary Key";
    private static final String LOADING = "<Loading...>";
    private static final String COPY_TO_CLIPBOARD = "<a>Copy to Clipboard</a>";
    private static final String COPY_FAIL = "Cannot copy to system clipboard.";

    private final RedisPropertyViewPresenter<RedisPropertyView> redisPropertyViewPresenter;

    //widget
    private ScrolledComposite scrolledComposite;
    private Composite container;
    private Text txtNameValue;
    private Text txtTypeValue;
    private Text txtResGrpValue;
    private Text txtSubscriptionValue;
    private Text txtRegionValue;
    private Text txtHostNameValue;
    private Text txtSslPortValue;
    private Text txtNonSslPortValue;
    private Text txtVersionValue;
    private Link lnkPrimaryKey;
    private Link lnkSecondaryKey;

    //data
    private String primaryKey = "";
    private String secondaryKey = "";

    public RedisPropertyView() {
        this.redisPropertyViewPresenter = new RedisPropertyViewPresenter<RedisPropertyView>();
        this.redisPropertyViewPresenter.onAttachView(this);
    }

    /**
     * Create contents of the view part.
     */
    @Override
    public void createPartControl(Composite parent) {
        scrolledComposite = new ScrolledComposite(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);

        container = new Composite(scrolledComposite, SWT.NONE);
        GridLayout gridLayoutContainer = new GridLayout(COLUMN_NUM, false);
        gridLayoutContainer.verticalSpacing = VERTICAL_SPACING;
        gridLayoutContainer.horizontalSpacing = HORIZONTAL_SPACING;
        container.setLayout(gridLayoutContainer);

        Label lblName = new Label(container, SWT.NONE);
        lblName.setText(LABEL_NAME);

        txtNameValue = new Text(container, SWT.READ_ONLY);
        txtNameValue.setText(LOADING);

        Label lblType = new Label(container, SWT.NONE);
        lblType.setText(LABEL_TYPE);

        txtTypeValue = new Text(container, SWT.READ_ONLY);
        txtTypeValue.setText(LOADING);

        Label lblResGrp = new Label(container, SWT.NONE);
        lblResGrp.setText(LABEL_RES_GRP);

        txtResGrpValue = new Text(container, SWT.READ_ONLY);
        txtResGrpValue.setText(LOADING);

        Label lblSubscription = new Label(container, SWT.NONE);
        lblSubscription.setText(LABEL_SUBSCRIPTION);

        txtSubscriptionValue = new Text(container, SWT.READ_ONLY);
        txtSubscriptionValue.setText(LOADING);

        Label lblRegion = new Label(container, SWT.NONE);
        lblRegion.setText(LABEL_REGION);

        txtRegionValue = new Text(container, SWT.READ_ONLY);
        txtRegionValue.setText(LOADING);

        Label lblHostName = new Label(container, SWT.NONE);
        lblHostName.setText(LABEL_HOST_NAME);

        txtHostNameValue = new Text(container, SWT.READ_ONLY);
        txtHostNameValue.setText(LOADING);

        Label lblSslPort = new Label(container, SWT.NONE);
        lblSslPort.setText(LABEL_SSL);

        txtSslPortValue = new Text(container, SWT.READ_ONLY);
        txtSslPortValue.setText(LOADING);

        Label lblNonSslPort = new Label(container, SWT.NONE);
        lblNonSslPort.setText(LABEL_NONSSL);

        txtNonSslPortValue = new Text(container, SWT.READ_ONLY);
        txtNonSslPortValue.setText(LOADING);

        Label lblVersion = new Label(container, SWT.NONE);
        lblVersion.setText(LABEL_VERSION);

        txtVersionValue = new Text(container, SWT.READ_ONLY);
        txtVersionValue.setText(LOADING);

        Label lblPrimaryKey = new Label(container, SWT.NONE);
        lblPrimaryKey.setText(LABEL_PRIMARY_KEY);

        lnkPrimaryKey = new Link(container, SWT.NONE);
        lnkPrimaryKey.setEnabled(false);
        lnkPrimaryKey.setText(COPY_TO_CLIPBOARD);

        Label lblSecondaryKey = new Label(container, SWT.NONE);
        lblSecondaryKey.setText(LABEL_SECONDARY_KEY);

        lnkSecondaryKey = new Link(container, SWT.NONE);
        lnkSecondaryKey.setEnabled(false);
        lnkSecondaryKey.setText(COPY_TO_CLIPBOARD);

        setChildrenTransparent(container);
        setScrolledCompositeContent();

        lnkPrimaryKey.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                copyToSystemClipboard(primaryKey);
            }
        });

        lnkSecondaryKey.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                copyToSystemClipboard(secondaryKey);
            }
        });
    }

    @Override
    public void setFocus() { }

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
        IWorkbench workbench = PlatformUI.getWorkbench();
        final IWorkbenchPage activePage = workbench.getActiveWorkbenchWindow().getActivePage();
        workbench.addWorkbenchListener(new IWorkbenchListener() {
            @Override
            public boolean preShutdown(IWorkbench workbench, boolean forced) {
                activePage.hideView(RedisPropertyView.this);
                return true;
            }

            @Override
            public void postShutdown(IWorkbench workbench) { }
        });
    }

    @Override
    public void dispose() {
        this.redisPropertyViewPresenter.onDetachView();
        super.dispose();
    }

    @Override
    public void onReadProperty(String sid, String id) {
        redisPropertyViewPresenter.onGetRedisProperty(sid, id);
    }

    @Override
    public void showProperty(RedisCacheProperty property) {
        primaryKey = property.getPrimaryKey();
        secondaryKey = property.getSecondaryKey();

        txtNameValue.setText(property.getName());
        txtTypeValue.setText(property.getType());
        txtResGrpValue.setText(property.getGroupName());
        txtSubscriptionValue.setText(property.getSubscriptionId());
        txtRegionValue.setText(property.getRegionName());
        txtHostNameValue.setText(property.getHostName());
        txtSslPortValue.setText(String.valueOf(property.getSslPort()));
        txtNonSslPortValue.setText(String .valueOf(property.isNonSslPort()));
        txtVersionValue.setText(property.getVersion());
        lnkPrimaryKey.setEnabled(true);
        lnkSecondaryKey.setEnabled(true);
        container.layout();
        setScrolledCompositeContent();

        this.setPartName(property.getName());
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

    private void setScrolledCompositeContent() {
        scrolledComposite.setContent(container);
        scrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    private void copyToSystemClipboard(String key) {
        StringSelection stringSelection = new StringSelection(key);
        Toolkit toolKit = Toolkit.getDefaultToolkit();
        if (toolKit == null) {
            onError(COPY_FAIL);
            return;
        }
        Clipboard clipboard = toolKit.getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }
}