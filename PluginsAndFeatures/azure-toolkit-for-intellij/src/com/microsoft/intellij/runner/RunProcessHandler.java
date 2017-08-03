/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
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
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;

public class RunProcessHandler extends ProcessHandler {

    private static final String PROCESS_TERMINATED = "The process has been terminated";

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

    public void print(String message, Key type) {
        if (!this.isProcessTerminating() && !this.isProcessTerminated()) {
            this.notifyTextAvailable(message, type);
        } else {
            throw new Error(PROCESS_TERMINATED);
        }
    }

    public void println(String message, Key type) {
        if (!this.isProcessTerminating() && !this.isProcessTerminated()) {
            this.notifyTextAvailable(message + "\n", type);
        } else {
            throw new Error(PROCESS_TERMINATED);
        }
    }

    public RunProcessHandler() {
        this.addProcessListener(new ProcessListener() {
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
        });
    }
}
