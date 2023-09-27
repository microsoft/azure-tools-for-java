/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.utils.Debouncer;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Node<D> {
    @Nonnull
    @Getter
    @EqualsAndHashCode.Include
    private final D value;
    @Nullable
    private Function<D, AzureIcon> iconBuilder;
    @Nullable
    private Function<D, String> labelBuilder;
    @Getter
    @Nullable
    private Function<D, String> descBuilder;
    @Nullable
    private Function<D, String> tipsBuilder;
    @Nonnull
    private Predicate<D> enableWhen = o -> true;
    @Nonnull
    private Predicate<D> visibleWhen = o -> true;
    @Nonnull
    private final List<BiConsumer<D, Object>> clickHandlers = new ArrayList<>();
    @Nonnull
    private final List<BiConsumer<D, Object>> doubleClickHandlers = new ArrayList<>();
    @Nonnull
    private final List<ChildrenBuilder<D, ?>> childrenBuilders = new ArrayList<>();
    private Consumer<? super D> moreChildrenLoader;
    // by default, we will load all children, so return false for has more child
    @Getter(AccessLevel.NONE)
    private Predicate<? super D> hasMoreChildren = ignore -> false;
    @Getter
    @Nullable
    private IActionGroup actions;
    @Getter
    @Nonnull
    private final List<Action<? super D>> inlineActions = new ArrayList<>();
    @Setter
    @Nullable
    protected ViewRenderer viewRenderer;
    @Setter
    @Nullable
    protected ChildrenRenderer childrenRenderer;
    @Getter
    private boolean lazy = true;
    private final Map<String, Object> data = new HashMap<>();
    private final AtomicReference<List<Node<?>>> children = new AtomicReference<>();
    private final AtomicReference<View> view = new AtomicReference<>();
    private final Debouncer refreshViewLater = new TailingDebouncer(this::refreshView, 500);
    private final Debouncer refreshChildrenLater = new TailingDebouncer(this::refreshChildren, 500);
    @Nullable
    private Boolean resetChildrenLater; // for debouncing `refreshChildren`

    public Node(@Nonnull D value) {
        this.value = value;
    }

    public Node<D> withIcon(@Nonnull final String iconPath) {
        this.iconBuilder = (any) -> AzureIcon.builder().iconPath(iconPath).build();
        return this;
    }

    public Node<D> withIcon(@Nonnull final AzureIcon icon) {
        this.iconBuilder = (any) -> icon;
        return this;
    }

    public Node<D> withIcon(@Nonnull final Function<D, AzureIcon> iconProvider) {
        this.iconBuilder = iconProvider;
        return this;
    }

    public Node<D> withLabel(@Nonnull final String label) {
        this.labelBuilder = (any) -> label;
        return this;
    }

    public Node<D> withLabel(@Nonnull final Function<D, String> labelProvider) {
        this.labelBuilder = labelProvider;
        return this;
    }

    public Node<D> withDescription(@Nonnull final String desc) {
        this.descBuilder = (any) -> desc;
        return this;
    }

    public Node<D> withDescription(@Nonnull final Function<D, String> descProvider) {
        this.descBuilder = descProvider;
        return this;
    }


    public Node<D> withTips(@Nonnull final String tips) {
        this.tipsBuilder = (any) -> tips;
        return this;
    }

    public Node<D> withTips(@Nonnull final Function<D, String> tipsProvider) {
        this.tipsBuilder = tipsProvider;
        return this;
    }

    public Node<D> enableWhen(@Nonnull Predicate<D> enableWhen) {
        this.enableWhen = enableWhen;
        return this;
    }

    public Node<D> visibleWhen(@Nonnull Predicate<D> visibleWhen) {
        this.visibleWhen = visibleWhen;
        return this;
    }

    public Node<D> withChildrenLoadLazily(final boolean lazy) {
        this.lazy = lazy;
        return this;
    }

    public Node<D> withActions(String groupId) {
        return this.withActions(AzureActionManager.getInstance().getGroup(groupId));
    }

    public Node<D> withActions(IActionGroup group) {
        this.actions = group;
        return this;
    }

    public Node<D> onClicked(@Nonnull final BiConsumer<D, Object> clickHandler) {
        this.clickHandlers.add(clickHandler);
        return this;
    }

    public Node<D> onClicked(@Nonnull final Consumer<D> clickHandler) {
        this.clickHandlers.add((d, o) -> clickHandler.accept(d));
        return this;
    }

    public Node<D> onClicked(Action.Id<? super D> actionId) {
        this.clickHandlers.add((d, o) -> AzureActionManager.getInstance().getAction(actionId).handle(d, o));
        return this;
    }

    public void click(final Object event) {
        if (!this.clickHandlers.isEmpty()) {
            this.clickHandlers.forEach(h -> h.accept(this.value, event));
        }
    }

    public Node<D> onDoubleClicked(@Nonnull final BiConsumer<D, Object> dblClickHandler) {
        this.doubleClickHandlers.add(dblClickHandler);
        return this;
    }

    public Node<D> onDoubleClicked(@Nonnull final Consumer<D> dblClickHandler) {
        this.doubleClickHandlers.add((d, o) -> dblClickHandler.accept(d));
        return this;
    }

    public Node<D> onDoubleClicked(Action.Id<? super D> actionId) {
        this.doubleClickHandlers.add((d, o) -> AzureActionManager.getInstance().getAction(actionId).handle(d, o));
        return this;
    }

    public void doubleClick(final Object event) {
        if (!this.doubleClickHandlers.isEmpty()) {
            this.doubleClickHandlers.forEach(h -> h.accept(this.value, event));
        }
    }

    public Node<D> addChildren(@Nonnull List<Node<?>> children) {
        return this.addChildren((d) -> children, (cd, n) -> cd);
    }

    public Node<D> addChildren(@Nonnull Function<? super D, ? extends List<Node<?>>> getChildrenNodes) {
        return this.addChildren(getChildrenNodes, (cd, n) -> cd);
    }

    public <C> Node<D> addChildren(
        @Nonnull Function<? super D, ? extends List<C>> getChildrenData,
        @Nonnull BiFunction<C, Node<D>, Node<?>> buildChildNode) {
        this.childrenBuilders.add(new ChildrenBuilder<>(getChildrenData, buildChildNode));
        return this;
    }

    public Node<D> addChild(@Nonnull Node<?> childNode) {
        return this.addChildren(Collections.singletonList(childNode));
    }

    public Node<D> addChild(@Nonnull Function<? super Node<D>, ? extends Node<?>> buildChildNode) {
        return this.addChildren((d) -> Collections.singletonList(this), (cd, n) -> buildChildNode.apply(n));
    }

    public <C> Node<D> addChild(
        @Nonnull Function<? super D, ? extends C> getChildData,
        @Nonnull BiFunction<C, Node<D>, Node<?>> buildChildNode) {
        this.childrenBuilders.add(new ChildrenBuilder<>(d -> Collections.singletonList(getChildData.apply(d)), buildChildNode));
        return this;
    }

    public Node<D> withMoreChildren(@Nonnull Predicate<? super D> hasMoreChildren, Consumer<? super D> moreChildrenLoader) {
        this.hasMoreChildren = hasMoreChildren;
        this.moreChildrenLoader = moreChildrenLoader;
        return this;
    }

    public List<Node<?>> getChildren() {
        if (this.children.compareAndSet(null, Collections.emptyList())) {
            this.refreshChildrenLater();
        }
        return this.children.get();
    }

    @Nonnull
    @ToString.Include
    public View getView() {
        if (this.view.compareAndSet(null, new View(AzureIcons.Common.REFRESH_ICON, this.buildLabel()))) {
            this.refreshViewLater();
        }
        return this.view.get();
    }

    protected List<Node<?>> buildChildren() {
        try {
            return this.childrenBuilders.stream().flatMap((builder) -> builder.build(this)).collect(Collectors.toList());
        } catch (final Exception e) {
            final Throwable root = ExceptionUtils.getRootCause(e);
            if (!(root instanceof InterruptedException)) {
                AzureMessager.getMessager().error(e);
            }
            return Collections.emptyList();
        }
    }

    protected View buildView() {
        try {
            final String label = this.buildLabel();
            final AzureIcon icon = this.buildIcon();
            final String desc = this.buildDescription();
            final String tips = this.buildTips();
            final boolean enabled = this.checkEnabled();
            final boolean visible = this.checkVisible();
            return new View(icon, label, desc, tips, enabled, visible);
        } catch (final Exception e) {
            final Throwable root = ExceptionUtils.getRootCause(e);
            if (!(root instanceof InterruptedException)) {
                AzureMessager.getMessager().error(e);
            }
            return new View(AzureIcons.Common.UNKNOWN_ICON, buildLabel());
        }
    }

    protected synchronized void refreshChildren() {
        final boolean incremental = BooleanUtils.isFalse(this.resetChildrenLater);
        this.view.compareAndSet(null, new View(AzureIcons.Common.REFRESH_ICON, this.buildLabel()));
        this.view.get().setIcon(AzureIcons.Common.REFRESH_ICON);
        this.rerenderView();
        this.children.set(this.buildChildren());
        this.rerenderChildren(incremental);
        this.view.set(this.buildView());
        this.rerenderView();
    }

    protected synchronized void refreshView() {
        this.view.compareAndSet(null, new View(AzureIcons.Common.REFRESH_ICON, this.buildLabel()));
        this.view.get().setIcon(AzureIcons.Common.REFRESH_ICON);
        this.rerenderView();
        this.view.set(this.buildView());
        this.rerenderView();
    }

    public void refreshChildrenLater(boolean... incremental) {
        this.resetChildrenLater = BooleanUtils.isTrue(this.resetChildrenLater) || Objects.isNull(incremental) || incremental.length < 1 || !incremental[0];
        this.refreshChildrenLater.debounce();
    }

    public void refreshViewLater() {
        this.refreshViewLater.debounce();
    }

    public void refreshViewLater(int delay) {
        this.refreshViewLater.debounce(delay);
    }

    private void rerenderChildren(boolean... incremental) {
        Optional.ofNullable(this.childrenRenderer).ifPresent(r -> r.updateChildren(incremental));
    }

    private void rerenderView() {
        Optional.ofNullable(this.viewRenderer).ifPresent(ViewRenderer::updateView);
    }

    public AzureIcon buildIcon() {
        return Optional.ofNullable(this.iconBuilder).map(p -> p.apply(this.value)).orElse(null);
    }

    @EqualsAndHashCode.Include
    public String getLabel() {
        return this.buildLabel();
    }

    @Nonnull
    public String buildLabel() {
        return Optional.ofNullable(this.labelBuilder).map(p -> p.apply(this.value)).orElseGet(this.value::toString);
    }

    public String buildDescription() {
        return Optional.ofNullable(this.descBuilder).map(p -> p.apply(this.value)).orElse(null);
    }

    public String buildTips() {
        return Optional.ofNullable(this.tipsBuilder).map(p -> p.apply(this.value)).orElse(null);
    }

    public boolean checkEnabled() {
        return this.enableWhen.test(this.value);
    }

    public boolean checkVisible() {
        return this.visibleWhen.test(this.value);
    }

    public boolean hasChildren() {
        return !this.childrenBuilders.isEmpty();
    }

    public boolean hasMoreChildren() {
        return this.hasMoreChildren.test(this.value);
    }

    public void loadMoreChildren() {
        this.moreChildrenLoader.accept(this.value);
    }

    public <U> U get(String key) {
        //noinspection unchecked
        return (U) this.data.get(key);
    }

    public <U> U getOrDefault(String key, U defaultValue) {
        // noinspection unchecked
        return (U) this.data.getOrDefault(key, defaultValue);
    }

    public void set(String key, Object value) {
        this.data.put(key, value);
    }

    public void triggerInlineAction(final Object event, int index, final String place) {
        final List<Action<? super D>> enabledActions = this.inlineActions.stream()
            .filter(action -> action.getView(this.value, place).isEnabled()).collect(Collectors.toList());
        if (index >= 0 && index < enabledActions.size()) {
            Optional.ofNullable(enabledActions.get(index)).ifPresent(a -> a.handle(this.value, event));
        }
    }

    public Node<D> addInlineAction(Action.Id<? super D> actionId) {
        this.inlineActions.add(AzureActionManager.getInstance().getAction(actionId));
        return this;
    }

    public void dispose() {
        this.setChildrenRenderer(null);
        this.setViewRenderer(null);
    }

    @RequiredArgsConstructor
    private static class ChildrenBuilder<D, C> {
        private final Function<? super D, ? extends List<C>> getChildrenData;
        private final BiFunction<C, Node<D>, Node<?>> buildChildNode;

        private Stream<Node<?>> build(Node<D> n) {
            final List<C> childrenData = this.getChildrenData.apply(n.value);
            return childrenData.stream().filter(Objects::nonNull).map(d -> buildChildNode.apply(d, n));
        }
    }

    @Setter
    @Getter
    @RequiredArgsConstructor
    @AllArgsConstructor
    @ToString(onlyExplicitlyIncluded = true)
    public static class View implements IView.Label {
        @ToString.Include
        private AzureIcon icon;
        @Nonnull
        @ToString.Include
        private final String label;
        private String description;
        @Getter(AccessLevel.NONE)
        private String tips;
        private boolean enabled = true;
        private boolean visible = true;

        public View(AzureIcon icon, @Nonnull String label) {
            this.label = label;
            this.icon = icon;
        }

        @Nullable
        public String getIconPath() {
            return Objects.isNull(this.icon) ? null : this.icon.getIconPath();
        }

        public String getTips() {
            return Optional.ofNullable(this.tips).filter(StringUtils::isNotBlank).or(() -> Optional.ofNullable(this.getDescription())).filter(StringUtils::isNotBlank).orElseGet(this::getLabel);
        }

        @Override
        public void dispose() {
        }
    }

    @FunctionalInterface
    public static interface ViewRenderer {
        void updateView();
    }

    @FunctionalInterface
    public static interface ChildrenRenderer {
        void updateChildren(boolean... incremental);
    }
}
