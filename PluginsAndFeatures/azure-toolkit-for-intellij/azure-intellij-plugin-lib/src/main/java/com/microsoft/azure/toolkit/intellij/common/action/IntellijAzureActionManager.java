/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.Action;
import com.microsoft.azure.toolkit.ide.common.action.ActionGroup;
import com.microsoft.azure.toolkit.ide.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.ide.common.component.IView;
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class IntellijAzureActionManager extends AzureActionManager {
    private static final ExtensionPointName<IActionsContributor> actionsExtensionPoint =
            ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.actions");

    private IntellijAzureActionManager() {
        super();
    }

    public static void register() {
        final IntellijAzureActionManager am = new IntellijAzureActionManager();
        register(am);
        final List<IActionsContributor> contributors = actionsExtensionPoint.getExtensionList();
        contributors.stream().sorted(Comparator.comparing(IActionsContributor::getZOrder)).forEach((e) -> e.registerActions(am));
        contributors.stream().sorted(Comparator.comparing(IActionsContributor::getZOrder)).forEach((e) -> e.registerHandlers(am));
        contributors.stream().sorted(Comparator.comparing(IActionsContributor::getZOrder)).forEach((e) -> e.registerGroups(am));
    }

    @Override
    public <T> void registerAction(String id, Action<T> action) {
        ActionManager.getInstance().registerAction(id, new AnActionWrapper<>(action));
    }

    @Override
    public <T> Action<T> getAction(String id) {
        //noinspection unchecked
        final AnActionWrapper<T> action = ((AnActionWrapper<T>) ActionManager.getInstance().getAction(id));
        return new Action.Proxy<>(action.getAction(), id);
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
    @RequiredArgsConstructor
    private static class AnActionWrapper<T> extends AnAction {
        @Nonnull
        private final Action<T> action;

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            final T source = (T) e.getDataContext().getData(Action.SOURCE);
            AzureTaskManager.getInstance().runOnPooledThread(() -> this.action.handle(source, e));
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            final T source = (T) e.getDataContext().getData(Action.SOURCE);
            final IView.Label view = this.action.view(source);
            final Presentation presentation = e.getPresentation();
            if (Objects.nonNull(view)) {
                if (Objects.nonNull(view.getIconPath()))
                    presentation.setIcon(AzureIcons.getIcon(view.getIconPath(), AnActionWrapper.class));
                presentation.setText(view.getTitle());
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
            final Presentation tmpl = this.getTemplatePresentation();
            if (Objects.nonNull(view)) {
                tmpl.setIcon(AzureIcons.getIcon(view.getIconPath(), AnActionWrapper.class));
                tmpl.setText(view.getTitle());
                tmpl.setDescription(view.getDescription());
            } else {
                tmpl.setText("Action Group");
            }
            this.addActions(group.actions());
        }

        private void addActions(List<Object> actions) {
            final ActionManager am = ActionManager.getInstance();
            for (final Object raw : actions) {
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
