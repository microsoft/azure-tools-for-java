package com.microsoft.azure.toolkit.ide.guideline;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.microsoft.azure.toolkit.ide.guideline.config.ProcessConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class GuidanceStartupListener implements StartupActivity.DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
        final ProcessConfig processConfigFromWorkspace = GuidanceConfigManager.getInstance().getProcessConfigFromWorkspace(project);
        Optional.ofNullable(processConfigFromWorkspace)
                .ifPresent(config -> GuidanceViewManager.getInstance().showGuidance(project, processConfigFromWorkspace));
    }
}
