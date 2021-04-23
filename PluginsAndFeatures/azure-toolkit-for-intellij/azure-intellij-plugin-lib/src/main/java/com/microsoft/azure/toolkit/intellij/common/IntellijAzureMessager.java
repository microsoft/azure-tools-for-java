package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.ui.Messages;
import com.microsoft.azure.toolkit.lib.common.AzureMessager;

import javax.annotation.Nonnull;

public class IntellijAzureMessager extends AzureMessager {
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private static final String DEFAULT_MESSAGE_TITLE = "Azure";

    private IntellijAzureMessager() {
        super();
    }

    private static final class LazyLoader {
        private static final IntellijAzureMessager INSTANCE = new IntellijAzureMessager();
    }

    public static IntellijAzureMessager getInstance() {
        return LazyLoader.INSTANCE;
    }

    private void showNotification(@Nonnull String title, @Nonnull String message, NotificationType type) {
        Notifications.Bus.notify(new Notification(NOTIFICATION_GROUP_ID, title, message, type));
    }

    @Override
    public boolean confirm(@Nonnull String title, @Nonnull String message) {
        return Messages.showYesNoDialog(message, title, null) == Messages.YES;
    }

    @Override
    public void alert(@Nonnull String title, @Nonnull String message) {
    }

    @Override
    public void alert(@Nonnull String message) {

    }

    @Override
    public void success(@Nonnull String title, @Nonnull String message) {
        this.showNotification(title, message, NotificationType.INFORMATION);
    }

    @Override
    public void success(@Nonnull String message) {
        this.showNotification(DEFAULT_MESSAGE_TITLE, message, NotificationType.INFORMATION);
    }

    @Override
    public void info(@Nonnull String title, @Nonnull String message) {
        this.showNotification(title, message, NotificationType.INFORMATION);
    }

    @Override
    public void warning(@Nonnull String title, @Nonnull String message) {
        this.showNotification(title, message, NotificationType.WARNING);
    }

    @Override
    public void error(@Nonnull Throwable throwable) {
        // TODO: migrate exception handler to this method
    }

    @Override
    public void error(@Nonnull String title, @Nonnull Throwable throwable) {
        // TODO: migrate exception handler to this method
    }
}
