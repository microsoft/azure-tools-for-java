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

package com.microsoft.intellij.runner;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;
import com.microsoft.azuretools.utils.IProgressIndicator;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;

public class RunProcessHandler extends ProcessHandler implements IProgressIndicator {

    private static final String PROCESS_TERMINATED = "The process has been terminated";

    public static final Runnable THROW_TERMINATED_EXCEPTION = () -> {
        throw new RuntimeException(PROCESS_TERMINATED);
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
