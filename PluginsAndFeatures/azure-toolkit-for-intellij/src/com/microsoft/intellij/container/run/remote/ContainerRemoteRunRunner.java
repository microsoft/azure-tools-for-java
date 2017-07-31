package com.microsoft.intellij.container.run.remote;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.DefaultProgramRunner;

import org.jetbrains.annotations.NotNull;

public class ContainerRemoteRunRunner extends DefaultProgramRunner {
    private static final String ID = "com.microsoft.intellij.container.run.remote.ContainerRemoteRunRunner";

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof ContainerRemoteRunConfiguration;
    }

    @NotNull
    @Override
    public String getRunnerId() {
        return ID;
    }
}
