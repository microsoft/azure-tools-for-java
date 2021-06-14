/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class IntellijAzureMessager implements IAzureMessager {
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private static final String DEFAULT_MESSAGE_TITLE = "Azure";

    private Notification createNotification(@Nonnull String title, @Nonnull String message, NotificationType type) {
        return new Notification(NOTIFICATION_GROUP_ID, title, message, type, new NotificationListener.UrlOpeningListener(true));
    }

    private String getTitle(String title) {
        if (StringUtils.isEmpty(title)) {
            return DEFAULT_MESSAGE_TITLE;
        }
        return title;
    }

    public String value(String val) {
        return val;
    }

    @Override
    public boolean show(IAzureMessage message) {
        final NotificationType type;
        switch (message.getType()) {
            case ALERT:
                return MessageDialogBuilder.okCancel(getTitle(message.getTitle()), message.getMessage()).guessWindowAndAsk();
            case CONFIRM:
                return MessageDialogBuilder.yesNo(getTitle(message.getTitle()), message.getMessage()).guessWindowAndAsk();
            case ERROR:
                if (Objects.nonNull(message.getPayload())) {
                    return true;
                }
                type = NotificationType.ERROR;
                break;
            case WARNING:
                type = NotificationType.WARNING;
                break;
            case INFO:
            case SUCCESS:
            default:
                type = NotificationType.INFORMATION;
        }
        final Notification notification = this.createNotification(getTitle(message.getTitle()), message.getMessage(), type);
        final IAzureMessage.Action[] actions = message.getActions();
        if (actions != null) {
            notification.addActions(Arrays.stream(actions).map(a -> toAction(a, message)).collect(Collectors.toList()));
        }
        Notifications.Bus.notify(notification);
        return true;
    }

    private static AnAction toAction(IAzureMessage.Action a, IAzureMessage message) {
        if (a instanceof IntellijActionMessageAction) {
            return ActionManager.getInstance().getAction(((IntellijActionMessageAction) a).getActionId());
        }
        return new AnAction(a.name()) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                a.actionPerformed(message);
            }
        };
    }
}
