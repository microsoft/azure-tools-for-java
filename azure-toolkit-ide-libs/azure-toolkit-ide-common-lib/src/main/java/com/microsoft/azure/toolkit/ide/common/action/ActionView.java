/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.action;

import com.microsoft.azure.toolkit.ide.common.component.IView;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;

@Getter
@RequiredArgsConstructor
public class ActionView implements IView.Label {

    @Nonnull
    private final Label view;
    private final boolean enabled;

    @Override
    public String getTitle() {
        return this.view.getTitle();
    }

    @Override
    public String getIconPath() {
        return this.view.getIconPath();
    }

    @Override
    public String getDescription() {
        return this.view.getDescription();
    }

    @Override
    public void dispose() {
        this.view.dispose();
    }
}
