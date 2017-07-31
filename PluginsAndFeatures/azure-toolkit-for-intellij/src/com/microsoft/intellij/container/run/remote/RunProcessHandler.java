package com.microsoft.intellij.container.run.remote;

import com.intellij.execution.process.ProcessHandler;

import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;

public class RunProcessHandler extends ProcessHandler {
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
}
