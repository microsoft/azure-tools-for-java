/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.DumbAware;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class IntellijAzureActionManager extends AzureActionManager {
    private static final ExtensionPointName<IActionsContributor> actionsExtensionPoint =
        ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.actions");

    /**
     * register {@code ACTION_SOURCE} as data key, so that PreCachedDataContext can pre cache it.
     */
    private static final DataKey<Object> ACTION_SOURCE = DataKey.create("ACTION_SOURCE");

    private IntellijAzureActionManager() {
        super();
    }

    public static void register() {
        final IntellijAzureActionManager am = new IntellijAzureActionManager();
        register(am);
        final List<IActionsContributor> contributors = actionsExtensionPoint.getExtensionList();
        contributors.stream().sorted(Comparator.comparing(IActionsContributor::getOrder)).forEach((e) -> e.registerActions(am));
        contributors.stream().sorted(Comparator.comparing(IActionsContributor::getOrder)).forEach((e) -> e.registerHandlers(am));
        contributors.stream().sorted(Comparator.comparing(IActionsContributor::getOrder)).forEach((e) -> e.registerGroups(am));
    }

    public <D> void registerAction(Action<D> action) {
        final ActionManager manager = ActionManager.getInstance();
        if (Objects.isNull(manager.getAction(action.getId()))) {
            manager.registerAction(action.getId(), new AnActionWrapper<>(action));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <D> Action<D> getAction(Action.Id<D> id) {
        final AnAction origin = ActionManager.getInstance().getAction(id.getId());
        if (origin instanceof AnActionWrapper) {
            return (Action<D>) ((AnActionWrapper<?>) origin).getAction();
        } else {
            return new Action<>(id)
                .withLabel(Objects.requireNonNull(origin.getTemplateText()))
                .withHandler((D d, AnActionEvent e) -> origin.actionPerformed(e))
                .withAuthRequired(false);
        }
    }

    @Override
    public void registerGroup(String id, ActionGroup group) {
        final ActionGroupWrapper nativeGroup = new ActionGroupWrapper(group);
        group.setOrigin(nativeGroup);
        final ActionManager manager = ActionManager.getInstance();
        if (Objects.isNull(manager.getAction(id))) {
            manager.registerAction(id, nativeGroup);
        }
    }

    @Override
    public IActionGroup getGroup(String id) {
        return (ActionGroupWrapper) ActionManager.getInstance().getAction(id);
    }

    @Getter
    private static class AnActionWrapper<T> extends AnAction implements DumbAware {
        @Nonnull
        private final Action<T> action;

        private AnActionWrapper(@Nonnull Action<T> action) {
            super();
            this.action = action;
        }

        @Nullable
        public ShortcutSet getShortcuts() {
            final Object shortcuts = action.getShortcut();
            if (shortcuts instanceof Action.Id) {
                return ActionManager.getInstance().getAction(((Action.Id<?>) shortcuts).getId()).getShortcutSet();
            } else if (shortcuts instanceof String) {
                return CustomShortcutSet.fromString((String) shortcuts);
            } else if (shortcuts instanceof String[]) {
                return CustomShortcutSet.fromString((String[]) shortcuts);
            } else if (shortcuts instanceof ShortcutSet) {
                return (ShortcutSet) shortcuts;
            } else {
                return null;
            }
        }

        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            final T source = (T) e.getDataContext().getData(Action.SOURCE);
            this.action.handle(source, e);
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            final T source = (T) e.getDataContext().getData(Action.SOURCE);
            final Presentation presentation = e.getPresentation();
            final IView.Label view = this.action.getView(source);

            boolean visible;
            if(source instanceof AbstractAzResource && ((AbstractAzResource<?, ?, ?>) source).getSubscription().getId().equals("[LinkedCluster]")) {
                visible = true;
            } else {
                final boolean isResourceInOtherSubs = source instanceof AbstractAzResource && !((AbstractAzResource<?, ?, ?>) source).getSubscription().isSelected() && this.action.isAuthRequired();
                visible = !isResourceInOtherSubs && view.isVisible() && Objects.nonNull(action.getHandler(source, e));
            }
            presentation.setVisible(visible);
            if (visible) {
                final boolean enabled = view.isEnabled() && Objects.nonNull(action.getHandler(source, e));
                presentation.setEnabled(enabled);
                presentation.setIcon(Optional.ofNullable(view.getIconPath()).map(IntelliJAzureIcons::getIcon).orElse(null));
                presentation.setText(view.getLabel());
                presentation.setDescription(view.getDescription());
            }
        }
    }

    @Getter
    public static class ActionGroupWrapper extends DefaultActionGroup implements IActionGroup, DumbAware {

        private final ActionGroup group;

        public ActionGroupWrapper(@Nonnull ActionGroup group) {
            super();
            this.group = group;
            this.setPopup(true);
            final IView.Label view = this.group.getView();
            final Presentation template = this.getTemplatePresentation();
            if (Objects.nonNull(view)) {
                template.setText(view.getLabel());
            } else {
                template.setText("Action Group");
            }
            this.addActions(group.getActions());
        }

        private void addActions(List<Object> actions) {
            for (final Object raw : actions) {
                doAddAction(raw);
            }
        }

        @Override
        public IView.Label getView() {
            return group.getView();
        }

        @Override
        public List<Object> getActions() {
            return group.getActions();
        }

        @Override
        public void addAction(Object raw) {
            this.group.addAction(raw);
            this.doAddAction(raw);
        }

        public void doAddAction(Object raw) {
            if (raw instanceof Action.Id) {
                raw = ((Action.Id<?>) raw).getId();
            }
            if (raw instanceof String) {
                final String actionId = (String) raw;
                if (actionId.startsWith("-")) {
                    final String title = actionId.replaceAll("-", "").trim();
                    if (StringUtils.isBlank(title)) {
                        this.addSeparator();
                    } else {
                        this.addSeparator(title);
                    }
                } else if (StringUtils.isNotBlank(actionId)) {
                    final ActionManager am = ActionManager.getInstance();
                    final AnAction action = am.getAction(actionId);
                    if (action instanceof com.intellij.openapi.actionSystem.ActionGroup) {
                        this.add(action);
                    } else if (Objects.nonNull(action)) {
                        this.add(EmptyAction.wrap(action));
                    }
                }
            } else if (raw instanceof Action<?>) {
                this.add(new AnActionWrapper<>((Action<?>) raw));
            } else if (raw instanceof ActionGroup) {
                this.add(new ActionGroupWrapper((ActionGroup) raw));
            }
        }

        public void registerCustomShortcutSetForActions(JComponent component, @Nullable Disposable disposable) {
            for (final AnAction origin : this.getChildActionsOrStubs()) {
                final AnAction real = origin instanceof EmptyAction.MyDelegatingAction ?
                    ((EmptyAction.MyDelegatingAction) origin).getDelegate() : origin;
                if (real instanceof AnActionWrapper) {
                    final ShortcutSet shortcuts = ((AnActionWrapper<?>) real).getShortcuts();
                    if (Objects.nonNull(shortcuts)) {
                        origin.registerCustomShortcutSet(shortcuts, component, disposable);
                    }
                }
            }
        }
    }

    @Override
    public Shortcuts getIDEDefaultShortcuts() {
        return new Shortcuts() {
            @Override
            public Object add() {
                return CommonShortcuts.getNew();
            }

            @Override
            public Object delete() {
                return CommonShortcuts.getDelete();
            }

            @Override
            public Object view() {
                return CommonShortcuts.getViewSource();
            }

            @Override
            public Object edit() {
                return CommonShortcuts.getEditSource();
            }

            @Override
            public Object refresh() {
                return Action.Id.of(IdeActions.ACTION_REFRESH);
            }

            @Override
            public Object start() {
                return "ctrl F1";
            }

            @Override
            public Object restart() {
                return "ctrl alt F5";
            }

            @Override
            public Object stop() {
                return Action.Id.of(IdeActions.ACTION_STOP_PROGRAM);
            }

            @Override
            public Object deploy() {
                return Action.Id.of(IdeActions.ACTION_DEFAULT_RUNNER);
            }
        };
    }
}
