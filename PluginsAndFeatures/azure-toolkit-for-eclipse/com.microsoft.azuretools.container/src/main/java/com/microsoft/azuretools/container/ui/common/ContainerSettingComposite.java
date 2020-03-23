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

package com.microsoft.azuretools.container.ui.common;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerSettingPresenter;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerSettingView;

public final class ContainerSettingComposite extends Composite implements ContainerSettingView {

    private final ContainerSettingPresenter<ContainerSettingComposite> presenter;
    private final String projectBasePath;
    private List<Registry> registryCache;

    private static final String LABEL_STARTUP_FILE = "Startup File";
    private static final String LABEL_TAG_PREFIX = "<Server URL>/";
    private static final String LABEL_IMAGE_AND_TAG = "Image and Tag";
    private static final String LABEL_PASSWORD = "Password";
    private static final String LABEL_USER_NAME = "User Name";
    private static final String LABEL_SERVER_URL = "Server URL";
    private static final String LABEL_CONTAINER_REGISTRY = "Container Registry";
    private static final String LABEL_DOCKER_FILE = "Docker File";
    private static final String SELECT_REGISTRY = "<Select Container Registry>";
    private static final String LOADING = "<Loading...>";
    private static final String SELECT_DOCKER_FILE = "Browse...";

    private Text txtServerUrl;
    private Text txtUserName;
    private Text txtPassword;
    private Text txtImageTag;
    private Text txtStartupFile;
    private Label lblStartupFile;
    private Combo cbContainerRegistry;
    private FileSelector dockerFileSelector;

    /**
     * Create the composite.
     */
    public ContainerSettingComposite(Composite parent, int style, String basePath) {
        super(parent, style);

        presenter = new ContainerSettingPresenter<>();
        presenter.onAttachView(this);
        projectBasePath = basePath;
        registryCache = new ArrayList<>();

        setLayout(new GridLayout(3, false));

        dockerFileSelector = new FileSelector(this, SWT.NONE, false, SELECT_DOCKER_FILE, projectBasePath,
                LABEL_DOCKER_FILE);
        dockerFileSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        Label lblContainerRegistry = new Label(this, SWT.NONE);
        lblContainerRegistry.setText(LABEL_CONTAINER_REGISTRY);

        cbContainerRegistry = new Combo(this, SWT.READ_ONLY);
        cbContainerRegistry.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

        Label lblServerUrl = new Label(this, SWT.NONE);
        lblServerUrl.setText(LABEL_SERVER_URL);

        txtServerUrl = new Text(this, SWT.BORDER);
        txtServerUrl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1));

        Label lblUserName = new Label(this, SWT.NONE);
        lblUserName.setText(LABEL_USER_NAME);

        txtUserName = new Text(this, SWT.BORDER);
        txtUserName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

        Label lblPassword = new Label(this, SWT.NONE);
        lblPassword.setText(LABEL_PASSWORD);

        txtPassword = new Text(this, SWT.BORDER | SWT.PASSWORD);
        txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

        Label lblImageAndTag = new Label(this, SWT.NONE);
        lblImageAndTag.setText(LABEL_IMAGE_AND_TAG);

        Label lblTagPrefix = new Label(this, SWT.NONE);
        lblTagPrefix.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        lblTagPrefix.setText(LABEL_TAG_PREFIX);

        txtImageTag = new Text(this, SWT.BORDER);
        txtImageTag.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        lblStartupFile = new Label(this, SWT.NONE);
        lblStartupFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        lblStartupFile.setText(LABEL_STARTUP_FILE);

        txtStartupFile = new Text(this, SWT.BORDER);
        txtStartupFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

        cbContainerRegistry.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                int index = cbContainerRegistry.getSelectionIndex();
                if (index == 0) {
                    enableWidgets();
                    return;
                }
                if (registryCache != null && index >= 1 && index < registryCache.size()) {
                    disableWidgets();
                    Registry registry = registryCache.get(index);
                    presenter.onGetRegistryCredential(registry);
                }
            }
        });

        txtServerUrl.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event event) {
                lblTagPrefix.setText(txtServerUrl.getText() + "/");
                layout();
            }
        });

        this.getShell().addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent arg0) {
                presenter.onDetachView();
            }
        });
    }

    public void addTxtServerUrlModifyListener(ModifyListener modifyListener) {
        txtServerUrl.addModifyListener(modifyListener);
    }

    @Override
    public void disposeEditor() {
    }

    @Override
    public void fillCredential(PrivateRegistryImageSetting setting) {
        txtServerUrl.setText(setting.getServerUrl());
        txtUserName.setText(setting.getUsername());
        txtPassword.setText(setting.getPassword());
        txtImageTag.setText(setting.getImageNameWithTag());
    }

    @Override
    public void listRegistries(@NotNull List<Registry> registryList) {
        cbContainerRegistry.removeAll();
        registryCache.clear();
        cbContainerRegistry.add(SELECT_REGISTRY);
        registryCache.add(null);
        for (Registry registry : registryList) {
            cbContainerRegistry.add(registry.name());
        }
        cbContainerRegistry.select(0);
        registryCache.addAll(registryList);
        layout();
    }

    @Override
    public void onListRegistries() {
        cbContainerRegistry.removeAll();
        cbContainerRegistry.add(LOADING);
        cbContainerRegistry.select(0);
        presenter.onListRegistries();
    }

    @Override
    public void setStartupFileVisible(boolean visible) {
        lblStartupFile.setVisible(visible);
        txtStartupFile.setVisible(visible);
        ((GridData) lblStartupFile.getLayoutData()).exclude = !visible;
        ((GridData) txtStartupFile.getLayoutData()).exclude = !visible;
        this.getParent().pack();
    }

    public void setDockerfilePath(String filePath) {
        dockerFileSelector.setFilePath(filePath);
    }

    public String getServerUrl() {
        return txtServerUrl.getText();
    }

    public String getUserName() {
        return txtUserName.getText();
    }

    public String getPassword() {
        return txtPassword.getText();
    }

    public String getImageTag() {
        return txtImageTag.getText();
    }

    public String getDockerfilePath() {
        return dockerFileSelector.getFilePath();
    }

    public String getStartupFile() {
        return txtStartupFile.getText();
    }

    private void disableWidgets() {
        txtServerUrl.setEnabled(false);
        txtUserName.setEnabled(false);
        txtPassword.setEnabled(false);
    }

    private void enableWidgets() {
        txtServerUrl.setEnabled(true);
        txtUserName.setEnabled(true);
        txtPassword.setEnabled(true);
    }
}
