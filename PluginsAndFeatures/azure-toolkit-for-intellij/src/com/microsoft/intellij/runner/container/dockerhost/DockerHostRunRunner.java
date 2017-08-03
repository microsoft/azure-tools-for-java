package com.microsoft.intellij.runner.container.dockerhost;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.DefaultProgramRunner;
import org.jetbrains.annotations.NotNull;

public class DockerHostRunRunner extends DefaultProgramRunner {
    private static final String ID = "DockerHostRunRunner";

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof DockerHostRunConfiguration;
    }

    @NotNull
    @Override
    public String getRunnerId() {
        return ID;
    }
}
