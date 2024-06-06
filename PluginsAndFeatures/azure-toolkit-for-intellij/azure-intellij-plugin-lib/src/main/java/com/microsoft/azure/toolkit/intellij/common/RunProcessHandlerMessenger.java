/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.execution.process.ProcessOutputType;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import org.apache.commons.lang3.ArrayUtils;

import static com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage.Type.*;

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
        final IAzureMessage.Type type = raw.getType();
        switch (type) {
            case INFO, WARNING -> handler.setText(raw.getMessage().toString());
            case DEBUG -> handler.println(raw.getMessage().toString(), ProcessOutputType.SYSTEM);
            case SUCCESS -> handler.println(raw.getMessage().toString(), ProcessOutputType.STDOUT);
            case ERROR -> handler.println(raw.getContent(), ProcessOutputType.STDERR);
        }
        if (ArrayUtils.isNotEmpty(raw.getActions()) || type == ERROR || type == WARNING || type == SUCCESS) {
            return AzureMessager.getDefaultMessager().show(msg);
        }
        return true;
    }
}