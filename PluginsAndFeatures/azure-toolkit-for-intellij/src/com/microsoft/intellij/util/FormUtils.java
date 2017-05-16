package com.microsoft.intellij.util;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.intellij.AzurePlugin;

public class FormUtils {

    public static void loadLocationsAndResourceGrps(Project project) {
        ProgressManager.getInstance().run(new Task.Modal(project,"Loading Available Locations...", false) {
            @Override
            public void run(ProgressIndicator indicator) {
                try {
                    AzureModelController.updateSubscriptionMaps(null);
                } catch (Exception ex) {
                    AzurePlugin.log("Error loading locations", ex);
                }
            }
        });
    }
}
