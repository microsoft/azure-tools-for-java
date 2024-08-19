package com.microsoft.azure.toolkit.intellij.dbtools;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;

public class DatabaseTools {
    public static <T> void openDatabaseTool(Project project, @Nonnull T server, BiConsumer<T, Project> dataSourceManagerDialogOpener) {
        DatabasePlugin.throwTryUltimateIfNotInstalled(server);
        AzureTaskManager.getInstance().runLater(() -> dataSourceManagerDialogOpener.accept(server, project));
    }
}
