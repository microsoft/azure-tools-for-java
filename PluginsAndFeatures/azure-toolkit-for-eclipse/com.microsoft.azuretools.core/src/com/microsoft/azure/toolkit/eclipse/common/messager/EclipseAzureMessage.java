/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common.messager;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureHtmlMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public class EclipseAzureMessage extends AzureHtmlMessage {
    public EclipseAzureMessage(@Nonnull IAzureMessage.Type type, @Nonnull AzureString message) {
        super(type, message);
    }

    public EclipseAzureMessage(IAzureMessage raw) {
        super(raw);
    }

    @Nonnull
    public String getContent() {
        String content = super.getContent();
        if (this.getType() == Type.ERROR) {
            content = super.getContent() + this.getDetails();
        }
        final String font = "font-family:'Segoe UI', Consolas, 'Liberation Mono', Menlo, Courier, monospace;";
        final String style = "font-size:12px;" + font;
        return String.format("<html><head><style>body{%s}</style></head><body>%s</body></html>", style, content);
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