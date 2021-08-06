/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class AzureResourceLabelView<T extends IAzureResource<?>> implements IView.Label, IView.Dynamic, PropertyChangeListener {
    @Nonnull
    @Getter
    private final T resource;
    @Getter
    private final String title;
    @Getter
    private String description;
    @Nullable
    @Setter
    @Getter
    private Updater updater;

    public AzureResourceLabelView(@Nonnull T resource) {
        this.resource = resource;
        this.title = resource.name();
        resource.addPropertyChangeListener(this);
        this.updateView();
    }

    public void dispose() {
        this.resource.removePropertyChangeListener(this);
        this.updater = null;
    }

    public String getIconPath() {
        final String status = resource.status();
        final String type = resource.getClass().getSimpleName().toLowerCase();
        final String icon = StringUtils.isBlank(status) ? type : String.format("%s-%s", type, status.toLowerCase().trim());
        return String.format("/icons/%s.svg", icon);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        final String prop = e.getPropertyName();
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        if (prop.equals(IAzureResource.PROPERTY_STATUS)) {
            tm.runLater(this::updateView);
        } else if (prop.equals(IAzureResource.PROPERTY_CHILDREN)) {
            tm.runLater(this::updateChildren);
        }
    }
}
