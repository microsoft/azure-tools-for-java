package com.microsoft.azure.toolkit.intellij.connector.mysql;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceManager;
import org.jetbrains.annotations.NotNull;

public class MySQLDatabaseResourceConnectionStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        final String resourceType = MySQLDatabaseResource.TYPE;
        final String consumerType = ModuleResource.TYPE;
        ResourceManager.registerDefinition(MySQLDatabaseResource.Definition.AZURE_MYSQL);
        ConnectionManager.registerDefinition(resourceType, consumerType, MySQLDatabaseResourceConnection.Definition.MODULE_MYSQL);
    }
}
