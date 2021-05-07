/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.microsoft.azure.toolkit.intellij.common.handler.IntelliJAzureExceptionHandler;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;

public class IntellijAzureMessager implements IAzureMessager {
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private static final String DEFAULT_MESSAGE_TITLE = "Azure";

    private void showNotification(@Nonnull String title, @Nonnull String message, NotificationType type) {
        Notifications.Bus.notify(new Notification(NOTIFICATION_GROUP_ID, title, message, type));
    }

    private String getTitle(String... title) {
        if (ArrayUtils.isEmpty(title)) {
            return DEFAULT_MESSAGE_TITLE;
        }
        return title[0];
    }

    public boolean confirm(@Nonnull String message, String title) {
        return MessageDialogBuilder.yesNo(getTitle(title), message).guessWindowAndAsk();
    }

    public void alert(@Nonnull String message, String title) {
        MessageDialogBuilder.okCancel(getTitle(title), message).guessWindowAndAsk();
    }

    public void success(@Nonnull String message, String title) {
        this.showNotification(getTitle(title), message, NotificationType.INFORMATION);
    }

    public void info(@Nonnull String message, String title) {
        this.showNotification(getTitle(title), message, NotificationType.INFORMATION);
    }

    public void warning(@Nonnull String message, String title) {
        this.showNotification(getTitle(title), message, NotificationType.WARNING);
    }

    public void error(@Nonnull String message, String title) {
        this.showNotification(getTitle(title), message, NotificationType.ERROR);
    }

    public void error(@Nonnull Throwable throwable, String title) {
        IntelliJAzureExceptionHandler.getInstance().handleException(throwable);
    }

    public void error(@Nonnull Throwable throwable, @Nonnull String message, String title) {
        final AzureToolkitRuntimeException wrapped = new AzureToolkitRuntimeException(message, throwable);
        IntelliJAzureExceptionHandler.getInstance().handleException(wrapped);
    }

    public String value(String val) {
        return val;
    }
}
