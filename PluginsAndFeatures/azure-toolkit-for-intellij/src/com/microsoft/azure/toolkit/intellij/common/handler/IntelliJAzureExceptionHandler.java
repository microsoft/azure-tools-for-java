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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.intellij.common.AzureToolkitErrorDialog;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.handler.AzureExceptionHandler;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationRef;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationUtils;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskContext;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IntelliJAzureExceptionHandler extends AzureExceptionHandler {

    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";

    private static final Map<String, AzureExceptionAction> exceptionActionMap = new HashMap<>();
    public static final String AZURE_TOOLKIT_ERROR = "Azure Toolkit Error";

    public static IntelliJAzureExceptionHandler getInstance() {
        return LazyLoader.INSTANCE;
    }

    public void handleException(Project object, Throwable throwable, boolean isBackGround, @Nullable AzureExceptionHandler.AzureExceptionAction... action) {
        this.onHandleException(object, throwable, isBackGround, action);
    }

    @Override
    protected void onHandleException(final Throwable throwable, final @Nullable AzureExceptionAction[] actions) {
        Boolean backgrounded = AzureTaskContext.current().getBackgrounded();
        if (Objects.isNull(backgrounded)) {
            //TODO: detect task running background or not.
            backgrounded = false;
        }
        onHandleException(throwable, backgrounded, actions);
    }

    @Override
    protected void onHandleException(final Throwable throwable, final boolean isBackGround, final @Nullable AzureExceptionAction[] actions) {
        onHandleException(null, throwable, isBackGround, actions);
    }

    protected void onHandleException(final Project project, final Throwable throwable, final boolean isBackGround,
                                     final @Nullable AzureExceptionAction[] actions) {
        final List<AzureOperationRef> operationRefList = revise(AzureTaskContext.getContextOperations(AzureTaskContext.current()));
        final List<Throwable> azureToolkitExceptions = (List<Throwable>) ExceptionUtils.getThrowableList(throwable).stream()
                                                                                       .filter(object -> object instanceof AzureToolkitRuntimeException
                                                                                           || object instanceof AzureToolkitException)
                                                                                       .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(azureToolkitExceptions) && CollectionUtils.isEmpty(operationRefList)) {
            showException(project, isBackGround, throwable.getMessage(), null, actions, throwable);
        } else {
            // get action from the latest exception
            final AzureExceptionAction[] actionArray = getActions(azureToolkitExceptions, actions);
            final String description = getAzureErrorMessage(operationRefList, azureToolkitExceptions);
            final List<String> operationStack = getAzureOperationStack(operationRefList, azureToolkitExceptions);
            showException(project, isBackGround, description, operationStack, actionArray, throwable);
        }
    }

    private void showException(Project project, boolean isBackGround, String message, List<String> operationStack, AzureExceptionAction[] actions,
                               Throwable throwable) {
        if (isBackGround) {
            showBackgroundException(project, message, operationStack, actions, throwable);
        } else {
            showForegroundException(project, message, operationStack, actions, throwable);
        }
    }

    private void showForegroundException(Project project, String message, List<String> operationStack, AzureExceptionAction[] actions, Throwable throwable) {
        final String details = CollectionUtils.isEmpty(operationStack) ?
                               StringUtils.EMPTY : getErrorDialogDetails(operationStack);
        UIUtil.invokeLaterIfNeeded(() -> {
            final AzureToolkitErrorDialog errorDialog = new AzureToolkitErrorDialog(project, AZURE_TOOLKIT_ERROR, message, details, actions, throwable);
            final Window dialogWindow = errorDialog.getWindow();
            final Component modalityStateComponent = dialogWindow.getParent() == null ? dialogWindow : dialogWindow.getParent();
            ApplicationManager.getApplication().invokeLater(errorDialog::show, ModalityState.stateForComponent(modalityStateComponent));
        });
    }

    private String getErrorDialogDetails(List<String> operationStack) {
        final String template = "<html><style>li {list-style-type:none;}</style><ul>%s</ul></html>";
        return String.format(template, convertOperationToHTML(operationStack));
    }

    private String convertOperationToHTML(List<String> operationStack) {
        if (CollectionUtils.isEmpty(operationStack)) {
            return StringUtils.EMPTY;
        }
        final String operation = StringUtils.capitalize(operationStack.get(0));
        final List<String> leftStack = operationStack.size() > 1 ? operationStack.subList(1, operationStack.size()) : null;
        return leftStack == null ? String.format("<li>- %s</li>", operation) :
               String.format("<li>- %s<ul>%s</ul></li>", operation, convertOperationToHTML(leftStack));
    }

    private void showBackgroundException(Project project, String message, List<String> operations, AzureExceptionAction[] actions, Throwable throwable) {
        final String body = getNotificationBody(message, operations);
        final Notification notification = new Notification(NOTIFICATION_GROUP_ID, AZURE_TOOLKIT_ERROR, body, NotificationType.ERROR);
        for (AzureExceptionAction exceptionAction : actions) {
            notification.addAction(new AnAction(exceptionAction.name()) {
                @Override
                public void actionPerformed(@NotNull final AnActionEvent anActionEvent) {
                    exceptionAction.actionPerformed(throwable);
                }
            });
        }
        Notifications.Bus.notify(notification, project);
    }

    private String getNotificationBody(String message, List<String> operations) {
        if (CollectionUtils.isEmpty(operations)) {
            return String.format("<html>%s</html>", message);
        }
        final String liList = operations.stream()
                                        .map(string -> String.format("<li>%s</li>", StringUtils.capitalize(string)))
                                        .collect(Collectors.joining(System.lineSeparator()));
        return String.format("<html>%s <div><p>Call Stack: </p><ol>%s</ol></div></html>", message, liList);
    }

    private List<String> getAzureOperationStack(List<AzureOperationRef> callStacks, List<Throwable> throwableList) {
        final Stream<String> callStackStream = callStacks.stream().map(operation -> AzureOperationUtils.getOperationTitle(operation));
        final Stream<String> exceptionStream = throwableList.stream().map(throwable -> throwable.getMessage());
        return Stream.concat(callStackStream, exceptionStream).collect(Collectors.toList());
    }

    private String getAzureErrorMessage(List<AzureOperationRef> callStacks, List<Throwable> azureToolkitExceptions) {
        final String action = getActionText(azureToolkitExceptions);
        final String operation = CollectionUtils.isNotEmpty(callStacks) ?
                                 AzureOperationUtils.getOperationTitle(callStacks.get(0)) :
                                 azureToolkitExceptions.get(0).getMessage();
        final String cause = CollectionUtils.isNotEmpty(azureToolkitExceptions) ?
                             azureToolkitExceptions.get(azureToolkitExceptions.size() - 1).getMessage() :
                             AzureOperationUtils.getOperationTitle(callStacks.get(callStacks.size() - 1));
        if (StringUtils.isNotEmpty(action)) {
            return String.format("Failed to %s, please %s", operation, action);
        } else {
            return StringUtils.equals(operation, cause) ?
                   String.format("Failed to %s", operation) : String.format("Failed to %s, as %s failed", operation, cause);
        }
    }

    private String getActionText(final List<Throwable> throwableList) {
        final ListIterator<Throwable> iterator = throwableList.listIterator(throwableList.size());
        while (iterator.hasPrevious()) {
            final Throwable throwable = iterator.previous();
            if (throwable instanceof AzureToolkitException || throwable instanceof AzureToolkitRuntimeException) {
                final String action = throwable instanceof AzureToolkitException ? ((AzureToolkitException) throwable).getAction() :
                                      ((AzureToolkitRuntimeException) throwable).getAction();
                if (StringUtils.isNotEmpty(action)) {
                    return action;
                }
            }
        }
        return StringUtils.EMPTY;
    }

    private AzureExceptionAction[] getActions(final List<Throwable> throwableList, final AzureExceptionAction[] actions) {
        final ListIterator<Throwable> iterator = throwableList.listIterator(throwableList.size());
        String actionId = null;
        while (iterator.hasPrevious()) {
            final Throwable throwable = iterator.previous();
            if (throwable instanceof AzureToolkitException || throwable instanceof AzureToolkitRuntimeException) {
                actionId = throwable instanceof AzureToolkitException ? ((AzureToolkitException) throwable).getActionId() :
                           ((AzureToolkitRuntimeException) throwable).getActionId();
                if (StringUtils.isNotEmpty(actionId)) {
                    break;
                }
            }
        }
        final AzureExceptionAction registerAction = exceptionActionMap.get(actionId);
        return registerAction == null ? actions : ArrayUtils.addAll(actions, registerAction);
    }

    public static List<AzureOperationRef> revise(Deque<? extends AzureOperationRef> operations) {
        final LinkedList<AzureOperationRef> result = new LinkedList<>();
        for (final AzureOperationRef op : operations) {
            result.addFirst(op);
            final AzureOperation annotation = AzureOperationUtils.getAnnotation(op);
            if (annotation.type() == AzureOperation.Type.ACTION) {
                break;
            }
        }
        return result;
    }

    private static final class LazyLoader {
        private static final IntelliJAzureExceptionHandler INSTANCE = new IntelliJAzureExceptionHandler();
    }
}
