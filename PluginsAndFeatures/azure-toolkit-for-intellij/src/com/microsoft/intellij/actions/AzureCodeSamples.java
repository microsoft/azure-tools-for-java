package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import org.jdesktop.swingx.JXHyperlink;

import java.net.URI;

/**
 * Created by vlashch on 6/10/16.
 */
public class AzureCodeSamples extends AzureAnAction {
    @Override
    public void onActionPerformed(AnActionEvent anActionEvent) {
        JXHyperlink portalLing = new JXHyperlink();
        portalLing.setURI(URI.create("https://azure.microsoft.com/en-us/documentation/samples/?platform=java"));
        portalLing.doClick();
    }

    @Override
    protected String getServiceName() {
        return TelemetryConstants.SYSTEM;
    }

    @Override
    protected String getOperationName() {
        return TelemetryConstants.AZURECODE_SAMPLES;
    }
}
