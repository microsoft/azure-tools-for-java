package com.microsoft.azure.toolkit.intellij.common.messager;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.management.exception.ManagementException;
import com.google.common.collect.Streams;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.SimpleMessage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationRef;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Setter
@RequiredArgsConstructor
public class IntellijAzureMessage implements IAzureMessage {
    static final Pattern URL_PATTERN = Pattern.compile("(https?|ftp)://(www\\d?|[a-zA-Z0-9]+)?.[a-zA-Z0-9-]+(:|.)([a-zA-Z0-9.]+|(\\d+)?)([/?:].*)?");
    static final String DEFAULT_MESSAGE_TITLE = "Azure";
    @Nullable
    private String title;
    @Nullable
    private Object payload;
    @Nullable
    private Action[] actions;
    @Nullable
    private Boolean backgrounded;
    @Nullable
    @Getter
    private Project project;
    @Nonnull
    @Getter
    private final IAzureMessage original;

    public IntellijAzureMessage(@Nonnull Type type, @Nonnull String message) {
        this(new SimpleMessage(type, message));
    }

    @Nonnull
    public String getMessage() {
        return this.original.getMessage();
    }

    @Nonnull
    @Override
    public Type getType() {
        return this.original.getType();
    }

    @Nonnull
    @Override
    public String getTitle() {
        return StringUtils.firstNonBlank(this.title, this.original.getTitle(), DEFAULT_MESSAGE_TITLE);
    }

    @Nullable
    @Override
    public Object getPayload() {
        return ObjectUtils.firstNonNull(this.payload, this.original.getPayload());
    }

    @Nonnull
    @Override
    public Action[] getActions() {
        return ObjectUtils.firstNonNull(this.actions, this.original.getActions(), new Action[0]);
    }

    public String getContent(boolean includingDetails) {
        if (original.getType() != IAzureMessage.Type.ERROR || !(original.getPayload() instanceof Throwable)) {
            return IntellijAzureMessage.transformURLIntoLinks(original.getMessage());
        }
        final Throwable throwable = (Throwable) original.getPayload();
        final List<IAzureOperation> operations = this.getOperations();
        final String failure = operations.isEmpty() ? "Failed to proceed" : "Failed to " + operations.get(0).getTitle();
        final String cause = Optional.ofNullable(IntellijAzureMessage.getCause(throwable))
                .map(IntellijAzureMessage::transformURLIntoLinks)
                .map(c -> String.format(", because <span style='font-style: italic;'>%s</span>", c))
                .orElse("");
        final String errorAction = Optional.ofNullable(getErrorAction(throwable))
                .map(IntellijAzureMessage::transformURLIntoLinks)
                .map(c -> String.format("<p>%s</p>", c))
                .orElse("");
        if (includingDetails) {
            final String details = this.getDetails(operations);
            final String style = "margin:0;margin-top:2px;padding-left:0;list-style-type:none;";
            final String detailsMsg = StringUtils.isNotBlank(details) ? String.format("<div>Call Stack:</div><ul style='%s'>%s</ul>", style, details) : "";
            return "<html>" + failure + cause + errorAction + detailsMsg + "</html>";
        }
        return "<html>" + failure + cause + errorAction + "</html>";
    }

    public String getDetails() {
        return this.getDetails(this.getOperations());
    }

    private String getDetails(List<? extends IAzureOperation> operations) {
        return operations.size() < 2 ? "" : operations.stream()
                .map(IAzureOperation::getTitle)
                .map(title -> String.format("<li>&#9679; %s</li>", StringUtils.capitalize(title.toString())))
                .collect(Collectors.joining(""));
    }

    @Nonnull
    public List<AnAction> getAnActions() {
        return Arrays.stream(this.getActions()).map(a -> toAction(a, original)).collect(Collectors.toList());
    }

