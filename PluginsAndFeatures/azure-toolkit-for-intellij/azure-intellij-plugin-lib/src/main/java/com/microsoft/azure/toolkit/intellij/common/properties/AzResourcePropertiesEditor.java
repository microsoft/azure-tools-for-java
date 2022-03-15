/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.BaseEditor;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public abstract class AzResourcePropertiesEditor<T extends AzResourceBase> extends BaseEditor {

    private final AzureEventBus.EventListener<Object, AzureEvent<Object>> listener;
    private final T resource;
    private final TailingDebouncer debouncer;
    protected final Project project;
    protected final AzureResourceEditorViewManager manager;

    public AzResourcePropertiesEditor(@Nonnull final VirtualFile virtualFile, @Nonnull T resource, @Nonnull final Project project) {
        super(virtualFile);
        this.resource = resource;
        this.project = project;
        this.manager = virtualFile.getUserData(AzureResourceEditorViewManager.AZURE_RESOURCE_EDITOR_MANAGER_KEY);
        this.listener = new AzureEventBus.EventListener<>(this::onEvent);
        AzureEventBus.on("resource.status_changed.resource", listener);
        AzureEventBus.on("resource.refreshed.resource", listener);
        this.debouncer = new TailingDebouncer(this::rerender, 500);
    }

    @Nonnull
    @Override
    public String getName() {
        return this.resource.getName();
    }

    private void onEvent(AzureEvent<Object> event) {
        final String type = event.getType();
        final Object source = event.getSource();
        if (source instanceof AzResourceBase && ((AzResourceBase) source).getId().equals(this.resource.getId())) {
            if (StringUtils.equalsAnyIgnoreCase(((AzResourceBase) source).getStatus(), "deleted", "removed")) {
                onResourceDeleted();
            } else {
                this.debouncer.debounce();
            }
        }
    }

    protected void onResourceDeleted() {
        this.manager.closeEditor(resource, project);
        final String message = String.format("Close properties view of \"%s\" because the resource is deleted.", this.resource.getName());
        AzureMessager.getMessager().warning(message);
    }

    @Override
    public void dispose() {
        AzureEventBus.off("resource.status_changed.resource", listener);
        AzureEventBus.off("resource.refreshed.resource", listener);
    }

    protected abstract void rerender();
}
