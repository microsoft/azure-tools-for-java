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
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskContext;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
        switch (raw.getType()) {
            case ALERT:
            case CONFIRM:
                final String title = StringUtils.firstNonBlank(raw.getTitle(), DEFAULT_TITLE);
                return MessageDialogBuilder.yesNo(title, raw.getContent()).guessWindowAndAsk();
        }
        if (Objects.equals(getBackgrounded(raw), Boolean.FALSE) && raw.getType() == IAzureMessage.Type.ERROR) {
            this.showErrorDialog(raw);
        } else {
            this.showNotification(raw);
        }
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
        notification.addActions(message.getAnActions());
        Notifications.Bus.notify(notification, message.getProject());
    }

    @Nullable
    private Boolean getBackgrounded(IAzureMessage message) {
        if (!(message instanceof AzureMessage) || Objects.isNull(((AzureMessage) message).getBackgrounded())) {
            final AzureTask<?> task = AzureTaskContext.current().getTask();
            return Optional.ofNullable(task).map(AzureTask::getBackgrounded).orElse(null);
        }
        return ((AzureMessage) message).getBackgrounded();
    }
}
