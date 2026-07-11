/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.Optional;

/**
 * A JButton subclass that bridges Azure Toolkit {@link Action} with Swing buttons.
 * <p>
 * Overrides {@link AbstractButton#setAction(javax.swing.Action)} to prevent the inherited
 * Swing method from clearing the button text/icon when the IntelliJ form runtime or
 * Look-and-Feel calls it unexpectedly. This ensures compatibility with IntelliJ's New UI
 * (2026.1+) where DarculaButtonUI may interact with the Swing Action property.
 */
public class AzureActionButton<T> extends JButton {
    public static final DataKey<ActionEvent> ACTION_EVENT_KEY = DataKey.create("AzureActionButton.actionEvent");
    private final ActionListener listener = this::onActionPerformed;
    protected Action<T> action;

    public AzureActionButton() {
        super();
    }

    public AzureActionButton(@Nonnull final Action.Id<T> actionId) {
        this(AzureActionManager.getInstance().getAction(actionId));
    }

    public AzureActionButton(@Nonnull final Action<T> action) {
        super();
        this.action = action;
        this.registerActionListener();
    }

    /**
     * Override the inherited {@link AbstractButton#setAction(javax.swing.Action)} to prevent
     * the Swing Action mechanism from clearing button text and icon. In IntelliJ's New UI,
     * the form runtime or LAF may call this method, which by default calls
     * {@code configurePropertiesFromAction()} and resets all button properties (text, icon,
     * tooltip, enabled) from the javax.swing.Action — wiping out values set by the form
     * instrumentation or programmatic code.
     * <p>
     * This no-op override preserves the button's text and icon as set by the GUI form
     * or by calling {@link #setText(String)} / {@link #setIcon(Icon)} directly.
     */
    @Override
    public void setAction(javax.swing.Action a) {
        // Intentionally do NOT call super.setAction(a).
        // The Swing default would call configurePropertiesFromAction() which clears
        // text/icon/tooltip if the javax.swing.Action has no corresponding properties.
        // Our buttons get their text from the .form file and icons from Java code.
    }

    public void setAction(@Nonnull final Action.Id<T> actionId) {
        setAction(actionId, null);
    }

    public void setAction(@Nonnull final Action.Id<T> actionId, @Nullable T source) {
        final Action<T> action = AzureActionManager.getInstance().getAction(actionId);
        this.setAction(Objects.isNull(source) ? action : action.bind(source));
    }

    public void setAction(@Nonnull final Action<T> action) {
        this.action = action;
        this.registerActionListener();
    }

    private void registerActionListener() {
        final boolean registered = ArrayUtils.contains(this.getActionListeners(), this.listener);
        if (!registered) {
            this.addActionListener(this.listener);
        }
    }

    private void onActionPerformed(ActionEvent actionEvent) {
        final DataContext dataContext = DataManager.getInstance().getDataContext(this);
        final DataContext context = new DataContext() {
            @Override
            public Object getData(@Nonnull String dataId) {
                if (StringUtils.equals(dataId, ACTION_EVENT_KEY.getName())) {
                    return actionEvent;
                }
                return dataContext.getData(dataId);
            }

            @Override
            public <T> T getData(@Nonnull DataKey<T> key) {
                if (StringUtils.equals(key.getName(), ACTION_EVENT_KEY.getName())) {
                    @SuppressWarnings("unchecked")
                    T result = (T) actionEvent;
                    return result;
                }
                return dataContext.getData(key);
            }
        };
        // todo: use panel name as the action place
        final AnActionEvent event = AnActionEvent.createEvent(context, new Presentation(), "actionButton", ActionUiKind.NONE, null);
        Optional.ofNullable(action).ifPresent(a -> a.handle(null, event));
    }
}
