/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ActionNode<T> extends Node<Action.Id<T>> {
    private Action.Id<T> value;
    private T resource;

    public ActionNode(@Nonnull Action.Id<T> id, @Nonnull final T resource) {
        super(id);
        this.value = id;
        this.resource = resource;

        this.withIcon(i -> AzureIcon.builder().iconPath(getAction().getView(resource).getIconPath()).build());
        this.withLabel(i -> getAction().getView(resource).getLabel());
        this.withDescription(i -> getAction().getView(resource).getDescription());
        this.onClicked((action, event) -> AzureActionManager.getInstance().getAction(action).handle(resource, event));
    }

    public boolean isEnabled() {
        return getAction().getView(resource).isEnabled();
    }

    public void invoke(@Nullable final Object event) {
        getAction().handle(resource, event);
    }

    private Action<T> getAction() {
        return AzureActionManager.getInstance().getAction(value);
    }
}
