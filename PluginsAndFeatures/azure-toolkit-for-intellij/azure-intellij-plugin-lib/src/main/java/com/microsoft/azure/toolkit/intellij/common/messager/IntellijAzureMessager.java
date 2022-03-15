/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class IntellijAzureMessager implements IAzureMessager {
    private static final Logger log = Logger.getInstance(AnAction.class);
    static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private static final Map<IAzureMessage.Type, NotificationType> types = Map.ofEntries(
        Map.entry(IAzureMessage.Type.INFO, NotificationType.INFORMATION),
        Map.entry(IAzureMessage.Type.SUCCESS, NotificationType.INFORMATION),
        Map.entry(IAzureMessage.Type.WARNING, NotificationType.WARNING),
        Map.entry(IAzureMessage.Type.ERROR, NotificationType.ERROR)
    );

    private Notification createNotification(@Nonnull String title, @Nonnull String content, NotificationType type) {
        return new Notification(NOTIFICATION_GROUP_ID, title, content, type, new NotificationListener.UrlOpeningListener(true));
    }

    @Override
    public boolean show(IAzureMessage raw) {
        if (raw.getPayload() instanceof Throwable) {
            log.warn("caught an error by messager", ((Throwable) raw.getPayload()));
        }
        switch (raw.getType()) {
            case ALERT:
            case CONFIRM:
                final boolean[] result = new boolean[]{true};
                try {
                    UIUtil.invokeAndWaitIfNeeded((ThrowableRunnable<?>) () -> {
                        final String title = StringUtils.firstNonBlank(raw.getTitle(), DEFAULT_TITLE);
                        result[0] = MessageDialogBuilder.yesNo(title, raw.getContent()).guessWindowAndAsk();
                    });
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
                return result[0];
            default:
        }
        this.showNotification(raw);
        return true;
    }

    private void showErrorDialog(@Nonnull IAzureMessage message) {
        UIUtil.invokeLaterIfNeeded(() -> {
            final IntellijAzureMessage error = new DialogMessage(message);
            final IntellijErrorDialog errorDialog = new IntellijErrorDialog(error);
            final Window window = errorDialog.getWindow();
            final Component modalityStateComponent = window.getParent() == null ? window : window.getParent();
            ApplicationManager.getApplication().invokeLater(errorDialog::show, ModalityState.stateForComponent(modalityStateComponent));
        });
    }

    private void showNotification(@Nonnull IAzureMessage raw) {
        final IntellijAzureMessage message = new NotificationMessage(raw);
        final NotificationType type = types.get(message.getType());
        final String content = message.getContent();
        final Notification notification = this.createNotification(message.getTitle(), content, type);
        notification.addActions(Arrays.stream(message.getActions()).map(a -> {
            final String title = Optional.ofNullable(a.getView(null)).map(IView.Label::getLabel).orElse(a.toString());
            return new NotificationAction(title) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                    a.handle(null, e);
                }
            };
        }).collect(Collectors.toList()));
        Notifications.Bus.notify(notification, message.getProject());
    }
}
