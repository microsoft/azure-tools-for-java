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

package com.microsoft.azuretools.azureexplorer.editors.container;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.container.utils.DockerUtil;
import com.microsoft.azuretools.core.components.AzureListenerWrapper;
import com.microsoft.azuretools.core.mvp.model.container.ContainerRegistryMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.core.mvp.ui.containerregistry.ContainerRegistryProperty;
import com.microsoft.azuretools.core.ui.views.AzureDeploymentProgressNotification;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryPropertyMvpView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryPropertyViewPresenter;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;

public class ContainerRegistryExplorerEditor extends EditorPart implements ContainerRegistryPropertyMvpView {

    private static final String INSIGHT_NAME = "AzurePlugin.Eclipse.Editor.ContainerRegistryExplorerEditor";

    public static final String ID = "com.microsoft.azuretools.azureexplorer.editors.container.ContainerRegistryExplorerEditor";

    private final ContainerRegistryPropertyViewPresenter<ContainerRegistryExplorerEditor> containerExplorerPresenter;

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
    private static final String LABEL_TAG = "Tag";
    private static final String LABEL_REPOSITORY = "Repository";
    private static final String TLTM_NEXT = "Next";
    private static final String TLTM_PREVIOUS = "Previous";
    private static final String TLTM_REFRESH = "Refresh";
    private static final String BTN_DISABLE = "Disable";
    private static final String BTN_ENABLE = "Enable";
    private static final String LOADING = "<Loading...>";
    private static final String COPY_TO_CLIPBOARD = "<a>Copy to Clipboard</a>";
    private static final String PULL_IMAGE = "Pull Image";
    private static final String REPO_TAG_NOT_AVAILABLE = "Cannot get Current repository and tag";

    private static final int PROGRESS_BAR_HEIGHT = 3;
    private static final String REFRESH_ICON_PATH = "icons/refresh_16.png";
    private static final String REFRESH_DISABLE_ICON_PATH = "icons/refresh_16_disable.png";

    private String password = "";
    private String password2 = "";
    private String registryId;
    private String subscriptionId;
    private String currentRepo;
    private String currentTag;

    private ScrolledComposite scrolledComposite;
    private Composite panelHolder;
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
    private Menu popupMenu;

    private boolean isAdminEnabled;

    private SashForm sashForm;
    private Composite cmpoRepo;
    private Composite cmpoTag;
    private ToolBar repoToolBar;
    private ToolItem tltmRepoPreviousPage;
    private ToolItem tltmRepoNextPage;
    private org.eclipse.swt.widgets.List lstRepo;
    private Label label;
    private Label lblTag;
    private org.eclipse.swt.widgets.List lstTag;
    private ToolBar tagToolBar;
    private ToolItem tltmTagPreviousPage;
    private ToolItem tltmTagNextPage;
    private Composite container;
    private ProgressBar progressBar;
    private ToolBar repoRefreshToolBar;
    private ToolItem tltmRefreshRepo;
    private ToolBar tagRefreshToolBar;
    private ToolItem tltmRefreshTag;

    private final Image imgRefreshEnable;
    private final Image imgRefreshDisable;

    public ContainerRegistryExplorerEditor() {
        this.containerExplorerPresenter = new ContainerRegistryPropertyViewPresenter<ContainerRegistryExplorerEditor>();
        this.containerExplorerPresenter.onAttachView(this);
        imgRefreshEnable = Activator.getImageDescriptor(REFRESH_ICON_PATH).createImage();
        imgRefreshDisable = Activator.getImageDescriptor(REFRESH_DISABLE_ICON_PATH).createImage();
    }

    @Override
    public void createPartControl(Composite parent) {

        scrolledComposite = new ScrolledComposite(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);

        panelHolder = new Composite(scrolledComposite, SWT.NONE);
        GridLayout glPanelHolder = new GridLayout(1, false);
        glPanelHolder.marginHeight = 0;
        glPanelHolder.verticalSpacing = 0;
        glPanelHolder.horizontalSpacing = 0;
        panelHolder.setLayout(glPanelHolder);

        progressBar = new ProgressBar(panelHolder, SWT.HORIZONTAL | SWT.INDETERMINATE);
        GridData gdProgressBar = new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1);
        gdProgressBar.heightHint = PROGRESS_BAR_HEIGHT;
        progressBar.setLayoutData(gdProgressBar);

