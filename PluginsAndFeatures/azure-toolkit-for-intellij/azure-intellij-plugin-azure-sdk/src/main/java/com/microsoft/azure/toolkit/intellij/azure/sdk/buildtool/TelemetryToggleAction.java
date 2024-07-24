package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.TelemetryClientProvider.TelemetryClientProviderVisitor;

/**
 * This class is responsible for toggling the telemetry service on and off.
 */
public class TelemetryToggleAction extends AnAction implements DumbAware {

    // This is the telemetry service that will be toggled on and off.
    private static TelemetryClientProviderVisitor telemetryService;

    // This method sets the telemetry service
    public static void setTelemetryService(TelemetryClientProviderVisitor service) {
        telemetryService = service;
    }

    /**
     * This method is called when the action is performed.
     * It toggles the telemetry service on and off.
     */
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || telemetryService == null) {
            return;
        }

        // If the telemetry service is running, stop it. Otherwise, start it.
        if (telemetryService.isRunning()) {
            telemetryService.stopTelemetryService();
        } else {
            telemetryService.startTelemetryService();
        }
        // Update the presentation of the action.
        updatePresentation(e.getPresentation());
    }

    /**
     * This method updates the presentation of the action.
     * "Presentation of the action" means the UI representation of the action
     *  It ensures the UI is updated to reflect the current state of the telemetry service.
     * It sets the text of the action to "Toggle Telemetry ON" if the telemetry service is not running.
     * Otherwise, it sets the text of the action to "Toggle Telemetry OFF".
     */
    private void updatePresentation(Presentation presentation) {
        if (telemetryService != null && telemetryService.isRunning()) {
            presentation.setText("Toggle Telemetry OFF");
        } else {
            presentation.setText("Toggle Telemetry ON");
        }
    }

    /**
     * This method updates the action event.
     */
    @Override
    public void update(AnActionEvent e) {
        updatePresentation(e.getPresentation());
    }
}
