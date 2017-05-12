package com.microsoft.intellij.util;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.intellij.AzurePlugin;

import java.util.List;
import java.util.Map;

public class FormUtils {

    public static void loadLocationsAndResourceGrps(Project project) {
        Map<SubscriptionDetail, List<Location>> subscription2Location = AzureModel.getInstance().getSubscriptionToLocationMap();
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
