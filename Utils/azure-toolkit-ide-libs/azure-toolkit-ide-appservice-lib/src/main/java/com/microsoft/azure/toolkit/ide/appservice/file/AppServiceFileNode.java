/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.file;

import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AppServiceFileNode extends Node<AppServiceFile> {
    public static final String FILE_EXTENSION_ICON_PREFIX = "file/";

    private final AppServiceFile file;
    private final IAppService<?> appService;
    private final NodeView view;
    private final String label;

    public static AppServiceFile getRootFileNodeForAppService(@Nonnull IAppService<?> appService) {
        final AppServiceFile appServiceFile = new AppServiceFile();
        appServiceFile.setName("Files");
        appServiceFile.setPath("/site/wwwroot");
        appServiceFile.setMime("inode/directory");
        appServiceFile.setApp(appService);
        return appServiceFile;
    }

    public static AppServiceFile getRootLogNodeForAppService(@Nonnull IAppService<?> appService) {
        final AppServiceFile appServiceFile = new AppServiceFile();
        appServiceFile.setName("Logs");
        appServiceFile.setPath("/LogFiles");
        appServiceFile.setMime("inode/directory");
        appServiceFile.setApp(appService);
        return appServiceFile;
    }

    public AppServiceFileNode(@Nonnull AppServiceFile data) {
        super(data);
        this.file = data;
        this.appService = data.getApp();
        this.view = new AppServiceFileLabelView(data);
        final String actionGroupId = data.getType() == AppServiceFile.Type.DIRECTORY ?
                AppServiceFileActionsContributor.APP_SERVICE_DIRECTORY_ACTIONS : AppServiceFileActionsContributor.APP_SERVICE_FILE_ACTIONS;
        this.actions(actionGroupId);
        this.label = data.getName();
    }

    @Nonnull
    @Override
    public NodeView view() {
        return this.view;
    }

    @Override
    public boolean hasChildren() {
        return file.getType() == AppServiceFile.Type.DIRECTORY;
    }

    @Override
    public List<Node<?>> getChildren() {
        return file.getType() != AppServiceFile.Type.DIRECTORY ? Collections.emptyList() :
                appService.getFilesInDirectory(file.getPath()).stream()
                        .sorted((first, second) -> first.getType() == second.getType() ?
                                StringUtils.compare(first.getName(), second.getName()) :
                                first.getType() == AppServiceFile.Type.DIRECTORY ? -1 : 1)
                        .map(AppServiceFileNode::new)
                        .collect(Collectors.toList());
    }

    static class AppServiceFileLabelView implements NodeView {
        @Nonnull
        @Getter
        private final AppServiceFile file;
        private final AzureEventBus.EventListener<Object, AzureEvent<Object>> listener;

        @Nullable
        @Setter
        @Getter
        private Refresher refresher;

        public AppServiceFileLabelView(@Nonnull AppServiceFile file) {
            this.file = file;
            this.listener = new AzureEventBus.EventListener<>(this::onEvent);
            AzureEventBus.on("common|resource.refresh", listener);
            this.refreshView();
        }

        private void onEvent(AzureEvent<Object> event) {
            final String type = event.getType();
            final Object source = event.getSource();
            if (source instanceof AppServiceFile && StringUtils.equalsIgnoreCase(((AppServiceFile) source).getFullName(), this.file.getFullName())) {
                AzureTaskManager.getInstance().runLater(this::refreshChildren);
            }
        }

        @Override
        public String getLabel() {
            return file.getName();
        }

        @Override
        public String getIconPath() {
            return file.getType() == AppServiceFile.Type.DIRECTORY ? "/icons/storagefolder.png" :
                    FILE_EXTENSION_ICON_PREFIX +  FilenameUtils.getExtension(file.getName());
        }

        @Override
        public String getDescription() {
            return file.getType() == AppServiceFile.Type.DIRECTORY ?
                    String.format("Type: %s Date modified: %s", file.getMime(), file.getMtime()) :
                    String.format("Type: %s Size: %s Date modified: %s", file.getMime(), FileUtils.byteCountToDisplaySize(file.getSize()), file.getMtime());
        }

        @Override
        public void dispose() {
            AzureEventBus.off("common|resource.refresh", listener);
            this.refresher = null;
        }
    }
}
