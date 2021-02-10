/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file;

import com.microsoft.azure.toolkit.lib.appservice.file.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.file.AppServiceFileService;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperationTitle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;

import javax.swing.*;

@Log
public class AppServiceFileNode extends AzureRefreshableNode {
    private static final String MODULE_ID = WebAppModule.class.getName();
    private static final long SIZE_20MB = 20 * 1024 * 1024;
    private final AppServiceFileService fileService;
    private final AppServiceFile file;

    public AppServiceFileNode(final AppServiceFile file, final Node parent, AppServiceFileService service) {
        super(file.getName(), file.getName(), parent, null);
        this.file = file;
        this.fileService = service;
        if (this.file.getType() != AppServiceFile.Type.DIRECTORY) {
            this.addDownloadAction();
        }
    }

    private void addDownloadAction() {
        this.addAction("Download", new NodeActionListener() {
            @Override
            protected void actionPerformed(final NodeActionEvent e) {
                download();
            }
        });
    }

    @AzureOperation(name = "appservice|file.download", params = {"@file.getName()"}, type = AzureOperation.Type.ACTION)
    private void download() {
        DefaultLoader.getIdeHelper().saveAppServiceFile(file, getProject(), null);
    }

    @Override
    @AzureOperation(name = "appservice|file.refresh", params = {"@file.getName()"}, type = AzureOperation.Type.ACTION)
    protected void refreshItems() {
        if (this.file.getType() != AppServiceFile.Type.DIRECTORY) {
            return;
        }
        this.fileService.getFilesInDirectory(this.file.getPath()).stream()
                        .map(file -> new AppServiceFileNode(file, this, fileService))
                        .forEach(this::addChildNode);
    }

    @AzureOperation(name = "appservice|file.open", params = {"@file.getName()"}, type = AzureOperation.Type.ACTION)
    private void open(final Object context) {
        DefaultLoader.getIdeHelper().openAppServiceFile(this.file, context);
    }

    @Override
    public void onNodeDblClicked(Object context) {
        if (this.file.getType() == AppServiceFile.Type.DIRECTORY) {
            return;
        } else if (this.file.getSize() > SIZE_20MB) {
            DefaultLoader.getUIHelper().showError("File is too large, please download it first", "File is Too Large");
            return;
        }
        final Runnable runnable = () -> open(context);
        final IAzureOperationTitle title = AzureOperationBundle.title("appservice|file.get_content", file.getName(), file.getApp().name());
        AzureTaskManager.getInstance().runInBackground(new AzureTask(this.getProject(), title, false, runnable));
    }

    @Override
    public int getPriority() {
        return this.file.getType() == AppServiceFile.Type.DIRECTORY ? Sortable.HIGH_PRIORITY : Sortable.DEFAULT_PRIORITY;
    }

    @Override
    public Icon getIcon() {
        return DefaultLoader.getIdeHelper().getFileTypeIcon(this.file.getName(), this.file.getType() == AppServiceFile.Type.DIRECTORY);
    }

    @Override
    public String getToolTip() {
        return file.getType() == AppServiceFile.Type.DIRECTORY ?
               String.format("Type: %s Date modified: %s", file.getMime(), file.getMtime()) :
               String.format("Type: %s Size: %s Date modified: %s", file.getMime(), FileUtils.byteCountToDisplaySize(file.getSize()), file.getMtime());
    }
}
