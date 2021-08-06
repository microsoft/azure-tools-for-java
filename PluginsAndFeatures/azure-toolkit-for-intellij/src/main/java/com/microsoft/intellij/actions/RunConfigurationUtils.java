/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.actions;

import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

public class RunConfigurationUtils {

    public static RunnerAndConfigurationSettings getOrCreateRunConfigurationSettings(Module module, RunManagerEx manager, ConfigurationFactory factory) {
        final Project project = module.getProject();
        RunnerAndConfigurationSettings settings = manager.findConfigurationByName(
                String.format("%s: %s:%s", factory.getName(), project.getName(), module.getName()));
        if (settings == null) {
            settings = manager.createConfiguration(
                    String.format("%s: %s:%s", factory.getName(), project.getName(), module.getName()),
                    factory);
        }
        return settings;
    }
}