        container = new Composite(panelHolder, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        GridLayout glContainer = new GridLayout(4, false);
        glContainer.marginHeight = 2;
        glContainer.marginWidth = 0;
        glContainer.horizontalSpacing = 30;
        glContainer.verticalSpacing = 10;
        container.setLayout(glContainer);

        Label lblRegistryName = new Label(container, SWT.NONE);
        lblRegistryName.setText(LABEL_NAME);

        txtRegistryName = new Text(container, SWT.READ_ONLY | SWT.BORDER);
        txtRegistryName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        txtRegistryName.setText(LOADING);

        Label lblAdminUserEnabled = new Label(container, SWT.NONE);
        lblAdminUserEnabled.setText(LABEL_ADMIN_USER_ENABLED);

        compAdminUserBtn = new Composite(container, SWT.NONE);
        compAdminUserBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        GridLayout compositeLayout = new GridLayout(2, true);
        compositeLayout.marginWidth = 0;
        compositeLayout.marginHeight = 0;
        compositeLayout.horizontalSpacing = 0;
        compositeLayout.verticalSpacing = 0;
        compAdminUserBtn.setLayout(compositeLayout);

        btnEnable = new Button(compAdminUserBtn, SWT.NONE);
        btnEnable.setEnabled(false);
        btnEnable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnEnable.setText(BTN_ENABLE);

        btnDisable = new Button(compAdminUserBtn, SWT.NONE);
        btnDisable.setEnabled(false);
        btnDisable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnDisable.setText(BTN_DISABLE);

        Label lblType = new Label(container, SWT.NONE);
        lblType.setText(LABEL_TYPE);

        txtType = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        txtType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        txtType.setText(LOADING);

        lblUserName = new Label(container, SWT.NONE);
        lblUserName.setText(LABEL_USER_NAME);
        lblUserName.setVisible(false);

        txtUserName = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        txtUserName.setText(LOADING);
        txtUserName.setVisible(false);

        Label lblResourceGroup = new Label(container, SWT.NONE);
        lblResourceGroup.setText(LABEL_RES_GRP);

        txtResGrp = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        txtResGrp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        txtResGrp.setText(LOADING);

        lblPrimaryPassword = new Label(container, SWT.NONE);
        lblPrimaryPassword.setText(LABEL_PASSWORD);
        lblPrimaryPassword.setVisible(false);

        lnkPrimaryPassword = new Link(container, SWT.NONE);
        lnkPrimaryPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        lnkPrimaryPassword.setText(COPY_TO_CLIPBOARD);
        lnkPrimaryPassword.setVisible(false);

        lnkPrimaryPassword.addListener(SWT.Selection,
                new AzureListenerWrapper(INSIGHT_NAME, "lnkPrimaryPassword", null) {
                    @Override
                    protected void handleEventFunc(Event event) {
                        try {
                            Utils.copyToSystemClipboard(password);
                        } catch (Exception e) {
                            onError(e.getMessage());
                        }
                    }
                });

        Label lblSubscriptionId = new Label(container, SWT.NONE);
        lblSubscriptionId.setText(LABEL_SUBSCRIPTION);

        txtSubscriptionId = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        txtSubscriptionId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        txtSubscriptionId.setText(LOADING);

        lblSecondaryPassword = new Label(container, SWT.NONE);
        lblSecondaryPassword.setText(LABEL_PASSWORD2);
        lblSecondaryPassword.setVisible(false);

        lnkSecondaryPassword = new Link(container, SWT.NONE);
        lnkSecondaryPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        lnkSecondaryPassword.setText(COPY_TO_CLIPBOARD);
        lnkSecondaryPassword.setVisible(false);

        lnkSecondaryPassword.addListener(SWT.Selection,
                new AzureListenerWrapper(INSIGHT_NAME, "lnkSecondaryPassword", null) {
                    @Override
                    protected void handleEventFunc(Event event) {
                        try {
                            Utils.copyToSystemClipboard(password2);
                        } catch (Exception e) {
                            onError(e.getMessage());
                        }
                    }
                });

        Label lblRegion = new Label(container, SWT.NONE);
        lblRegion.setText(LABEL_REGION);

        txtRegion = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        txtRegion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        txtRegion.setText(LOADING);
        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);

