/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.SimpleMessage;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.regex.Pattern.compile;

@Setter
public class IntellijAzureMessage extends AzureMessage {
    static final Pattern URL_PATTERN = compile("\\s+https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&//=]*)");
    @Nullable
    @Getter
    private Project project;

    public IntellijAzureMessage(IAzureMessage raw) {
        super(raw);
    }

    public IntellijAzureMessage(@Nonnull Type type, @Nonnull String message) {
        super(new SimpleMessage(type, message));
    }

    @Nonnull
    @Override
    public String getMessage() {
        return transformURLIntoLinks(super.getMessage());
    }

    @Nullable
    @Override
    protected String getCause(@Nonnull Throwable throwable) {
        return Optional.ofNullable(super.getCause(throwable))
                .map(c -> String.format("<span style='font-style: italic;'>%s</span>", c))
                .orElse(null);
    }

    @Nullable
    @Override
    protected String getErrorAction(@Nonnull Throwable throwable) {
        return Optional.ofNullable(super.getErrorAction(throwable))
                .map(a -> String.format("<p>%s</p>", a))
                .orElse(null);
    }

    @Override
    protected String getDetailItem(IAzureOperation o) {
        return String.format("<li>%s</li>", super.getDetailItem(o));
    }

    @Nonnull
    protected List<AnAction> getAnActions() {
        return Arrays.stream(this.getActions()).map(a -> toAction(a, getOriginal())).collect(Collectors.toList());
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

    public static IntellijAzureMessage from(IAzureMessage raw) {
        return raw instanceof IntellijAzureMessage ? (IntellijAzureMessage) raw : new IntellijAzureMessage(raw);
    }
}

class DialogMessage extends IntellijAzureMessage {
    DialogMessage(@Nonnull IAzureMessage original) {
        super(original);
    }

    DialogMessage(@Nonnull String message) {
        this(new SimpleMessage(IAzureMessage.Type.ERROR, message));
    }

    public String getMessage() {
        final String message = super.getMessage();
        return String.format("<html>%s</html>", message);
    }

    @Override
    public String getDetails() {
        final String details = super.getDetails();
        if (StringUtils.isNotBlank(details)) {
            final String style = "margin:0;margin-top:2px;padding-left:1px;list-style-type:none;";
            return String.format("<html><ul style='%s'>%s</ul></html>", style, details);
        }
        return "";
    }
}

class NotificationMessage extends IntellijAzureMessage {
    NotificationMessage(@Nonnull IAzureMessage original) {
        super(original);
    }

    NotificationMessage(@Nonnull String message) {
        this(new SimpleMessage(IAzureMessage.Type.ERROR, message));
    }

    public String getMessage() {
        final String message = super.getMessage();
        final String details = this.getDetails();
        return String.format("<html>%s</html>", message + details);
    }

    @Override
    public String getDetails() {
        final String details = super.getDetails();
        if (StringUtils.isNotBlank(details)) {
            final String style = "margin:0;margin-top:2px;padding-left:0;list-style-type:none;";
            return String.format("<div>Call Stack:</div><ul style='%s'>%s</ul>", style, details);
        }
        return "";
    }
}

