/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Accessors(chain = true, fluent = true)
public class Node<D> {
    @Nonnull
    private final D data;
    @Nonnull
    @Getter(AccessLevel.NONE)
    private final List<ChildrenNodeBuilder<D, ?>> childrenBuilders = new ArrayList<>();
    @Nonnull
    @Setter
    private NodeView view;
    @Setter
    private boolean lazy = true;
    @Nullable
    private ActionGroup actions;
    private int order;

    public Node(@Nonnull D data) {
        this(data, null);
    }

    public Node(@Nonnull D data, @Nonnull NodeView view) {
        this.data = data;
        this.view = view;
    }

    public <C> Node<D> addChildren(
            @Nonnull Function<? super D, ? extends List<C>> getChildrenData,
            @Nonnull BiFunction<C, Node<D>, Node<?>> buildChildNode) {
        this.childrenBuilders.add(new ChildrenNodeBuilder<>(getChildrenData, buildChildNode));
        return this;
    }

    public Node<D> addChild(@Nonnull Function<? super Node<D>, ? extends Node<?>> buildChildNode) {
        return this.addChildren((d) -> Collections.singletonList(null), (cd, n) -> buildChildNode.apply(n));
    }

    public Node<D> addChildren(@Nonnull List<Node<?>> children) {
        return this.addChildren((d) -> children, (cd, n) -> cd);
    }

    public Node<D> addChild(@Nonnull Node<?> childNode) {
        return this.addChildren(Collections.singletonList(childNode));
    }

    public List<Node<?>> getChildren() {
        return this.childrenBuilders.stream().flatMap((builder) -> builder.build(this).stream()).collect(Collectors.toList());
    }

    public boolean hasChildren() {
        return !this.childrenBuilders.isEmpty();
    }

    public Node<D> actions(String groupId) {
        return this.actions(AzureActionManager.getInstance().getGroup(groupId));
    }

    public Node<D> actions(ActionGroup group) {
        this.actions = group;
        return this;
    }

    public void dispose() {
        this.view.dispose();
    }

    @RequiredArgsConstructor
    private static class ChildrenNodeBuilder<D, C> {
        private final Function<? super D, ? extends List<C>> getChildrenData;
        private final BiFunction<C, Node<D>, Node<?>> buildChildNode;

        private List<Node<?>> build(Node<D> n) {
            final val childrenData = this.getChildrenData.apply(n.data);
            return childrenData.stream().map(d -> buildChildNode.apply(d, n)).collect(Collectors.toList());
        }
    }
}
