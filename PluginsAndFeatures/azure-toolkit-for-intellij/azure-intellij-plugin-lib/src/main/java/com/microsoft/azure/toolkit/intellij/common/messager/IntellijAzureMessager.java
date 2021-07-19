/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import lombok.extern.java.Log;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

@Log
public class IntellijAzureMessager implements IAzureMessager {
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
            log.log(Level.WARNING, "caught an error by messager", ((Throwable) raw.getPayload()));
        }

        final IntellijAzureMessage message = IntellijAzureMessage.from(raw);
        switch (message.getType()) {
            case ALERT:
            case CONFIRM:
                return MessageDialogBuilder.yesNo(message.getTitle(), message.getMessage()).guessWindowAndAsk();
        }
        if (Objects.equals(message.getBackgrounded(), Boolean.FALSE) && message.getType() == IAzureMessage.Type.ERROR) {
            this.showErrorDialog(message);
        } else {
            this.showNotification(message);
        }
        return true;
    }

    private void showErrorDialog(@Nonnull AzureMessage message) {
        UIUtil.invokeLaterIfNeeded(() -> {
            final IntellijAzureMessage error = new DialogMessage(message);
            final IntellijErrorDialog errorDialog = new IntellijErrorDialog(error);
            final Window window = errorDialog.getWindow();
            final Component modalityStateComponent = window.getParent() == null ? window : window.getParent();
            ApplicationManager.getApplication().invokeLater(errorDialog::show, ModalityState.stateForComponent(modalityStateComponent));
        });
    }

    private void showNotification(@Nonnull AzureMessage raw) {
        final IntellijAzureMessage message = new NotificationMessage(raw);
        final NotificationType type = types.get(message.getType());
        final String content = message.getMessage();
        final Notification notification = this.createNotification(message.getTitle(), content, type);
        notification.addActions(message.getAnActions());
        Notifications.Bus.notify(notification, message.getProject());
    }
}
