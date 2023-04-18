/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.deployimage;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.DefaultProgramRunner;
import org.jetbrains.annotations.NotNull;

public class DeployImageRunRunner extends DefaultProgramRunner {
    private static final String ID = "DEPLOY_IMAGE_TO_CONTAINER_APP_RUNNER";

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof DeployImageRunConfiguration;
    }

    @NotNull
    @Override
    public String getRunnerId() {
        return ID;
    }
}