        Label lblLoginServerUrl = new Label(container, SWT.NONE);
        lblLoginServerUrl.setText(LABEL_LOGIN_SERVER_URL);

        txtLoginServerUrl = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        txtLoginServerUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        txtLoginServerUrl.setText(LOADING);
        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);

        label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

        sashForm = new SashForm(container, SWT.NONE);
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));

        cmpoRepo = new Composite(sashForm, SWT.BORDER);
        cmpoRepo.setLayout(new GridLayout(2, false));

        Label lblRepo = new Label(cmpoRepo, SWT.NONE);
        lblRepo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
        lblRepo.setText(LABEL_REPOSITORY);

        repoRefreshToolBar = new ToolBar(cmpoRepo, SWT.FLAT | SWT.RIGHT);

        tltmRefreshRepo = new ToolItem(repoRefreshToolBar, SWT.NONE);
        tltmRefreshRepo.setImage(imgRefreshEnable);
        tltmRefreshRepo.setToolTipText(TLTM_REFRESH);
        tltmRefreshRepo.setText(TLTM_REFRESH);
        tltmRefreshRepo.addListener(SWT.Selection, new AzureListenerWrapper(INSIGHT_NAME, "tltmRefreshRepo", null) {
            @Override
            protected void handleEventFunc(Event event) {
                disableWidgets(true, true);
                containerExplorerPresenter.onRefreshRepositories(subscriptionId, registryId, true /* isNextPage */);
            }
        });

        lstRepo = new org.eclipse.swt.widgets.List(cmpoRepo, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        lstRepo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        lstRepo.addListener(SWT.Selection, new AzureListenerWrapper(INSIGHT_NAME, "lstRepo", null) {
            @Override
            protected void handleEventFunc(Event event) {
                int index = lstRepo.getSelectionIndex();
                if (index < 0 || index >= lstRepo.getItemCount()) {
                    return;
                }
                String selectedRepo = lstRepo.getItem(index);
                if (Utils.isEmptyString(selectedRepo) || selectedRepo.equals(currentRepo)) {
                    return;
                }
                currentRepo = selectedRepo;
                disableWidgets(false, true);
                containerExplorerPresenter.onListTags(subscriptionId, registryId, currentRepo, true /* isNextPage */);
            }
        });

        repoToolBar = new ToolBar(cmpoRepo, SWT.FLAT | SWT.RIGHT);
        repoToolBar.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));

        tltmRepoPreviousPage = new ToolItem(repoToolBar, SWT.NONE);
        tltmRepoPreviousPage.setToolTipText(TLTM_PREVIOUS);
        tltmRepoPreviousPage.setText(TLTM_PREVIOUS);;
        tltmRepoPreviousPage.setText("Previous");
        tltmRepoPreviousPage
                .setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_BACK));
        tltmRepoPreviousPage.addListener(SWT.Selection,
                new AzureListenerWrapper(INSIGHT_NAME, "tltmRepoPreviousPage", null) {
                    @Override
                    protected void handleEventFunc(Event event) {
                        disableWidgets(true, true);
                        containerExplorerPresenter.onListRepositories(subscriptionId, registryId,
                                false /* isNextPage */);
                    }
                });

        tltmRepoNextPage = new ToolItem(repoToolBar, SWT.NONE);
        tltmRepoNextPage.setToolTipText(TLTM_NEXT);
        tltmRepoNextPage.setText(TLTM_NEXT);
        tltmRepoNextPage.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_FORWARD));
        tltmRepoNextPage.addListener(SWT.Selection, new AzureListenerWrapper(INSIGHT_NAME, "tltmRepoNextPage", null) {
            @Override
            protected void handleEventFunc(Event event) {
                disableWidgets(true, true);
                containerExplorerPresenter.onListRepositories(subscriptionId, registryId, true /* isNextPage */);
            }
        });
        cmpoTag = new Composite(sashForm, SWT.BORDER);
        cmpoTag.setLayout(new GridLayout(2, false));

        lblTag = new Label(cmpoTag, SWT.NONE);
        lblTag.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
        lblTag.setText(LABEL_TAG);

        tagRefreshToolBar = new ToolBar(cmpoTag, SWT.FLAT | SWT.RIGHT);

        tltmRefreshTag = new ToolItem(tagRefreshToolBar, SWT.NONE);
        tltmRefreshTag.setImage(imgRefreshEnable);
        tltmRefreshTag.setToolTipText(TLTM_REFRESH);
        tltmRefreshTag.setText(TLTM_REFRESH);
        tltmRefreshTag.addListener(SWT.Selection, new AzureListenerWrapper(INSIGHT_NAME, "tltmRefreshTag", null) {
            @Override
            protected void handleEventFunc(Event event) {
                if (Utils.isEmptyString(currentRepo)) {
                    return;
                }
                disableWidgets(false, true);
                containerExplorerPresenter.onListTags(subscriptionId, registryId, currentRepo, true /* isNextPage */);
            }
        });

        lstTag = new org.eclipse.swt.widgets.List(cmpoTag, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        lstTag.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        lstTag.addListener(SWT.Selection, new AzureListenerWrapper(INSIGHT_NAME, "lstTag", null) {
            @Override
            protected void handleEventFunc(Event event) {
                int index = lstTag.getSelectionIndex();
                if (index < 0 || index >= lstTag.getItemCount()) {
                    return;
                }
                String selectedTag = lstTag.getItem(index);
                if (Utils.isEmptyString(selectedTag) || selectedTag.equals(currentTag)) {
                    return;
                }
                currentTag = selectedTag;
            }
        });
        popupMenu = new Menu(lstTag);
        MenuItem pullImage = new MenuItem(popupMenu, SWT.NONE);
        pullImage.addListener(SWT.Selection, new AzureListenerWrapper(INSIGHT_NAME, "menuItem", null) {
            @Override
            protected void handleEventFunc(Event event) {
                pullImage();
            }
        });
        pullImage.setText(PULL_IMAGE);
        lstTag.setMenu(popupMenu);
        lstTag.addListener(SWT.MenuDetect, new Listener() {
            @Override
            public void handleEvent(Event event) {
                int index = lstTag.getSelectionIndex();
                if (index == -1) {
                    event.doit = false;
                }
            }
        });

        tagToolBar = new ToolBar(cmpoTag, SWT.FLAT | SWT.RIGHT);
        tagToolBar.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));

        tltmTagPreviousPage = new ToolItem(tagToolBar, SWT.NONE);
        tltmTagPreviousPage.setToolTipText(TLTM_PREVIOUS);
        tltmTagPreviousPage.setText(TLTM_PREVIOUS);
        tltmTagPreviousPage.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_BACK));
        tltmTagPreviousPage.addListener(SWT.Selection,
                new AzureListenerWrapper(INSIGHT_NAME, "tltmTagPreviousPage", null) {
                    @Override
                    protected void handleEventFunc(Event event) {
                        if (Utils.isEmptyString(currentRepo)) {
                            return;
                        }
                        disableWidgets(false, true);
                        containerExplorerPresenter.onListTags(subscriptionId, registryId, currentRepo,
                                false /* isNextPage */);
                    }
                });

        tltmTagNextPage = new ToolItem(tagToolBar, SWT.NONE);
        tltmTagNextPage.setToolTipText(TLTM_NEXT);
        tltmTagNextPage.setText(TLTM_NEXT);
        tltmTagNextPage.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_FORWARD));
        tltmTagNextPage.addListener(SWT.Selection, new AzureListenerWrapper(INSIGHT_NAME, "tltmTagNextPage", null) {
            @Override
            protected void handleEventFunc(Event event) {
                if (Utils.isEmptyString(currentRepo)) {
                    return;
                }
                disableWidgets(false, true);
                containerExplorerPresenter.onListTags(subscriptionId, registryId, currentRepo, true /* isNextPage */);
            }
        });
        sashForm.setWeights(new int[] { 1, 1 });

        btnEnable.addListener(SWT.Selection, new AzureListenerWrapper(INSIGHT_NAME, "btnEnable", null) {
            @Override
            protected void handleEventFunc(Event event) {
                disableWidgets(true, true);
                onAdminUserBtnClick();
            }
        });

        btnDisable.addListener(SWT.Selection, new AzureListenerWrapper(INSIGHT_NAME, "btnDisable", null) {
            @Override
            protected void handleEventFunc(Event event) {
                disableWidgets(false, false);
                onAdminUserBtnClick();
            }
        });
        disableWidgets(true, true);

        setScrolledCompositeContent();
        setChildrenTransparent(panelHolder);
        setChildrenTransparent(container);

        txtRegistryName.setFocus();
    }

    @Override
    public void onErrorWithException(String message, Exception ex) {
        ContainerRegistryPropertyMvpView.super.onErrorWithException(message, ex);
        this.enableWidgets();
    }

    @Override
    public void onReadProperty(String sid, String id) {
        containerExplorerPresenter.onGetRegistryProperty(sid, id);
    }

    @Override
    public void showProperty(ContainerRegistryProperty property) {
        isAdminEnabled = property.isAdminEnabled();
        txtRegistryName.setText(property.getName());
        txtType.setText(property.getType());
        txtResGrp.setText(property.getGroupName());
        txtSubscriptionId.setText(subscriptionId);
        txtRegion.setText(property.getRegionName());
        txtLoginServerUrl.setText(property.getLoginServerUrl());
        disableWidgets(true, true);
        lblUserName.setVisible(isAdminEnabled);
        txtUserName.setVisible(isAdminEnabled);
        lblPrimaryPassword.setVisible(isAdminEnabled);
        lnkPrimaryPassword.setVisible(isAdminEnabled);
        lblSecondaryPassword.setVisible(isAdminEnabled);
        lnkSecondaryPassword.setVisible(isAdminEnabled);
        updateAdminUserBtn(isAdminEnabled);
        if (isAdminEnabled) {
            txtUserName.setText(property.getUserName());
            password = property.getPassword();
            password2 = property.getPassword2();
            containerExplorerPresenter.onRefreshRepositories(subscriptionId, registryId, true /* isNextPage */);
            sashForm.setVisible(true);
        } else {
            sashForm.setVisible(false);
            progressBar.setVisible(false);
        }

        panelHolder.layout();
        setScrolledCompositeContent();
        this.setPartName(property.getName());
    }

    @Override
    public void listRepo(List<String> repos) {
        lstRepo.removeAll();
        fillList(repos, lstRepo);
        enableWidgets();
    }

    @Override
    public void listTag(List<String> tags) {
        lstTag.removeAll();
        fillList(tags, lstTag);
        enableWidgets();
        lstRepo.setFocus();
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
            this.subscriptionId = containerInput.getSubscriptionId();
            this.registryId = containerInput.getId();
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
        this.containerExplorerPresenter.onEnableAdminUser(subscriptionId, registryId, !isAdminEnabled);
    }

    private void updateAdminUserBtn(boolean isAdminEnabled) {
        btnEnable.setEnabled(!isAdminEnabled);
        btnDisable.setEnabled(isAdminEnabled);
    }

    private void setScrolledCompositeContent() {
        scrolledComposite.setContent(panelHolder);
        scrolledComposite.setMinSize(panelHolder.computeSize(SWT.DEFAULT, SWT.DEFAULT));
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

    private void enableWidgets() {
        updateAdminUserBtn(isAdminEnabled);
        lstRepo.setEnabled(true);
        tltmRefreshRepo.setEnabled(true);
        tltmRefreshRepo.setImage(imgRefreshEnable);
        progressBar.setVisible(false);
        if (containerExplorerPresenter.hasNextRepoPage()) {
            tltmRepoNextPage.setEnabled(true);
        }
        if (containerExplorerPresenter.hasPreviousRepoPage()) {
            tltmRepoPreviousPage.setEnabled(true);
        }
        lstTag.setEnabled(true);
        if (currentRepo == null) {
            return;
        }
        tltmRefreshTag.setEnabled(true);
        tltmRefreshTag.setImage(imgRefreshEnable);
        if (containerExplorerPresenter.hasNextTagPage()) {
            tltmTagNextPage.setEnabled(true);
        }
        if (containerExplorerPresenter.hasPreviousTagPage()) {
            tltmTagPreviousPage.setEnabled(true);
        }
    }

    private void disableWidgets(boolean needResetRepo, boolean needResetTag) {
        btnEnable.setEnabled(false);
        btnDisable.setEnabled(false);
        if (needResetRepo) {
            currentRepo = null;
            lstRepo.removeAll();
        }
        if (needResetTag) {
            lstTag.removeAll();
        }
        lstRepo.setEnabled(false);
        tltmRefreshRepo.setEnabled(false);
        tltmRefreshRepo.setImage(imgRefreshDisable);
        tltmRepoPreviousPage.setEnabled(false);
        tltmRepoNextPage.setEnabled(false);
        lstTag.setEnabled(false);
        tltmRefreshTag.setEnabled(false);
        tltmRefreshTag.setImage(imgRefreshDisable);
        tltmTagPreviousPage.setEnabled(false);
        tltmTagNextPage.setEnabled(false);
        progressBar.setVisible(true);
    }

    private void fillList(List<String> list, @NotNull org.eclipse.swt.widgets.List widget) {
        if (list != null && list.size() > 0) {
            Collections.sort(list);
            for (String item : list) {
                widget.add(item);
            }
        }
    }

    private void pullImage() {
        Job job = new Job(PULL_IMAGE) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask(PULL_IMAGE, IProgressMonitor.UNKNOWN);
                String deploymentName = UUID.randomUUID().toString();
                try {
                    if (Utils.isEmptyString(currentRepo) || Utils.isEmptyString(currentTag)) {
                        throw new Exception(REPO_TAG_NOT_AVAILABLE);
                    }
                    final String image = String.format("%s:%s", currentRepo, currentTag);
                    String jobDescription = String.format("Pulling: %s", image);
                    AzureDeploymentProgressNotification.createAzureDeploymentProgressNotification(deploymentName,
                            jobDescription);
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, "", 5,
                            "Getting Registry...");
                    final Registry registry = ContainerRegistryMvpModel.getInstance()
                            .getContainerRegistry(subscriptionId, registryId);
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, "", 5,
                            "Getting Credential...");
                    final PrivateRegistryImageSetting setting = ContainerRegistryMvpModel.getInstance()
                            .createImageSettingWithRegistry(registry);
                    final String fullImageTagName = String.format("%s/%s", registry.loginServerUrl(), image);
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, "", 10,
                            "Pulling image...");
                    DockerClient docker = DefaultDockerClient.fromEnv().build();
                    DockerUtil.pullImage(docker, registry.loginServerUrl(), setting.getUsername(),
                            setting.getPassword(), fullImageTagName);
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, null, 100, "Finish.");
                    sendTelemetry(true, subscriptionId, null);
                } catch (Exception ex) {
                    monitor.done();
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, null, -1, ex.getMessage());
                    sendTelemetry(false, subscriptionId, ex.getMessage());
                    return Status.CANCEL_STATUS;
                }
                monitor.done();
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private void sendTelemetry(boolean success, @NotNull String subscriptionId, @Nullable String errorMsg) {
        Map<String, String> map = new HashMap<>();
        map.put("SubscriptionId", subscriptionId);
        map.put("Success", String.valueOf(success));
        if (!success) {
            map.put("ErrorMsg", errorMsg);
        }
        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, "ACR", PULL_IMAGE, map);
    }
}
