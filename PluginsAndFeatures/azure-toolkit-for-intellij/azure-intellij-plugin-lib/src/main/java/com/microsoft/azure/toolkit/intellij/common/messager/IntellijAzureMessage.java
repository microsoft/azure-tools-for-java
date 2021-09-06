/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class IntellijAzureMessage extends AzureMessage {
    static final Pattern URL_PATTERN = compile("\\s+https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&//=]*)");
    @Nullable
    @Getter
    @Setter
    private Project project;
    private IAzureMessage original;

    public IntellijAzureMessage(@Nonnull Type type, @Nonnull AzureString message) {
        super(type, message);
    }

    public IntellijAzureMessage(IAzureMessage raw) {
        super(raw.getType(), raw.getMessage());
        this.original = raw;
        if (raw instanceof AzureMessage) {
            this.setValueDecorator(((AzureMessage) raw).getValueDecorator());
        }
        this.setTitle(raw.getTitle());
        this.setPayload(raw.getPayload());
        this.setActions(raw.getActions());
    }

    @Nonnull
    @Override
    public String getContent() {
        return transformURLIntoLinks(super.getContent());
    }

    @Nullable
    @Override
    protected String getCause(@Nonnull Throwable throwable) {
        final String color = Integer.toHexString(JBColor.RED.getRGB()).substring(2);
        return Optional.ofNullable(super.getCause(throwable))
                .map(cause -> String.format("<span style=\"color: #%s;\">%s</span>", color, cause))
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

    @Override
    public String decorateValue(@Nonnull Object p, @Nullable Supplier<String> dft) {
        String result = super.decorateValue(p, null);
        if (Objects.isNull(result)) {
            final String color = Integer.toHexString(JBColor.BLUE.getRGB()).substring(2);
            final String font = "'JetBrains Mono', Consolas, 'Liberation Mono', Menlo, Courier, monospace";
            result = String.format("<span style=\"color: #%s;font-family: %s;\">%s</span>", color, font, p.toString());
        }
        return Objects.isNull(result) && Objects.nonNull(dft) ? dft.get() : result;
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

class DialogMessage extends IntellijAzureMessage {
    DialogMessage(@Nonnull IAzureMessage original) {
        super(original);
    }

    public String getContent() {
        final String content = super.getContent();
        return String.format("<html>%s</html>", content);
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

    public String getContent() {
        if (this.getType() == Type.ERROR) {
            return super.getContent() + this.getDetails();
        }
        return super.getContent();
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

