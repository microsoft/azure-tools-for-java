/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.action;

import com.microsoft.azure.toolkit.ide.common.component.IView;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Accessors(chain = true, fluent = true)
public class Action<D> {
    public static final String SOURCE = "ACTION_SOURCE";
    @Nonnull
    private List<AbstractMap.SimpleEntry<Object, Object>> handlers = new ArrayList<>();
    @Nullable
    @Getter
    private ActionView.Builder view;

    public Action(@Nullable ActionView.Builder view) {
        this.view = view;
    }

    public Action(@Nonnull Consumer<D> handler) {
        this.registerHandler((d) -> true, handler);
    }

    public <E> Action(@Nonnull BiConsumer<D, E> handler) {
        this.registerHandler((d, e) -> true, handler);
    }

    public Action(@Nonnull Consumer<D> handler, @Nullable ActionView.Builder view) {
        this.view = view;
        this.registerHandler((d) -> true, handler);
    }

    public <E> Action(@Nonnull BiConsumer<D, E> handler, @Nullable ActionView.Builder view) {
        this.view = view;
        this.registerHandler((d, e) -> true, handler);
    }

    private Action(@Nonnull List<AbstractMap.SimpleEntry<Object, Object>> handlers, @Nullable ActionView.Builder view) {
        this.view = view;
        this.handlers = handlers;
    }

    @Nullable
    public IView.Label view(D source) {
        return Objects.nonNull(this.view) ? this.view.toActionView(source) : null;
    }

    @SuppressWarnings("unchecked")
    public void handle(D source, Object e) {
        for (int i = this.handlers.size() - 1; i >= 0; i--) {
            final AbstractMap.SimpleEntry<Object, Object> p = this.handlers.get(i);
            final Object condition = p.getKey();
            if (condition instanceof BiPredicate && ((BiPredicate<D, Object>) condition).test(source, e)) {
                ((BiConsumer<D, Object>) p.getValue()).accept(source, e);
                return;
            } else if (condition instanceof Predicate && ((Predicate<D>) condition).test(source)) {
                ((Consumer<D>) p.getValue()).accept(source);
                return;
            }
        }
    }

    public void handle(D source) {
        this.handle(source, null);
    }

    public void registerHandler(@Nonnull Predicate<D> condition, @Nonnull Consumer<D> handler) {
        this.handlers.add(new AbstractMap.SimpleEntry<>(condition, handler));
    }

    public <E> void registerHandler(@Nonnull BiPredicate<D, E> condition, @Nonnull BiConsumer<D, E> handler) {
        this.handlers.add(new AbstractMap.SimpleEntry<>(condition, handler));
    }

    @Getter
    @Accessors(chain = true, fluent = true)
    public static class Delegate<D> extends Action<D> {
        @Nonnull
        private final String id;
        @Nonnull
        private final Action<D> action;

        public Delegate(@Nonnull Action<D> action, @Nonnull String id) {
            super(action.handlers, action.view);
            this.id = id;
            this.action = action;
        }
    }

    public static class Id<D> {
        @Nonnull
        private final String id;

        private Id(@Nonnull String id) {
            this.id = id;
        }

        public static <D> Id<D> of(@Nonnull String id) {
            assert StringUtils.isNotBlank(id) : "action id can not be blank";
            return new Id<>(id);
        }

        @Nonnull
        public String getId() {
            return id;
        }
    }
}

