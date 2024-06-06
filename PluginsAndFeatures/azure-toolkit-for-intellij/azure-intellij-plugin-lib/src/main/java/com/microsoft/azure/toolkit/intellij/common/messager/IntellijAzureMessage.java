/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.StartupUiUtil;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureHtmlMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.time.Duration;

public class IntellijAzureMessage extends AzureHtmlMessage {
    public static final int PRIORITY_IMMEDIATE = 0;
    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_MIDDLE = 2;
    public static final int PRIORITY_LOW = 3;

    @Getter
    private Project project;
    @Getter
    private Integer priority = 0;
    @Getter
    private Duration delay = null;

    public IntellijAzureMessage(@Nonnull Type type, @Nonnull AzureString message) {
        super(type, message);
    }

    public IntellijAzureMessage(IAzureMessage raw) {
        super(raw);
    }

    public IntellijAzureMessage setProject(Project project) {
        this.project = project;
        return this;
    }

    public IntellijAzureMessage setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public IntellijAzureMessage delay(Duration delay) {
        this.delay = delay;
        return this;
    }

    protected String getErrorColor() {
        return "#" + Integer.toHexString(JBColor.RED.getRGB()).substring(2);
    }

    protected String getValueColor() {
        // color from compile_dark.svg and compile.svg
        //noinspection UnstableApiUsage
        return StartupUiUtil.INSTANCE.isDarkTheme() ? "#688457" : "#59A869";
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
        final String content = this.getType() == Type.ERROR ? super.getContent() + this.getDetails() : super.getContent();
        if (StringUtils.isBlank(this.getTitle())) {
            return "<b>Azure: </b>" + content;
        }
        return content;
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

