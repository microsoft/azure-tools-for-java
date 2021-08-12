/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.action;

import com.microsoft.azure.toolkit.ide.common.component.IView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

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

    @RequiredArgsConstructor
    @AllArgsConstructor
    @Setter
    @Getter
    @Accessors(chain = true, fluent = true)
    public static class Builder {
        @Nonnull
        protected final Function<Object, String> title;
        @Nullable
        protected Function<Object, String> iconPath;
        @Nullable
        protected Function<Object, String> description;
        @Nullable
        protected Function<Object, Boolean> enabled = s -> true;

        public Builder(String title) {
            this(s -> title);
        }

        public Builder(String title, String iconPath) {
            this(s -> title, s -> iconPath, null, (s) -> true);
        }

        public ActionView toActionView(Object s) {
            try {
                final Boolean enabled = Optional.ofNullable(this.enabled).map(p -> p.apply(s)).orElse(true);
                final String iconPath = Optional.ofNullable(this.iconPath).map(p -> p.apply(s)).orElse(null);
                final String description = Optional.ofNullable(this.description).map(p -> p.apply(s)).orElse(null);
                return new ActionView(new Static(this.title.apply(s), iconPath, description), enabled);
            } catch (final Exception e) {
                return new ActionView(new Static(""), false);
            }
        }
    }
}
