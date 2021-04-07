package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class ConnectorStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        ResourceManager.registerDefinition(ModuleResource.Definition.IJ_MODULE);
    }
}