    @Nullable
    public Boolean getBackgrounded() {
        return Optional.ofNullable(this.backgrounded)
                .or(() -> Optional.ofNullable(AzureTaskContext.current().getTask())
                        .map(AzureTask::getBackgrounded)).orElse(null);
    }

    @Nullable
    private static String getCause(@Nonnull Throwable throwable) {
        final Throwable root = getRecognizableCause(throwable);
        if (Objects.isNull(root)) {
            return null;
        }
        String cause = null;
        if (root instanceof ManagementException) {
            cause = ((ManagementException) root).getValue().getMessage();
        } else if (root instanceof HttpResponseException) {
            cause = ((HttpResponseException) root).getResponse().getBodyAsString().block();
        }
        return StringUtils.firstNonBlank(cause, root.getMessage());
    }

    @Nullable
    private static Throwable getRecognizableCause(@Nonnull Throwable throwable) {
        final List<Throwable> throwables = ExceptionUtils.getThrowableList(throwable);
        for (int i = throwables.size() - 1; i >= 0; i--) {
            final Throwable root = throwables.get(i);
            if (root instanceof AzureOperationException) {
                return null;
            }
            final String rootClassName = root.getClass().getName();
            if (rootClassName.startsWith("com.microsoft.azure") || rootClassName.startsWith("com.azure")) {
                return root;
            }
        }
        return null;
    }

    @Nullable
    private static String getErrorAction(@Nonnull Throwable throwable) {
        return ExceptionUtils.getThrowableList(throwable).stream()
                .filter(t -> t instanceof AzureToolkitRuntimeException || t instanceof AzureToolkitException)
                .map(t -> t instanceof AzureToolkitRuntimeException ? ((AzureToolkitRuntimeException) t).getAction() : ((AzureToolkitException) t).getAction())
                .findFirst()
                .orElse(null);
    }

    @Nonnull
    public List<IAzureOperation> getOperations() {
        final List<IAzureOperation> contextOperations = getContextOperations();
        final List<IAzureOperation> exceptionOperations = Optional.ofNullable(this.getPayload())
                .filter(p -> p instanceof Throwable)
                .map(p -> getExceptionOperations((Throwable) p))
                .orElse(new ArrayList<>());
        final LinkedHashMap<String, IAzureOperation> operations = new LinkedHashMap<>();
        Streams.concat(contextOperations.stream(), exceptionOperations.stream())
                .filter(o -> !operations.containsKey(o.getName()))
                .forEachOrdered(o -> operations.put(o.getName(), o));
        return new ArrayList<>(operations.values());
    }

    @Nonnull
    private static List<IAzureOperation> getContextOperations() {
        final LinkedList<IAzureOperation> result = new LinkedList<>();
        IAzureOperation current = AzureTaskContext.current().currentOperation();
        while (Objects.nonNull(current)) {
            if (current instanceof AzureOperationRef) {
                result.addFirst(current);
                final AzureOperation annotation = ((AzureOperationRef) current).getAnnotation(AzureOperation.class);
                if (annotation.type() == AzureOperation.Type.ACTION) {
                    break;
                }
            }
            current = current.getParent();
        }
        return result;
    }

    @Nonnull
    private static List<IAzureOperation> getExceptionOperations(@Nonnull Throwable throwable) {
        return ExceptionUtils.getThrowableList(throwable).stream()
                .filter(object -> object instanceof AzureOperationException)
                .map(o -> ((AzureOperationException) o).getOperation())
                .collect(Collectors.toList());
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

    private static String transformURLIntoLinks(String text) {
        final Matcher m = URL_PATTERN.matcher(text);
        final StringBuilder sb = new StringBuilder();
        while (m.find()) {
            final String found = m.group(0);
            m.appendReplacement(sb, "<a href='" + found + "'>" + found + "</a>");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static IntellijAzureMessage from(IAzureMessage raw) {
        return raw instanceof IntellijAzureMessage ? (IntellijAzureMessage) raw : new IntellijAzureMessage(raw);
    }
}
