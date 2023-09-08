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
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.Operation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.*;

@Slf4j
public class IntellijAzureMessager implements IAzureMessager {
    static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private static final Map<IAzureMessage.Type, NotificationType> types = Map.ofEntries(
        Map.entry(IAzureMessage.Type.INFO, NotificationType.INFORMATION),
        Map.entry(IAzureMessage.Type.SUCCESS, NotificationType.INFORMATION),
        Map.entry(IAzureMessage.Type.WARNING, NotificationType.WARNING),
        Map.entry(IAzureMessage.Type.ERROR, NotificationType.ERROR)
    );

    private Notification createNotification(@Nonnull String title, @Nonnull String content, NotificationType type) {
        return new Notification(NOTIFICATION_GROUP_ID, title, content, type, new NotificationListener.UrlOpeningListener(false) {
            @Override
            @SneakyThrows
            protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                super.hyperlinkActivated(notification, event);
                final URL url = event.getURL();
                if (Objects.nonNull(url)) {
                    final URIBuilder uri = new URIBuilder(url.toString());
                    uri.getQueryParams().stream().filter(p -> "_ijop_".equalsIgnoreCase(p.getName())).map(NameValuePair::getValue)
                        .reduce((first, second) -> second) // find last
                        .ifPresent(op -> {
                            final Map<String, String> properties = new HashMap<>();
                            final String[] parts = op.split("\\.");
                            if (parts.length > 1) {
                                properties.put(SERVICE_NAME, parts[0]);
                                properties.put(OPERATION_NAME, parts[1]);
                                properties.put(OP_NAME, op);
                                properties.put(OP_TYPE, Operation.Type.USER);
                                AzureTelemeter.log(AzureTelemetry.Type.OP_END, properties);
                            }
                        });
                }
            }
        });
    }

    @Override
    public boolean show(IAzureMessage raw) {
        if (raw.getPayload() instanceof Throwable) {
            log.warn("caught an error by messager", ((Throwable) raw.getPayload()));
        }
        switch (raw.getType()) {
            case ALERT, CONFIRM -> {
                final boolean[] result = new boolean[]{true};
                ApplicationManager.getApplication().invokeAndWait(() -> {
                    try {
                        final String title = StringUtils.firstNonBlank(raw.getTitle(), DEFAULT_TITLE);
                        result[0] = MessageDialogBuilder.yesNo(title, raw.getContent()).guessWindowAndAsk();
                    } catch (final Throwable e) {
                        e.printStackTrace();
                    }
                }, ModalityState.any());
                return result[0];
            }
            case DEBUG -> {
                return true;
            }
            default -> {
            }
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
        final Collection<NotificationAction> actions = Arrays.stream(message.getActions())
            .map(a -> ImmutablePair.of(a, a.getView(null)))
            .filter(p -> p.getValue().isVisible())
            .map(p -> new NotificationAction(p.getValue().getLabel()) {
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e, @Nonnull Notification notification) {
                    final Action<?> action = p.getKey();
                    action.handle(null, e);
                    if (action.getId().equals(ResourceCommonActionsContributor.SUPPRESS_ACTION)) {
                        notification.expire();
                    }
                }
            }).collect(Collectors.toList());
        notification.addActions(actions);
        Notifications.Bus.notify(notification, message.getProject());
    }
}
