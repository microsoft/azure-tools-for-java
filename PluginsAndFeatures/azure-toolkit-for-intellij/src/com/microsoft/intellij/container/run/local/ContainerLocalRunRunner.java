package com.microsoft.intellij.container.run.local;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.microsoft.intellij.container.run.remote.ContainerRemoteRunConfiguration;
import org.jetbrains.annotations.NotNull;

public class ContainerLocalRunRunner extends DefaultProgramRunner{
    private static final String ID = "com.microsoft.intellij.container.run.remote.ContainerLocalRunRunner";

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof ContainerLocalRunConfiguration;
    }

    @NotNull
    @Override
    public String getRunnerId() {
        return ID;
    }
}
