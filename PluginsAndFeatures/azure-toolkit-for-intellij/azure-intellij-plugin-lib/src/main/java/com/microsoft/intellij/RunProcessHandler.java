/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azuretools.utils.IProgressIndicator;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;

public class RunProcessHandler extends ProcessHandler implements IProgressIndicator {

    public static final Runnable THROW_TERMINATED_EXCEPTION = () -> {
        throw new RuntimeException(AzureMessageBundle.message("appService.runProcess.hint.processTerminated").toString());
    };
    public static final Runnable DO_NOTHING = () -> {
    };

    private Runnable processTerminatedHandler;

    @Override
    protected void destroyProcessImpl() {
    }

    @Override
    protected void detachProcessImpl() {
        notifyProcessDetached();
    }

    @Override
    public boolean detachIsDefault() {
        return false;
    }

    @Nullable
    @Override
    public OutputStream getProcessInput() {
        return null;
    }

    @Override
    public void notifyProcessTerminated(int exitCode) {
        super.notifyProcessTerminated(exitCode);
    }

    public boolean isProcessRunning() {
        return !this.isProcessTerminating() && !this.isProcessTerminated();
    }

    /**
     *
     * @param message String value.
     * @param type Key value.
     */
    public void print(String message, Key type) {
        if (isProcessRunning()) {
            this.notifyTextAvailable(message, type);
        } else {
            processTerminatedHandler.run();
        }
    }

    /**
     *
     * @param message String value.
     * @param type Key value.
     */
    public void println(String message, Key type) {
        if (isProcessRunning()) {
            this.notifyTextAvailable(message + "\n", type);
        } else {
            processTerminatedHandler.run();
        }
    }

    /**
     * Process handler to show the progress message.
     */
    public RunProcessHandler() {
        processTerminatedHandler = THROW_TERMINATED_EXCEPTION;
    }

    public void addDefaultListener() {
        ProcessListener defaultListener = new ProcessListener() {
            @Override
            public void startNotified(ProcessEvent processEvent) {
            }

            @Override
            public void processTerminated(ProcessEvent processEvent) {
            }

            @Override
            public void processWillTerminate(ProcessEvent processEvent, boolean b) {
                notifyProcessTerminated(0);
            }

            @Override
            public void onTextAvailable(ProcessEvent processEvent, Key key) {
            }
        };
        addProcessListener(defaultListener);
    }

    @Override
    public void setText(String text) {
        println(text, ProcessOutputTypes.STDOUT);
    }

    @Override
    public void setText2(String text2) {
        setText(text2);
    }

    @Override
    public void notifyComplete() {
        notifyProcessTerminated(0);
    }

    @Override
    public void setFraction(double fraction) {}

    @Override
    public boolean isCanceled() {
        return false;
    }

    public void setProcessTerminatedHandler(Runnable runnable) {
        this.processTerminatedHandler = runnable == null ? DO_NOTHING : runnable;
    }
}
