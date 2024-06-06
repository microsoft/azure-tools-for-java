/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.util.registry.Registry;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessagerProvider;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.event.HyperlinkEvent;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

    public IntellijAzureMessager() {
        Scheduler.start();
    }

    private static Notification createNotification(@Nonnull String title, @Nonnull String content, NotificationType type) {
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
            case PROGRESS -> {
                final ProgressManager manager = ProgressManager.getInstance();
                final ProgressIndicator indicator = manager.getProgressIndicator();
                if (Objects.nonNull(indicator)) {
                    indicator.setText2(raw.getContent());
                    return false;
                }
                return true;
            }
            default -> {
            }
        }
        final IntellijAzureMessage message = (IntellijAzureMessage) raw;
        final Project project = message.getProject();
        if (message.getDelay() != null) {
            Mono.delay(message.getDelay()).subscribe(next -> showNotification(message));
        } else if (message.getPriority() > 0) {
            Scheduler.push(message);
        } else {
            showNotification(message);
        }
        return true;
    }

    @Override
    public AzureMessage buildMessage(@Nonnull IAzureMessage.Type type, @Nonnull AzureString content, @Nullable String title, @Nullable Object[] actions, @Nullable Object payload) {
        final AzureMessage message = IAzureMessager.super.buildMessage(type, content, title, actions, payload);
        if (type == IAzureMessage.Type.PROGRESS) {
            return message;
        } else if (type == IAzureMessage.Type.ALERT || type == IAzureMessage.Type.CONFIRM) {
            return new DialogMessage(message);
        }
        return new NotificationMessage(message);
    }

    private static void showNotification(@Nonnull IntellijAzureMessage message) {
        final NotificationType type = types.get(message.getType());
        final String content = message.getContent();
        final Notification notification = createNotification(message.getTitle(), content, type);
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
        if (Objects.nonNull(message.getProject()) && message.getProject().isDisposed()) {
            return;
        }
        Notifications.Bus.notify(notification, message.getProject());
    }

    public static class Provider implements AzureMessagerProvider {
        public IAzureMessager getMessager() {
            return ApplicationManager.getApplication().getService(IAzureMessager.class);
        }
    }

    static class Scheduler {
        private static volatile long lastTime = -1;
        private static final PriorityQueue<IntellijAzureMessage> queue = new PriorityQueue<>(Comparator.comparing(IntellijAzureMessage::getPriority));
        private static final int MINUTE = 60 * 1000;
        private static final int[] INTERVALS = new int[]{5 * 1000, 5 * MINUTE, 30 * MINUTE, 60 * MINUTE};
        private static final AtomicBoolean started = new AtomicBoolean(false);

        static void start() {
            if (started.compareAndSet(false, true)) {
                Mono.delay(Duration.ofSeconds(5))
                    .thenMany(Flux.interval(Duration.ofMinutes(2)).onBackpressureBuffer())
                    .doOnNext(m -> {
                        final IntellijAzureMessage message = queue.peek();
                        if (message != null) {
                            final int delay = lastTime < 0 || Registry.is("ide.debugMode") ? 5 * 1000 : INTERVALS[message.getPriority()]; // show message immediately(5s delayed) if it's the first message
                            if (lastTime + delay < System.currentTimeMillis()) {
                                queue.poll();
                                lastTime = System.currentTimeMillis();
                                showNotification(message);
                            }
                        }
                    }).subscribe();
            }
        }

        static void push(IntellijAzureMessage message) {
            queue.add(message);
        }
    }
}
