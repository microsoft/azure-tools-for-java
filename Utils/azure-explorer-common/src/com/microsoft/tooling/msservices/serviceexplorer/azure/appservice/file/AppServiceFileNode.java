/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file;

import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.Sortable;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.util.Collections;
import java.util.Map;

@Log
public class AppServiceFileNode extends AzureRefreshableNode implements TelemetryProperties {
    private static final long SIZE_20MB = 20 * 1024 * 1024;

    private final AppServiceFile file;
    private final IAppService appService;

    public AppServiceFileNode(final AppServiceFile file, final Node parent, IAppService webApp) {
        super(file.getName(), file.getName(), parent, null);
        this.file = file;
        this.appService = webApp;
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

    @AzureOperation(name = "appservice|file.download", params = {"this.file.getName()"}, type = AzureOperation.Type.ACTION)
    private void download() {
        DefaultLoader.getIdeHelper().saveAppServiceFile(file, getProject(), null);
    }

    @Override
    @AzureOperation(name = "appservice|file.refresh", params = {"this.file.getName()"}, type = AzureOperation.Type.ACTION)
    protected void refreshItems() {
        executeWithTelemetryWrapper(TelemetryConstants.REFRESH_FILE, () -> {
            if (this.file.getType() != AppServiceFile.Type.DIRECTORY) {
                return;
            }
            appService.getFilesInDirectory(this.file.getPath()).stream()
                    .map(file -> new AppServiceFileNode(file, this, appService))
                    .forEach(this::addChildNode);
        });
    }

    @AzureOperation(name = "appservice|file.open", params = {"this.file.getName()"}, type = AzureOperation.Type.ACTION)
    private void open(final Object context) {
        executeWithTelemetryWrapper(TelemetryConstants.OPEN_FILE, () -> DefaultLoader.getIdeHelper().openAppServiceFile(this.file, context));
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
        final AzureString title = AzureOperationBundle.title("appservice|file.get_content", file.getName(), file.getApp().name());
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

    @Override
    public String getServiceName() {
        return file.getApp() instanceof IFunctionAppBase ? TelemetryConstants.FUNCTION : TelemetryConstants.WEBAPP;
    }

    @Override
    public Map<String, String> toProperties() {
        return Collections.singletonMap(AppInsightsConstants.SubscriptionId, Utils.getSubscriptionId(file.getApp().id()));
    }

    // todo: replace with AzureOperation when custom properties is supported for AzureOperation
    private void executeWithTelemetryWrapper(final String operationName, Runnable runnable) {
        EventUtil.executeWithLog(getServiceName(), operationName, operation -> {
            operation.trackProperty(TelemetryConstants.SUBSCRIPTIONID, Utils.getSubscriptionId(file.getApp().id()));
            runnable.run();
        });
    }
}
