/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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

    @Override
    public <D> void registerAction(Action.Id<D> id, Action<D> action) {
        ActionManager.getInstance().registerAction(id.getId(), new AnActionWrapper<>(action));
    }

    @Override
    public <D> Action<D> getAction(Action.Id<D> id) {
        //noinspection unchecked
        final AnActionWrapper<D> action = ((AnActionWrapper<D>) ActionManager.getInstance().getAction(id.getId()));
        return new Action.Delegate<>(action.getAction(), id.getId());
    }

    @Override
    public void registerGroup(String id, ActionGroup group) {
        ActionManager.getInstance().registerAction(id, new ActionGroupWrapper(group));
    }

    @Override
    public ActionGroup getGroup(String id) {
        final ActionGroupWrapper group = (ActionGroupWrapper) ActionManager.getInstance().getAction(id);
        return new ActionGroup.Proxy(group.getGroup(), id);
    }

    @Getter
    private static class AnActionWrapper<T> extends AnAction {
        @Nonnull
        private final Action<T> action;

        private AnActionWrapper(@Nonnull Action<T> action) {
            super();
            this.action = action;
            final IView.Label view = action.view(null);
            applyView(view, this.getTemplatePresentation());
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            final T source = (T) e.getDataContext().getData(Action.SOURCE);
            this.action.handle(source, e);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            final T source = (T) e.getDataContext().getData(Action.SOURCE);
            final IView.Label view = this.action.view(source);
            applyView(view, e.getPresentation());
        }

        private static void applyView(IView.Label view, Presentation presentation) {
            if (Objects.nonNull(view)) {
                if (Objects.nonNull(view.getIconPath()))
                    presentation.setIcon(AzureIcons.getIcon(view.getIconPath(), AnActionWrapper.class));
                presentation.setText(view.getLabel());
                presentation.setDescription(view.getDescription());
                presentation.setEnabled(view.isEnabled());
            }
        }
    }

    @Getter
    public static class ActionGroupWrapper extends DefaultActionGroup {

        private final ActionGroup group;

        public ActionGroupWrapper(@Nonnull ActionGroup group) {
            super();
            this.group = group;
            this.setSearchable(true);
            this.setPopup(true);
            final IView.Label view = this.group.view();
            final Presentation template = this.getTemplatePresentation();
            if (Objects.nonNull(view)) {
                AnActionWrapper.applyView(view, template);
            } else {
                template.setText("Action Group");
            }
            this.addActions(group.actions());
        }

        private void addActions(List<Object> actions) {
            final ActionManager am = ActionManager.getInstance();
            for (Object raw : actions) {
                if (raw instanceof Action.Id) {
                    raw = ((Action.Id<?>) raw).getId();
                }
                if (raw instanceof String) {
                    final String actionId = (String) raw;
                    if (actionId.startsWith("-")) {
                        final String title = actionId.replaceAll("-", "").trim();
                        if (StringUtils.isBlank(title)) this.addSeparator();
                        else this.addSeparator(title);
                    } else if (StringUtils.isNotBlank(actionId)) {
                        final AnAction action = am.getAction(actionId);
                        if (Objects.nonNull(action)) {
                            this.add(action);
                        }
                    }
                } else if (raw instanceof Action<?>) {
                    this.add(new AnActionWrapper<>((Action<?>) raw));
                } else if (raw instanceof ActionGroup) {
                    this.add(new ActionGroupWrapper((ActionGroup) raw));
                }
            }
        }
    }
}
