/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureHtmlMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public class IntellijAzureMessage extends AzureHtmlMessage {
    @Getter
    private Project project;

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

    protected String getErrorColor() {
        return "#" + Integer.toHexString(JBColor.RED.getRGB()).substring(2);
    }

    protected String getValueColor() {
        // color from compile_dark.svg and compile.svg
        return UIUtil.isUnderDarcula() ? "#499C54" : "#59A869";
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

    public static final int FORBIDDEN = 403;

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

