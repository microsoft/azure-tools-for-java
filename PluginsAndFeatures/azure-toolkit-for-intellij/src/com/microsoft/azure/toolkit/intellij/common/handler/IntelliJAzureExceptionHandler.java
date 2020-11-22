/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.intellij.common.handler;

import com.intellij.ide.DataManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.Messages;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitOperationException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.handler.AzureExceptionHandler;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationUtils;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.parboiled.common.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IntelliJAzureExceptionHandler extends AzureExceptionHandler {

    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";

    private static final Map<String, AzureExceptionAction> exceptionActionMap = new HashMap<>();

    public static IntelliJAzureExceptionHandler getInstance() {
        return LazyLoader.INSTANCE;
    }

    static {
        exceptionActionMap.put("001", new AzureExceptionAction() {
            @Override
            public String name() {
                return "Sign In";
            }

            @Override
            public void actionPerformed(final Throwable throwable) {
                new AzureSignInAction().actionPerformed(new AnActionEvent(null,
                                                                          DataManager.getInstance().getDataContext(),
                                                                          ActionPlaces.UNKNOWN,
                                                                          new Presentation(),
                                                                          ActionManager.getInstance(),
                                                                          0));
            }
        });
    }

    @Override
    protected void onHandleException(final Throwable throwable, final @Nullable AzureExceptionAction[] azureExceptionActions) {
        onHandleException(throwable, false, azureExceptionActions);
    }

    @Override
    protected void onHandleException(final Throwable throwable, final boolean isBackGround, final @Nullable AzureExceptionAction[] actions) {
        final List throwableList = ExceptionUtils.getThrowableList(throwable);
        final List<Throwable> azureToolkitExceptions = (List) throwableList.stream()
                                                                .filter(object -> object instanceof AzureToolkitRuntimeException
                                                                        || object instanceof AzureToolkitException)
                                                                .map(object -> (Throwable) object)
                                                                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(azureToolkitExceptions)) {
            showException(isBackGround, "Error", throwable.getMessage(), actions, throwable);
        } else {
            // get action from the latest exception
            final String action = getActionText(azureToolkitExceptions.get(azureToolkitExceptions.size()-1));
            final String actionId = getActionId(azureToolkitExceptions.get(azureToolkitExceptions.size()-1));
            final AzureExceptionAction registerAction = exceptionActionMap.get(actionId);
            final AzureExceptionAction[] actionArray = registerAction == null ? actions : ArrayUtils.addAll(actions, registerAction);
            final List<String> descriptionList =
                azureToolkitExceptions.stream()
                                      .map(azureThrowable -> String.format("<li>%s</li>", getErrorDescription(azureThrowable)))
                                      .collect(Collectors.toList());
            descriptionList.remove(0); // Remove first which should not shown in error stack
            final String description = CollectionUtils.isEmpty(descriptionList) ? "" : "<ul>" + String.join("\n",descriptionList) + "</ul>";
            final String message = StringUtils.isEmpty(description) ? throwable.getMessage() :
                                   "<html>" + throwable.getMessage() + "\n" + description + "\n" + action + "</html>";
            showException(isBackGround, throwable.getMessage(), message, actionArray, throwable);
        }
    }

    private void showException(boolean isBackGround, String title, String message, AzureExceptionAction[] actions, Throwable throwable) {
        if (isBackGround) {
            showBackgroundException(title, message, actions, throwable);
        } else {
            showForegroundException(title, message, actions, throwable);
        }
    }

    private void showForegroundException(String title, String message, AzureExceptionAction[] actions, Throwable throwable) {
        final String[] actionTitles = Arrays.stream(actions).map(AzureExceptionAction::name).toArray(String[]::new);
        final String[] dialogActions = ArrayUtils.addAll(new String[]{Messages.OK_BUTTON}, actionTitles);
        ModalityState state = ModalityState.defaultModalityState();
        ApplicationManager.getApplication().invokeLater(() -> {
            int option = Messages.showDialog(message, title, dialogActions, 0, Messages.getErrorIcon());
            if (option > 0) {
                actions[option - 1].actionPerformed(throwable);
            }
        }, state);
    }

    private void showBackgroundException(String title, String message, AzureExceptionAction[] actions, Throwable throwable) {
        final Notification notification = new Notification(NOTIFICATION_GROUP_ID, title, message, NotificationType.ERROR);
        for (AzureExceptionAction exceptionAction : actions) {
            notification.addAction(new AnAction(exceptionAction.name()) {
                @Override
                public void actionPerformed(@NotNull final AnActionEvent anActionEvent) {
                    exceptionAction.actionPerformed(throwable);
                }
            });
        }
        AzureTaskManager.getInstance().runLater(() -> Notifications.Bus.notify(notification));
    }

    private String getErrorDescription(final Throwable throwable) {
        return throwable instanceof AzureToolkitOperationException ?
               AzureOperationUtils.getOperationTitle(((AzureToolkitOperationException) throwable).getOperation()) :
               throwable.getMessage();
    }

    private String getActionText(final Throwable throwable) {
        String actionText = null;
        if (throwable instanceof AzureToolkitException || throwable instanceof AzureToolkitRuntimeException) {
            actionText = throwable instanceof AzureToolkitException ? ((AzureToolkitException) throwable).getAction() :
                         ((AzureToolkitRuntimeException) throwable).getAction();
        }
        return StringUtils.isEmpty(actionText) ? "" : actionText;
    }

    private String getActionId(final Throwable throwable) {
        if (throwable instanceof AzureToolkitException || throwable instanceof AzureToolkitRuntimeException) {
            return throwable instanceof AzureToolkitException ? ((AzureToolkitException) throwable).getActionId() :
                   ((AzureToolkitRuntimeException) throwable).getActionId();
        }
        return null;
    }

    private static final class LazyLoader {
        private static final IntelliJAzureExceptionHandler INSTANCE = new IntelliJAzureExceptionHandler();
    }
}
