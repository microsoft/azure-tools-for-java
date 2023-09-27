/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.execution.process.ProcessOutputType;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;

public class RunProcessHandlerMessenger extends IntellijAzureMessager {
    private final RunProcessHandler handler;

    public RunProcessHandlerMessenger(RunProcessHandler handler) {
        super();
        this.handler = handler;
        ConsoleViewContentType.registerNewConsoleViewType(ProcessOutputType.SYSTEM, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        ConsoleViewContentType.registerNewConsoleViewType(ProcessOutputType.STDOUT, ConsoleViewContentType.NORMAL_OUTPUT);
        ConsoleViewContentType.registerNewConsoleViewType(ProcessOutputType.STDERR, ConsoleViewContentType.ERROR_OUTPUT);
    }

    @Override
    public boolean show(IAzureMessage msg) {
        final IAzureMessage raw = msg.getRawMessage();
        if (raw.getType() == IAzureMessage.Type.INFO || raw.getType() == IAzureMessage.Type.WARNING) {
            handler.setText(raw.getMessage().toString());
            return true;
        } else if (raw.getType() == IAzureMessage.Type.DEBUG) {
            handler.println(raw.getMessage().toString(), ProcessOutputType.SYSTEM);
            return true;
        } else if (raw.getType() == IAzureMessage.Type.SUCCESS) {
            handler.println(raw.getMessage().toString(), ProcessOutputType.STDOUT);
        } else if (raw.getType() == IAzureMessage.Type.ERROR) {
            handler.println(raw.getContent(), ProcessOutputType.STDERR);
        }
        return super.show(raw);
    }
}