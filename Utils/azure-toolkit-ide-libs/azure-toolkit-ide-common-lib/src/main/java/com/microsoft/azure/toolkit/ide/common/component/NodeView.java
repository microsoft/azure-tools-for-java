/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.lib.common.view.IView;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public interface NodeView extends IView.Label {

    default void refresh() {
        Optional.ofNullable(this.getRefresher()).ifPresent(Refresher::refreshView);
    }

    default void refreshView() {
        this.refresh();
    }

    default void refreshChildren() {
        Optional.ofNullable(this.getRefresher()).ifPresent(Refresher::refreshChildren);
    }

    void setRefresher(Refresher refresher);

    @Nullable
    Refresher getRefresher();

    interface Refresher {
        default void refreshView() {
        }

        default void refreshChildren() {
        }
    }

    class Static extends IView.Label.Static implements NodeView {
        private Refresher refresher;

        public Static(String title, String iconPath) {
            super(title, iconPath);
        }

        public Static(@Nonnull String label, @Nullable String iconPath, @Nullable String description) {
            super(label, iconPath, description);
        }

        @Override
        public void setRefresher(Refresher refresher) {
            this.refresher = refresher;
        }

        @Nullable
        @Override
        public Refresher getRefresher() {
            return this.refresher;
        }
    }
}
